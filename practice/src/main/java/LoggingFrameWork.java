// Enhanced high-performance, async logging system with config and per-class logger

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

// ============== LogLevel ==============
enum LogLevel {
    DEBUG, INFO, WARN, ERROR, FATAL;
}

// ============== AppenderType ==============
enum AppenderType {
    CONSOLE, FILE;
}

// ============== LogEvent ==============
// Immutable Object Pattern: Thread-safe log event data passed across producer/consumer threads.
class LogEvent {
    final String timestamp;
    final LogLevel level;
    final String message;
    final String threadName;
    final String loggerName;

    LogEvent(LogLevel level, String message, String loggerName) {
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        this.level = level;
        this.message = message;
        this.threadName = Thread.currentThread().getName();
        this.loggerName = loggerName;
    }
}

// ============== Layout ==============
// Strategy Pattern: Allows different formatting strategies (e.g., PatternLayout).
// Can plug into appenders dynamically.
interface Layout {
    String format(LogEvent event);
}

class PatternLayout implements Layout {
    public String format(LogEvent event) {
        return String.format("[%s] [%s] [%s] [%s] - %s", event.timestamp, event.level, event.threadName, event.loggerName, event.message);
    }
}

// ============== Appenders ==============
// Strategy Pattern: ConsoleAppender and FileAppender implement Appender interface.
// Enables interchangeable output strategies without altering core logic.
interface Appender {
    void append(LogEvent event);
    void close();
}

class ConsoleAppender implements Appender {
    Layout layout = new PatternLayout();
    public void append(LogEvent event) {
        System.out.println(layout.format(event));
    }
    public void close() {}
}

class FileAppender implements Appender {
    Layout layout = new PatternLayout();
    BufferedWriter writer;

    FileAppender(String filePath) throws IOException {
        writer = new BufferedWriter(new FileWriter(filePath, true));
    }

    public synchronized void append(LogEvent event) {
        try {
            writer.write(layout.format(event));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try { writer.close(); } catch (IOException e) { e.printStackTrace(); }
    }
}

// ============== Async Dispatcher ==============
// Producer-Consumer Pattern: Logger threads enqueue events, worker threads process them asynchronously.
class AsyncDispatcher {
    private final BlockingQueue<LogEvent> queue = new LinkedBlockingQueue<>(100000);
    private final List<Appender> appenders = new ArrayList<>();
    private final ExecutorService workers = Executors.newFixedThreadPool(4);
    private volatile boolean running = true;

    AsyncDispatcher(List<Appender> appenderList) {
        appenders.addAll(appenderList);
        for (int i = 0; i < 4; i++) {
            workers.submit(() -> {
                while (running || !queue.isEmpty()) {
                    try {
                        LogEvent event = queue.poll(100, TimeUnit.MILLISECONDS);
                        if (event != null) {
                            for (Appender a : appenders) a.append(event);
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            });
        }
    }

    public void submit(LogEvent event) {
        queue.offer(event);
    }

    public void shutdown() {
        running = false;
        workers.shutdown();
        appenders.forEach(Appender::close);
    }
}

// ============== Logger ==============
// Proxy Pattern (conceptual): Logger filters and delegates messages to the AsyncDispatcher,
// abstracting away the formatting and dispatch details from client code.
class Logger {
    private final String name;
    private final LogLevel level;
    private final AsyncDispatcher dispatcher;

    Logger(String name, LogLevel level, AsyncDispatcher dispatcher) {
        this.name = name;
        this.level = level;
        this.dispatcher = dispatcher;
    }

    private void log(LogLevel lvl, String msg) {
        if (lvl.ordinal() >= level.ordinal()) {
            dispatcher.submit(new LogEvent(lvl, msg, name));
        }
    }

    public void debug(String msg) { log(LogLevel.DEBUG, msg); }
    public void info(String msg)  { log(LogLevel.INFO, msg); }
    public void warn(String msg)  { log(LogLevel.WARN, msg); }
    public void error(String msg) { log(LogLevel.ERROR, msg); }
    public void fatal(String msg) { log(LogLevel.FATAL, msg); }
}

// ============== LoggerConfig ==============
// Configuration Object Pattern: Encapsulates configuration data for loggers and appenders.
// Can be externalized and extended to support JSON/YAML-based configs.
class LoggerConfig {
    public LogLevel level;
    public List<AppenderConfig> appenders;

    static class AppenderConfig {
        public AppenderType type;
        public String filePath; // required if type == FILE
    }

    public static LoggerConfig defaultConfig() {
        LoggerConfig config = new LoggerConfig();
        config.level = LogLevel.DEBUG;
        AppenderConfig console = new AppenderConfig();
        console.type = AppenderType.CONSOLE;
        AppenderConfig file = new AppenderConfig();
        file.type = AppenderType.FILE;
        file.filePath = "app.log";
        config.appenders = List.of(console, file);
        return config;
    }
}

// ============== LoggerFactory ==============
// Factory Pattern: Creates and caches Logger instances per class.
// Ensures consistent configuration and reuse of the AsyncDispatcher.
class LoggerFactory {
    private static final Map<String, Logger> loggerMap = new ConcurrentHashMap<>();
    private static AsyncDispatcher dispatcher;
    private static LoggerConfig config;

    static {
        try {
            config = LoggerConfig.defaultConfig();
            List<Appender> appenderInstances = new ArrayList<>();
            for (LoggerConfig.AppenderConfig ac : config.appenders) {
                if (ac.type == AppenderType.CONSOLE) {
                    appenderInstances.add(new ConsoleAppender());
                } else if (ac.type == AppenderType.FILE && ac.filePath != null) {
                    appenderInstances.add(new FileAppender(ac.filePath));
                }
            }
            dispatcher = new AsyncDispatcher(appenderInstances);
        } catch (IOException e) {
            throw new RuntimeException("Logger init failed", e);
        }
    }

    public static Logger getLogger(Class<?> clazz) {
        return loggerMap.computeIfAbsent(clazz.getName(), name -> new Logger(name, config.level, dispatcher));
    }
}

// ============== Example Usage ==============
public class LoggingFrameWork {
    private static final Logger logger = LoggerFactory.getLogger(LoggingFrameWork.class);

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 1000000; i++) {
            logger.info("Test log message " + i);
        }

        Thread.sleep(2000); // Let async workers finish
        System.exit(0);
    }
}
