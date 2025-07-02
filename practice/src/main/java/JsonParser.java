// ===== JSON PARSER WITH STREAMING AND NON-STREAMING SUPPORT =====

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

// ===== JSON Value Types (Composite Pattern) =====
// Composite Pattern allows uniform treatment of individual (primitive) and composite (array, object) JSON types.
// This is part of the Composite Design Pattern. Each JSON value type implements this interface, allowing uniform treatment of primitives, arrays, and objects.
interface JsonValue {}

class JsonNull implements JsonValue {
    public static final JsonNull INSTANCE = new JsonNull();
    private JsonNull() {}
}

class JsonBoolean implements JsonValue {
    public final boolean value;
    public JsonBoolean(boolean value) { this.value = value; }
}

class JsonNumber implements JsonValue {
    public final double value;
    public JsonNumber(double value) { this.value = value; }
}

class JsonString implements JsonValue {
    public final String value;
    public JsonString(String value) { this.value = value; }
}

class JsonArray implements JsonValue {
    public final List<JsonValue> values = new ArrayList<>();
}

class JsonObject implements JsonValue {
    public final Map<String, JsonValue> values = new HashMap<>();
}

// ===== Tokenizer (Non-Streaming) =====
// Acts as a Lexer; can be extended with Strategy Pattern for pluggable token rules.
class JsonTokenizer {
    private final String input;
    public JsonTokenizer(String input) {
        this.input = input.trim();
    }
    public List<String> tokenize() {
        List<String> tokens = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\s*(true|false|null|\"(\\\\.|[^\"])*\"|[\\[\\]\\{\\}:,]|-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?)\\s*");
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            tokens.add(matcher.group(1));
        }
        return tokens;
    }
}

// ===== Non-Streaming Parser Engine =====
// Template Method Pattern: `parse()` is the template calling various parsing steps.
//  This is an example of the Template Method Pattern — a high-level parse method delegates to step-specific implementations like parseObject, parseArray, etc.
class JsonParserEngine {
    private final List<String> tokens;
    private int index = 0;
    private final int maxDepth;

    public JsonParserEngine(List<String> tokens, int maxDepth) {
        this.tokens = tokens;
        this.maxDepth = maxDepth;
    }

    public JsonValue parse() {
        JsonValue val = parseValue(0);
        if (index < tokens.size()) throw new RuntimeException("Trailing characters found");
        return val;
    }

    private JsonValue parseValue(int depth) {
        if (depth > maxDepth) throw new RuntimeException("Max depth exceeded");
        String token = peek();

        switch (token) {
            case "true": next(); return new JsonBoolean(true);
            case "false": next(); return new JsonBoolean(false);
            case "null": next(); return JsonNull.INSTANCE;
            case "{": return parseObject(depth + 1);
            case "[": return parseArray(depth + 1);
            default:
                if (token.startsWith("\"")) return new JsonString(parseString());
                if (token.matches("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?")) return new JsonNumber(Double.parseDouble(next()));
                throw new RuntimeException("Unexpected token: " + token);
        }
    }

    private JsonObject parseObject(int depth) {
        JsonObject obj = new JsonObject();
        expect("{");
        while (!peek().equals("}")) {
            String key = parseString();
            expect(":");
            obj.values.put(key, parseValue(depth));
            if (peek().equals(",")) next();
            else break;
        }
        expect("}");
        return obj;
    }

    private JsonArray parseArray(int depth) {
        JsonArray arr = new JsonArray();
        expect("[");
        while (!peek().equals("]")) {
            arr.values.add(parseValue(depth));
            if (peek().equals(",")) next();
            else break;
        }
        expect("]");
        return arr;
    }

    private String parseString() {
        return next().replaceAll("^\"|\"$", "").replaceAll("\\\\\"", "\"");
    }

    private String peek() {
        return tokens.get(index);
    }

    private String next() {
        return tokens.get(index++);
    }

    private void expect(String expected) {
        if (!next().equals(expected)) throw new RuntimeException("Expected: " + expected);
    }
}

// ===== Parser Facade (for Non-Streaming) =====
// Facade Pattern: simplifies usage for client by hiding internal complexities.
// Acts as a Facade to hide internal parsing complexity from the user. You can mention this explicitly as an example of the Facade Pattern.
class JsonParser {
    private int maxDepth = 100;
    public void configure(int maxDepth) {
        this.maxDepth = maxDepth;
    }
    public JsonValue parse(String json) {
        JsonTokenizer tokenizer = new JsonTokenizer(json);
        List<String> tokens = tokenizer.tokenize();
        JsonParserEngine engine = new JsonParserEngine(tokens, maxDepth);
        return engine.parse();
    }
}

// ===== Tokenizer (Streamed Token Reader) =====
// Supports input stream processing, enabling memory-efficient JSON parsing.
// Could be extended using Strategy Pattern for varied stream formats.
class JsonStreamTokenizer {
    private final BufferedReader reader;
    private final Pattern pattern = Pattern.compile("\\s*(true|false|null|\"(\\\\.|[^\"])*\"|[\\[\\]\\{\\}:,]|-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?)\\s*");
    private String buffer = "";
    private final Queue<String> tokens = new LinkedList<>();

    public JsonStreamTokenizer(InputStream inputStream) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
    }

    public String nextToken() throws IOException {
        while (tokens.isEmpty()) {
            String line = reader.readLine();
            if (line == null) return null;
            buffer += line;
            Matcher matcher = pattern.matcher(buffer);
            int lastMatchEnd = 0;
            while (matcher.find()) {
                tokens.add(matcher.group(1));
                lastMatchEnd = matcher.end();
            }
            buffer = buffer.substring(lastMatchEnd);
        }
        return tokens.poll();
    }
}

// ===== Stream-Based Parser Engine =====
// Reuses recursive descent idea; adapted for streamed input.
class JsonStreamParser {
    private final JsonStreamTokenizer tokenizer;
    private String currentToken;
    private final int maxDepth;

    public JsonStreamParser(JsonStreamTokenizer tokenizer, int maxDepth) throws IOException {
        this.tokenizer = tokenizer;
        this.maxDepth = maxDepth;
        this.currentToken = tokenizer.nextToken();
    }

    public JsonValue parse() throws IOException {
        return parseValue(0);
    }

    private JsonValue parseValue(int depth) throws IOException {
        if (depth > maxDepth) throw new RuntimeException("Max depth exceeded");

        switch (currentToken) {
            case "true": advance(); return new JsonBoolean(true);
            case "false": advance(); return new JsonBoolean(false);
            case "null": advance(); return JsonNull.INSTANCE;
            case "{": return parseObject(depth + 1);
            case "[": return parseArray(depth + 1);
            default:
                if (currentToken.startsWith("\"")) return new JsonString(parseString());
                if (currentToken.matches("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?")) {
                    double num = Double.parseDouble(currentToken);
                    advance();
                    return new JsonNumber(num);
                }
                throw new RuntimeException("Unexpected token: " + currentToken);
        }
    }

    private JsonObject parseObject(int depth) throws IOException {
        JsonObject obj = new JsonObject();
        expect("{");
        while (!currentToken.equals("}")) {
            String key = parseString();
            expect(":");
            obj.values.put(key, parseValue(depth));
            if (currentToken.equals(",")) advance();
            else break;
        }
        expect("}");
        return obj;
    }

    private JsonArray parseArray(int depth) throws IOException {
        JsonArray arr = new JsonArray();
        expect("[");
        while (!currentToken.equals("]")) {
            arr.values.add(parseValue(depth));
            if (currentToken.equals(",")) advance();
            else break;
        }
        expect("]");
        return arr;
    }

    private String parseString() throws IOException {
        String str = currentToken.replaceAll("^\"|\"$", "").replaceAll("\\\\\"", "\"");
        advance();
        return str;
    }

    private void expect(String expected) throws IOException {
        if (!currentToken.equals(expected)) throw new RuntimeException("Expected: " + expected);
        advance();
    }

    private void advance() throws IOException {
        currentToken = tokenizer.nextToken();
    }
}

// ===== Shared User POJO =====
// Target of Adapter Pattern via reflection in mapper.
class User {
    public String name;
    public double age;
    public boolean active;
}

// ===== Mapper (Reflection + Adapter Pattern) =====
// Adapter Pattern: converts from dynamic `JsonObject` to static typed `User`.
// Reflection is used to dynamically set properties.
class JsonMapper {
    // Updated to support nested object mapping as well.
    public static <T> T mapTo(JsonObject json, Class<T> clazz) {
        try {
            T obj = clazz.getDeclaredConstructor().newInstance();
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                JsonValue value = json.values.get(field.getName());
                if (value instanceof JsonString) field.set(obj, ((JsonString) value).value);
                else if (value instanceof JsonNumber) field.set(obj, ((JsonNumber) value).value);
                else if (value instanceof JsonBoolean) field.set(obj, ((JsonBoolean) value).value);
                else if (value instanceof JsonObject) {
                    // Recursively map nested objects
                    Object nested = mapTo((JsonObject) value, field.getType());
                    field.set(obj, nested);
                } else if (value instanceof JsonNull) field.set(obj, null);
            }
            return obj;
        } catch (Exception e) {
            throw new RuntimeException("Mapping failed: " + e.getMessage());
        }
    }
}

// ===== Demo: Non-Streaming Main =====
class MainNonStreaming {
    public static void main(String[] args) {
        String json = "{ \"name\": \"Deepak\", \"age\": 30, \"active\": true }";
        JsonParser parser = new JsonParser();
        JsonValue result = parser.parse(json);
        if (result instanceof JsonObject) {
            User user = JsonMapper.mapTo((JsonObject) result, User.class);
            System.out.println("[Non-Streaming] Name: " + user.name);
            System.out.println("[Non-Streaming] Age: " + user.age);
            System.out.println("[Non-Streaming] Active: " + user.active);
        } else {
            System.out.println("Invalid JSON root object.");
        }
    }
}

// ===== Demo: Streaming Main =====
class MainStreaming {
    public static void main(String[] args) throws IOException {
        String json = "{ \"name\": \"Deepak\", \"age\": 30, \"active\": true }";
        InputStream stream = new ByteArrayInputStream(json.getBytes());
        JsonStreamTokenizer tokenizer = new JsonStreamTokenizer(stream);
        JsonStreamParser parser = new JsonStreamParser(tokenizer, 100);
        JsonValue result = parser.parse();
        if (result instanceof JsonObject) {
            User user = JsonMapper.mapTo((JsonObject) result, User.class);
            System.out.println("[Streaming] Name: " + user.name);
            System.out.println("[Streaming] Age: " + user.age);
            System.out.println("[Streaming] Active: " + user.active);
        } else {
            System.out.println("Invalid JSON root object.");
        }
    }
}
