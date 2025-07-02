// ============================
// EMAIL SYSTEM - LOW LEVEL DESIGN
// ============================
// Design Patterns Used:
// 1. Singleton Pattern -> For EmailService to ensure only one instance serves the whole application.
// 2. Repository Pattern -> For abstracting DB operations (EmailRepository, UserRepository).
// 3. MVC Architecture -> Controller (EmailController), Service (EmailService), Model (User, Email, etc).
// 4. Basic Authentication Pattern -> Added for user login using email and password.
// 5. Decorator Pattern -> SecuredEmailController wraps EmailController for authentication checks.
// This setup allows for testability, modularity, and clear separation of concerns.
// ============================
package mail;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// ========== MODEL LAYER ==========
class User {
    private final String id;
    private final String email;
    private String password;
    private List<Email> inbox = new ArrayList<>();
    private List<Email> sent = new ArrayList<>();
    private List<Email> drafts = new ArrayList<>();

    public User(String id, String email) {
        this.id = id;
        this.email = email;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public List<Email> getInbox() { return inbox; }
    public List<Email> getSent() { return sent; }
    public List<Email> getDrafts() { return drafts; }
}

// ========== EMAIL MODEL ==========
class Email {
    // Builder Pattern applied via inner static Builder class
    private static int counter = 0;
    private final String id;
    private final User sender;
    private final List<User> receivers;
    private final String subject;
    private final String body;
    private final LocalDateTime timestamp;
    private boolean isRead = false;
    private boolean isDeleted = false;
    private boolean isArchived = false;

    public Email(User sender, List<User> receivers, String subject, String body) {
        this.id = "email-" + (++counter);
        this.sender = sender;
        this.receivers = receivers;
        this.subject = subject;
        this.body = body;
        this.timestamp = LocalDateTime.now();
    }

    // Builder inner class for flexible construction
    public static class Builder {
        private User sender;
        private List<User> receivers = new ArrayList<>();
        private String subject = "";
        private String body = "";

        public Builder from(User sender) {
            this.sender = sender;
            return this;
        }

        public Builder to(List<User> receivers) {
            this.receivers = receivers;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Email build() {
            return new Email(sender, receivers, subject, body);
        }
    }

    public String getId() { return id; }
    public User getSender() { return sender; }
    public List<User> getReceivers() { return receivers; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public void setArchived(boolean archived) { isArchived = archived; }
}

// ========== ATTACHMENT & LABEL (Optional Extension Placeholders) ==========
class Attachment {
    // Placeholder for future implementation
}

class Label {
    // Placeholder for future implementation
}

// ========== REPOSITORIES ==========
interface UserRepository {
    User findById(String id);
    User findByEmail(String email);
    void save(User user);
}

interface EmailRepository {
    Email findById(String id);
    List<Email> findInbox(String userId);
    void save(Email email);
}

// ========== AUTHENTICATION SERVICE ==========
class AuthService {
    private static AuthService instance;
    private final UserRepository userRepo;
    private final Set<String> loggedInUsers = new HashSet<>();

    private AuthService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public static AuthService getInstance(UserRepository userRepo) {
        if (instance == null) {
            instance = new AuthService(userRepo);
        }
        return instance;
    }

    public boolean register(String id, String email, String password) {
        if (userRepo.findByEmail(email) != null) return false;
        User user = new User(id, email);
        user.setPassword(password);
        userRepo.save(user);
        return true;
    }

    public User login(String email, String password) {
        User user = userRepo.findByEmail(email);
        if (user != null && password.equals(user.getPassword())) {
            loggedInUsers.add(user.getId());
            return user;
        }
        return null;
    }

    public boolean isAuthenticated(String userId) {
        return loggedInUsers.contains(userId);
    }
}

// ========== DECORATOR: SECURED EMAIL CONTROLLER ==========
class SecuredEmailController {
    private final EmailController controller;
    private final AuthService authService;

    public SecuredEmailController(EmailController controller, AuthService authService) {
        this.controller = controller;
        this.authService = authService;
    }

    public void send(String senderId, List<String> receiverIds, String subject, String body) {
        if (authService.isAuthenticated(senderId)) {
            controller.send(senderId, receiverIds, subject, body);
        } else {
            System.out.println("Access denied: User not authenticated");
        }
    }

    public List<Email> inbox(String userId) {
        if (authService.isAuthenticated(userId)) {
            return controller.inbox(userId);
        }
        System.out.println("Access denied: User not authenticated");
        return Collections.emptyList();
    }

    public void markAsRead(String userId, String emailId) {
        if (authService.isAuthenticated(userId)) {
            controller.markAsRead(userId, emailId);
        } else {
            System.out.println("Access denied: User not authenticated");
        }
    }

    public void archive(String userId, String emailId) {
        if (authService.isAuthenticated(userId)) {
            controller.archive(userId, emailId);
        } else {
            System.out.println("Access denied: User not authenticated");
        }
    }
}

// ========== DRIVER / ENTRY POINT ==========
public class MailService {
    public static void main(String[] args) {
        EmailRepository emailRepo = new InMemoryEmailRepository();
        UserRepository userRepo = new InMemoryUserRepository();

        EmailService service = EmailService.getInstance(emailRepo, userRepo);
        EmailController controller = new EmailController(service);
        AuthService authService = AuthService.getInstance(userRepo);
        SecuredEmailController securedController = new SecuredEmailController(controller, authService);

        // Register and authenticate users
        authService.register("1", "alice@example.com", "pass123");
        authService.register("2", "bob@example.com", "bobpass");

        User alice = authService.login("alice@example.com", "pass123");
        User bob = authService.login("bob@example.com", "bobpass");

        if (alice != null && bob != null) {
            securedController.send(alice.getId(), List.of(bob.getId()), "Hello", "Hi Bob, how are you?");

            System.out.println("Bob's Inbox:");
            for (Email email : securedController.inbox(bob.getId())) {
                System.out.println("From: " + email.getSender().getEmail() + ", Subject: " + email.getSubject());
            }
        } else {
            System.out.println("Authentication failed");
        }
    }
}

// ========== EMAIL SERVICE ==========
class EmailService {
    private static EmailService instance;
    private final EmailRepository emailRepo;
    private final UserRepository userRepo;

    private EmailService(EmailRepository emailRepo, UserRepository userRepo) {
        this.emailRepo = emailRepo;
        this.userRepo = userRepo;
    }

    public static EmailService getInstance(EmailRepository emailRepo, UserRepository userRepo) {
        if (instance == null) {
            instance = new EmailService(emailRepo, userRepo);
        }
        return instance;
    }

    public void composeAndSend(String senderId, List<String> receiverIds, String subject, String body) {
        User sender = userRepo.findById(senderId);
        List<User> receivers = receiverIds.stream().map(userRepo::findById).collect(Collectors.toList());
        Email email = new Email.Builder()
                .from(sender)
                .to(receivers)
                .subject(subject)
                .body(body)
                .build();
        emailRepo.save(email);
    }

    public List<Email> getInbox(String userId) {
        return emailRepo.findInbox(userId);
    }

    public void markRead(String userId, String emailId) {
        Email email = emailRepo.findById(emailId);
        email.setRead(true);
        emailRepo.save(email);
    }

    public void archive(String userId, String emailId) {
        Email email = emailRepo.findById(emailId);
        email.setArchived(true);
        emailRepo.save(email);
    }
}

// ========== EMAIL CONTROLLER ==========
class EmailController {
    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    public void send(String senderId, List<String> receiverIds, String subject, String body) {
        emailService.composeAndSend(senderId, receiverIds, subject, body);
    }

    public List<Email> inbox(String userId) {
        return emailService.getInbox(userId);
    }

    public void markAsRead(String userId, String emailId) {
        emailService.markRead(userId, emailId);
    }

    public void archive(String userId, String emailId) {
        emailService.archive(userId, emailId);
    }
}

// ========== IN-MEMORY IMPLEMENTATIONS ==========
class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> users = new HashMap<>();

    public User findById(String id) {
        return users.get(id);
    }

    public User findByEmail(String email) {
        return users.values().stream()
                .filter(u -> u.getEmail().equals(email))
                .findFirst()
                .orElse(null);
    }

    public void save(User user) {
        users.put(user.getId(), user);
    }
}

class InMemoryEmailRepository implements EmailRepository {
    private final Map<String, Email> emailMap = new HashMap<>();
    private final Map<String, List<Email>> inboxMap = new HashMap<>();

    public Email findById(String id) {
        return emailMap.get(id);
    }

    public List<Email> findInbox(String userId) {
        return inboxMap.getOrDefault(userId, new ArrayList<>());
    }

    public void save(Email email) {
        emailMap.put(email.getId(), email);
        for (User receiver : email.getReceivers()) {
            inboxMap.computeIfAbsent(receiver.getId(), k -> new ArrayList<>()).add(email);
        }
    }
}
