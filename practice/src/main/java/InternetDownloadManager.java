/**
 * FULL Q&A CHAT LOG
 *
 * ❓ What is `file.write(buffer, 0, bytesRead);` doing?
 * ✅ It writes the first `bytesRead` bytes from the `buffer` to the file at the current position.
 * - `buffer`: byte array read from HTTP input stream.
 * - `0`: start writing at index 0 of buffer.
 * - `bytesRead`: number of valid bytes read from the stream.
 *
 * ❓ What is the REST API call used here?
 * ✅ It's an HTTP `GET` request with the `Range` header:
 *   Range: bytes=start-end
 * This instructs the server to return only a segment of the file.
 *
 * ❓ What is the Content-Type here?
 * ✅ It’s in the HTTP response, not the request.
 * You can get it with:
 *   connection.getContentType();
 * The server might return types like `application/zip` or `application/octet-stream`.
 *
 * ❓ Explain `SegmentDownloader` code completely.
 * ✅ It handles downloading a byte range using an HTTP Range request, seeks to the correct offset in a file, writes the data, retries up to 3 times, and uses CountDownLatch for coordination.
 *
 * ❓ What is the request method and headers used here?
 * ✅ The method is `GET` (default). The only custom header set is:
 *   Range: bytes=start-end
 * This is used for segmented downloading.
 *
 * ❓ What is the Content-Type here (again)?
 * ✅ The server returns the Content-Type in the HTTP response. Examples:
 * - application/zip
 * - application/octet-stream
 * - image/jpeg
 * You can access it with `connection.getContentType()`.
 */

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

/**
 * Design Patterns Used:
 *
 * 1. Singleton (Not implemented explicitly here, but DownloadManager could be made a Singleton to ensure only one manager exists)
 * 2. Strategy (RetryHandler can be extracted to apply different retry policies)
 * 3. Observer (ProgressTracker could notify listeners; not yet implemented but implied for progress updates)
 * 4. Thread Pool (via ExecutorService): Manages worker threads efficiently
 * 5. Template Method (SegmentDownloader follows a fixed skeleton with pluggable retry and writing strategies)
 *
 * Reasoning:
 * - These patterns enable clean separation of concerns, concurrency management, extensibility (e.g., different retry strategies), and potential event-driven updates (progress).
 */

public class InternetDownloadManager {
    public static void main(String[] args) {
        DownloadManager manager = new DownloadManager(3);
        manager.addDownload("https://example.com/file.zip", "file.zip", 4);
    }
}

class DownloadManager {
    private final int maxConcurrentDownloads;
    private final ExecutorService executor;
    private final Queue<DownloadTask> queue = new LinkedList<>();
    private final Set<DownloadTask> activeTasks = new HashSet<>();

    public DownloadManager(int maxConcurrentDownloads) {
        this.maxConcurrentDownloads = maxConcurrentDownloads;
        this.executor = Executors.newFixedThreadPool(maxConcurrentDownloads);
    }

    public void addDownload(String url, String filePath, int numSegments) {
        DownloadTask task = new DownloadTask(url, filePath, numSegments, this);
        queue.offer(task);
        processQueue();
    }

    private synchronized void processQueue() {
        while (activeTasks.size() < maxConcurrentDownloads && !queue.isEmpty()) {
            DownloadTask task = queue.poll();
            activeTasks.add(task);
            executor.execute(() -> {
                task.start();
                synchronized (DownloadManager.this) {
                    activeTasks.remove(task);
                    processQueue();
                }
            });
        }
    }
}

class DownloadTask {
    private final String url;
    private final String filePath;
    private final int numSegments;
    private final DownloadManager manager;
    private final List<SegmentDownloader> segmentDownloaders = new ArrayList<>();

    public DownloadTask(String url, String filePath, int numSegments, DownloadManager manager) {
        this.url = url;
        this.filePath = filePath;
        this.numSegments = numSegments;
        this.manager = manager;
    }

    public void start() {
        try {
            long fileSize = getFileSize(url);
            RandomAccessFile file = new RandomAccessFile(filePath, "rw");
            file.setLength(fileSize);
            long segmentSize = fileSize / numSegments;

            ExecutorService segmentExecutor = Executors.newFixedThreadPool(numSegments);
            CountDownLatch latch = new CountDownLatch(numSegments);

            for (int i = 0; i < numSegments; i++) {
                long start = i * segmentSize;
                long end = (i == numSegments - 1) ? fileSize - 1 : (start + segmentSize - 1);
                SegmentDownloader downloader = new SegmentDownloader(url, file, start, end, latch);
                segmentDownloaders.add(downloader);
                segmentExecutor.execute(downloader);
            }

            latch.await();
            segmentExecutor.shutdown();
            System.out.println("Download complete: " + filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long getFileSize(String fileURL) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(fileURL).openConnection();
        connection.setRequestMethod("HEAD");
        connection.getInputStream();
        return connection.getContentLengthLong();
    }
}

class SegmentDownloader implements Runnable {
    private final String url;
    private final RandomAccessFile file;
    private final long start;
    private final long end;
    private final CountDownLatch latch;
    private static final int MAX_RETRIES = 3;

    public SegmentDownloader(String url, RandomAccessFile file, long start, long end, CountDownLatch latch) {
        this.url = url;
        this.file = file;
        this.start = start;
        this.end = end;
        this.latch = latch;
    }

    @Override
    public void run() {
        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestProperty("Range", "bytes=" + start + "-" + end);
                connection.connect();
                try (InputStream in = connection.getInputStream()) {
                    file.seek(start);
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        file.write(buffer, 0, bytesRead);
                    }
                }
                break; // success
            } catch (IOException e) {
                retries++;
                if (retries == MAX_RETRIES) {
                    System.err.println("Failed to download segment: " + start + "-" + end);
                }
            } finally {
                latch.countDown();
            }
        }
    }
}
