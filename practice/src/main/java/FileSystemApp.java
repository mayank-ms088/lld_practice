import java.util.*;
import java.util.concurrent.locks.*;

// ======================= Core Interfaces ============================= //
// Strategy Pattern: Provides a contract for mounting, unmounting, and formatting.
// Useful for supporting different file system implementations (e.g., ext4, FAT, encrypted)
interface FileSystemPlugin {
    void mount();
    void unmount();
    void format();
}

// ======================= Metadata and Permissions ==================== //
class Permissions {
    boolean read, write, execute;

    public Permissions(boolean r, boolean w, boolean x) {
        this.read = r;
        this.write = w;
        this.execute = x;
    }
}

class Timestamps {
    long createdAt, modifiedAt, accessedAt;

    public Timestamps() {
        long now = System.currentTimeMillis();
        createdAt = modifiedAt = accessedAt = now;
    }
}

// ======================= Inode ======================================= //
// Inode is a UNIX-style metadata block. It stores file metadata but NOT the actual file name (except here for simplification).
// Encapsulation principle used to group related metadata and pointers together.
class Inode {
    int inodeId;
    String fileName;
    int fileSize;
    int[] directPointers = new int[12];  // Direct block references (Indexed allocation)
    Integer indirectPointer;
    Integer doubleIndirectPointer;
    Permissions permissions;
    Timestamps timestamps;
    String owner;
    String group;
    boolean isDirectory;

    // If this inode is a directory, it maintains a list of entries (filename -> inodeId)
    List<DirectoryEntry> children = new ArrayList<>();

    public Inode(int id, String fileName, String owner, String group, boolean isDirectory) {
        this.inodeId = id;
        this.fileName = fileName;
        this.owner = owner;
        this.group = group;
        this.isDirectory = isDirectory;
        this.permissions = new Permissions(true, true, false);
        this.timestamps = new Timestamps();
    }
}

// ======================= Directory Entry ============================= //
// Represents a mapping between a file/directory name and its inodeId.
// Forms the basic building block for a hierarchical directory structure.
class DirectoryEntry {
    String name;
    int inodeId;

    public DirectoryEntry(String name, int inodeId) {
        this.name = name;
        this.inodeId = inodeId;
    }
}

// ======================= Journaling for Recovery ===================== //
// Command Pattern: JournalEntry acts like a command that can be replayed for crash recovery.
// Ensures data consistency even if the system crashes mid-operation.
enum OperationType { CREATE, DELETE, WRITE }

class JournalEntry {
    OperationType type;
    int inodeId;
    String oldData, newData;
    long timestamp;

    public JournalEntry(OperationType type, int inodeId, String oldData, String newData) {
        this.type = type;
        this.inodeId = inodeId;
        this.oldData = oldData;
        this.newData = newData;
        this.timestamp = System.currentTimeMillis();
    }
}

class Journal {
    List<JournalEntry> log = new ArrayList<>();

    public void record(JournalEntry entry) {
        log.add(entry);
    }

    public void replay() {
        // Replay journal log in order of timestamps (crash recovery)
        for (JournalEntry entry : log) {
            System.out.println("Replaying: " + entry.type + " on inode " + entry.inodeId);
        }
    }
}

// ======================= In-Memory Structures ======================== //
// Singleton-like design: a single FileSystem instance coordinates all state.
// Allows centralized control over file creation, I/O, and metadata.
class FileSystem {
    Map<Integer, Inode> inodeMap = new HashMap<>();                    // Main inode table
    BitSet freeBlockBitmap = new BitSet(10000);                        // Allocation Strategy: Indexed with free space tracking
    Map<Integer, List<Integer>> fileBlockMap = new HashMap<>();       // Maps file inodeId to list of block indexes
    Map<Integer, StringBuilder> fileDataMap = new HashMap<>();        // Simulated in-memory block data (like disk blocks)
    Map<String, Integer> pathToInode = new HashMap<>();               // Flat path lookup (can be extended to hierarchical)
    Journal journal = new Journal();                                  // Journaling system for crash recovery

    // Concurrency control: Each inode gets its own ReentrantLock
    // ReentrantLock ensures that the same thread can acquire the same lock multiple times without deadlock.
    Lock[] inodeLocks = new ReentrantLock[10000];

    int inodeCounter = 0;        // Generates unique inodeIds
    int rootInodeId;             // Root directory inodeId

    public FileSystem() {
        // Initialize lock array and mark all blocks as free
        for (int i = 0; i < 10000; i++) {
            inodeLocks[i] = new ReentrantLock();
            freeBlockBitmap.set(i); // true = free
        }
        rootInodeId = createDirectory("/");  // Create root directory
    }

    // Creates a directory with the given path
    public synchronized int createDirectory(String path) {
        int inodeId = inodeCounter++;
        Inode dirInode = new Inode(inodeId, path, "root", "root", true);
        inodeMap.put(inodeId, dirInode);
        pathToInode.put(path, inodeId);
        return inodeId;
    }

    // Factory Method Pattern: Encapsulates file creation logic, sets up metadata, allocates block structures
    public synchronized int createFile(String path, String owner, String group) {
        String parentPath = path.substring(0, path.lastIndexOf("/"));
        String fileName = path.substring(path.lastIndexOf("/") + 1);

        int parentInodeId = pathToInode.getOrDefault(parentPath, -1);
        if (parentInodeId == -1 || !inodeMap.get(parentInodeId).isDirectory) {
            throw new IllegalArgumentException("Invalid parent directory: " + parentPath);
        }

        int inodeId = inodeCounter++;
        Inode inode = new Inode(inodeId, fileName, owner, group, false);
        inodeMap.put(inodeId, inode);
        pathToInode.put(path, inodeId);
        fileBlockMap.put(inodeId, new ArrayList<>());
        fileDataMap.put(inodeId, new StringBuilder());

        // Add to parent directory
        inodeMap.get(parentInodeId).children.add(new DirectoryEntry(fileName, inodeId));

        // Log operation in journal (for crash recovery)
        journal.record(new JournalEntry(OperationType.CREATE, inodeId, null, path));
        return inodeId;
    }

    // Writes data to a file. Lock ensures thread safety using ReentrantLock.
    public void writeFile(int inodeId, String data) {
        Lock lock = inodeLocks[inodeId];
        lock.lock();  // Acquire lock (can be re-acquired if method is called recursively)
        try {
            StringBuilder current = fileDataMap.get(inodeId);
            String oldData = current.toString();
            current.append(data);
            journal.record(new JournalEntry(OperationType.WRITE, inodeId, oldData, current.toString()));
        } finally {
            lock.unlock();  // Release lock
        }
    }

    // Reads file content from in-memory data blocks
    public String readFile(int inodeId) {
        return fileDataMap.get(inodeId).toString();
    }

    // Lists all entries under a directory (like `ls`)
    public List<DirectoryEntry> listDirectory(String path) {
        int inodeId = pathToInode.getOrDefault(path, -1);
        if (inodeId == -1 || !inodeMap.get(inodeId).isDirectory) {
            throw new IllegalArgumentException("Invalid directory path: " + path);
        }
        return inodeMap.get(inodeId).children;
    }
}

// ======================= Sample Usage ================================ //
public class FileSystemApp {
    public static void main(String[] args) {
        FileSystem fs = new FileSystem(); // Singleton-like global instance

        // Create file inside root directory
        int fileInode = fs.createFile("/file1.txt", "deepak", "dev");

        // Write content
        fs.writeFile(fileInode, "Hello World\n");
        fs.writeFile(fileInode, "Welcome to the FS\n");

        // Read content
        System.out.println("Reading file: \n" + fs.readFile(fileInode));

        // List files in root
        System.out.println("--- Directory Listing ---");
        for (DirectoryEntry entry : fs.listDirectory("/")) {
            System.out.println(entry.name + " -> inode " + entry.inodeId);
        }

        // Replay journal for crash recovery
        System.out.println("--- Journal Entries ---");
        fs.journal.replay();
    }
}
