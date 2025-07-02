// AlarmState.java

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Getter;

/*
 * ===============================================
 * ============ ALARM SYSTEM DESIGN ==============
 * ===============================================
 *
 * GOALS:
 * - Evaluate alarms based on metric thresholds or complex expressions.
 * - Trigger actions and notify when thresholds are breached.
 * - Support flexible scheduling and scalable architecture.
 *
 * DESIGN HIGHLIGHTS:
 *
 * 1. Condition System:
 *    - Interface: ConditionNode → evaluates to boolean.
 *    - Supports simple (MetricCondition) and composite logic (LogicalCondition).
 *    - ExpressionCondition handles arithmetic expressions using an expression tree.
 *
 * 2. Expression Evaluation:
 *    - ExpressionTreeNode abstract class represents arithmetic expressions.
 *    - Leaf nodes: ConstantNode, VariableNode
 *    - OperatorNode: supports +, -, *, /
 *    - Enables support for conditions like (CPU * MEM) / (CPU + MEM) < 19
 *
 * 3. Action System:
 *    - Uses Action interface (instead of raw Runnable).
 *    - Actions are defined by type + parameters.
 *    - Factory pattern can deserialize actions from DB (see Alarm.java).
 *
 * 4. Scheduling:
 *    - Strategy pattern: ScheduleStrategy interface with multiple implementations.
 *    - FixedRateSchedule uses ScheduledExecutorService.
 *    - Design supports future extension to CronSchedule, OneTimeSchedule, etc.
 *
 * 5. Persistence:
 *    - StorageService abstracts persistence (InMemoryStorageService for now).
 *    - Alarms are stored/reloaded at startup.
 *    - Actions and expressions are stored as structured data (not executable code).
 *
 * 6. Design Patterns Used:
 *    - Strategy: for scheduling
 *    - Composite: for logical condition trees
 *    - Factory: for reconstructing actions/alarms from persisted form
 *    - Interpreter (mini): for evaluating arithmetic expressions
 *
 * TRADEOFFS:
 * - Cannot persist user-defined Java functions (Runnables or lambdas).
 * - Complex user logic must be expressed declaratively or via script (future).
 * - Expression parser must be tightly controlled for security.
 *
 * EXTENSIONS:
 * - Add DSL or expression parser for end-user config.
 * - Add full persistence to DynamoDB, PostgreSQL, etc.
 * - Add validation layer for alarm definitions.
 */
enum AlarmState {
    OK,
    ALARM,
    INSUFFICIENT_DATA
}

// ConditionNode.java
interface ConditionNode {
    boolean evaluate(Map<String, Double> metrics);
}

// ExpressionTreeNode.java

abstract class ExpressionTreeNode {
    abstract double evaluate(Map<String, Double> metrics);
}

class ConstantNode extends ExpressionTreeNode {
    private final double value;
    public ConstantNode(double value) { this.value = value; }
    public double evaluate(Map<String, Double> metrics) { return value; }
}

class VariableNode extends ExpressionTreeNode {
    private final String name;
    public VariableNode(String name) { this.name = name; }
    public double evaluate(Map<String, Double> metrics) {
        if (!metrics.containsKey(name)) throw new RuntimeException("Unknown variable: " + name);
        return metrics.get(name);
    }
}

class OperatorNode extends ExpressionTreeNode {
    private final char operator;
    private final ExpressionTreeNode left, right;

    public OperatorNode(char operator, ExpressionTreeNode left, ExpressionTreeNode right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    public double evaluate(Map<String, Double> metrics) {
        double l = left.evaluate(metrics);
        double r = right.evaluate(metrics);
        switch (operator) {
            case '+': return l + r;
            case '-': return l - r;
            case '*': return l * r;
            case '/': return l / r;
            default: throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }
}

// ExpressionCondition.java

class ExpressionCondition implements ConditionNode {
    private final ExpressionTreeNode root;
    private final double threshold;
    private final String comparison;

    public ExpressionCondition(ExpressionTreeNode root, double threshold, String comparison) {
        this.root = root;
        this.threshold = threshold;
        this.comparison = comparison;
    }

    @Override
    public boolean evaluate(Map<String, Double> metrics) {
        double result = root.evaluate(metrics);
        switch (comparison) {
            case ">": return result > threshold;
            case "<": return result < threshold;
            case "==": return result == threshold;
            default: throw new IllegalArgumentException("Invalid comparison operator");
        }
    }
}

// LogicalCondition.java

class LogicalCondition implements ConditionNode {
    public enum Type { AND, OR }

    private Type type;
    private List<ConditionNode> children;

    public LogicalCondition(Type type, List<ConditionNode> children) {
        this.type = type;
        this.children = children;
    }

    @Override
    public boolean evaluate(Map<String, Double> metrics) {
        switch (type) {
            case AND:
                return children.stream().allMatch(c -> c.evaluate(metrics));
            case OR:
                return children.stream().anyMatch(c -> c.evaluate(metrics));
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
    }
}

// ScheduleStrategy.java

interface ScheduleStrategy {
    void schedule(Alarm alarm, AlarmManager manager, Map<String, Double> metrics);
}

// FixedRateSchedule.java

class FixedRateSchedule implements ScheduleStrategy {
    private final long intervalSeconds;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    public FixedRateSchedule(long intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    @Override
    public void schedule(Alarm alarm, AlarmManager manager, Map<String, Double> metrics) {
        executor.scheduleAtFixedRate(() -> alarm.evaluate(metrics), 0, intervalSeconds, TimeUnit.SECONDS);
    }
}

// StorageService.java

interface StorageService {
    void save(Alarm alarm);
    List<Alarm> loadAll();
}

// InMemoryStorageService.java
class InMemoryStorageService implements StorageService {
    private final Map<String, Alarm> store = new HashMap<>();

    public void save(Alarm alarm) {
        store.put(alarm.getName(), alarm);
    }

    public List<Alarm> loadAll() {
        return new ArrayList<>(store.values());
    }
}

// Alarm.java

/*
 * ==== ACTION SERIALIZATION STRATEGIES ====
 *
 * 1. Declarative Action Model (Recommended)
 *    - Define an Action interface with types (e.g., terminate_instance).
 *    - Store type + parameters in DB (JSON or structured fields).
 *    - Rehydrate using a factory based on 'type'.
 *    - Example stored form:
 *        { "type": "terminate_instance", "instanceId": "i-123" }
 *
 * 2. Scripting Support (Advanced)
 *    - Store scripts (JavaScript, Groovy) as strings.
 *    - Execute in a sandboxed interpreter.
 *    - Useful for user-defined logic but complex and risky.
 *
 * 3. Plugin-Based (Not recommended for general use)
 *    - Let users upload compiled JARs with custom Action classes.
 *    - Load via custom ClassLoader.
 *    - Difficult to secure and manage.
 *
 * Best Practice: Stick with declarative model for portability, safety, and testability.
 */

class Alarm {
    private String id;
    @Getter
    private String name;
    private ConditionNode condition;
    private String recurrence;
    @Getter
    private AlarmState state;
    private List<Runnable> actions;
    @Getter
    private List<String> notificationChannels;
    private ScheduleStrategy scheduleStrategy;

    public Alarm(String id, String name, ConditionNode condition, String recurrence,
            List<Runnable> actions, List<String> notificationChannels, ScheduleStrategy scheduleStrategy) {
        this.id = id;
        this.name = name;
        this.condition = condition;
        this.recurrence = recurrence;
        this.actions = actions;
        this.notificationChannels = notificationChannels;
        this.scheduleStrategy = scheduleStrategy;
        this.state = AlarmState.INSUFFICIENT_DATA;
    }

    public void evaluate(Map<String, Double> metrics) {
        boolean triggered = condition.evaluate(metrics);
        if (triggered) {
            state = AlarmState.ALARM;
            actions.forEach(Runnable::run);
            Notifier.notify(this);
        } else {
            state = AlarmState.OK;
        }
    }

    public void schedule(AlarmManager manager, Map<String, Double> metrics) {
        if (scheduleStrategy != null) {
            scheduleStrategy.schedule(this, manager, metrics);
        }
    }

}

// Notifier.java
// we can also implement observers for different channels and register them to a alarm
class Notifier {
    public static void notify(Alarm alarm) {
        for (String channel : alarm.getNotificationChannels()) {
            System.out.printf("Notifying via %s: Alarm %s triggered!%n", channel, alarm.getName());
        }
    }
}

// AlarmManager.java

class AlarmManager {
    private Map<String, Alarm> alarms = new HashMap<>();
    private final StorageService storageService;

    public AlarmManager(StorageService storageService) {
        this.storageService = storageService;
        for (Alarm a : storageService.loadAll()) {
            alarms.put(a.getName(), a);
        }
    }

    public void addAlarm(Alarm alarm) {
        alarms.put(alarm.getName(), alarm);
        storageService.save(alarm);
    }

    public void evaluateAll(Map<String, Double> metricValues) {
        for (Alarm alarm : alarms.values()) {
            alarm.evaluate(metricValues);
        }
    }

    public Alarm getAlarm(String name) {
        return alarms.get(name);
    }
}

// Main.java

public class AlertService {
    public static void main(String[] args) {
        ExpressionTreeNode cpu = new VariableNode("CPU");
        ExpressionTreeNode mem = new VariableNode("Memory");
        ExpressionTreeNode numerator = new OperatorNode('*', cpu, mem);
        ExpressionTreeNode denominator = new OperatorNode('+', cpu, mem);
        ExpressionTreeNode rootExpr = new OperatorNode('/', numerator, denominator);

        ConditionNode condition = new ExpressionCondition(rootExpr, 19, "<");

        Runnable terminateInstance = () -> System.out.println("Action: Terminating instance i-123");

        ScheduleStrategy scheduleStrategy = new FixedRateSchedule(10);

        Alarm alarm = new Alarm(
                "1", "Complex Arithmetic Alarm",
                condition,
                "fixed",
                Arrays.asList(terminateInstance),
                Arrays.asList("email", "sms"),
                scheduleStrategy
        );

        InMemoryStorageService storage = new InMemoryStorageService();
        AlarmManager manager = new AlarmManager(storage);
        manager.addAlarm(alarm);

        Map<String, Double> metrics = new HashMap<>();
        metrics.put("CPU", 50.0);
        metrics.put("Memory", 20.0);

        alarm.schedule(manager, metrics);
    }
}
