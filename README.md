# CampusBoot

A tiny Spring Boot-style framework built with custom annotations and Java Reflection (Java 11+, no external libraries).

| | |
|---|---|
| **Name** | Md Jahidul Islam |
| **ID** | IT24018 |

## About

CampusBoot shows that `@Component`, `@Value`, `@GetMapping` and friends are not magic, only annotations plus reflection. The project contains:

- **Annotations:** `@Component`, `@Value`, `@Range`, `@Route`
- **Container:** a mini IoC container with value injection
- **Validator:** checks `@Range` fields
- **Router:** maps a URL path to a method using `@Route`

## How to Run

Put all `.java` files in one folder (default package), then:

```bash
javac *.java
java Main
```

Or open the project in IntelliJ IDEA and run `Main.main()`.

## Output

Text version:

```
[Container] Registered: gradeController (GradeController)
[Container] Skipped: ReportPrinter (no @Component)
[Container] Injected: app.name -> GradeController.appName
[Container] Injected: app.passMark -> GradeController.passMark
--- Validation ---
VIOLATION: score - Score must be between 0 and 100 (actual: 120)
OK
VIOLATION: semester - Value out of range (actual: 15)
--- Routing ---
[Router] /hello -> Welcome to CampusBoot
[Router] /status -> Pass mark is 40
[Router] /unknown -> 404 Not Found: /unknown
```

## Source Code

### Part A: Annotations
#### Component.java
```java
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {
    String value() default "";
}
```

#### Value.java
```java
import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Value {
    String value();
}
```

#### Range.java
```java
import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Range {
    int min();
    int max();
    String message() default "Value out of range";
}
```

#### Route.java
```java
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Route {
    String path();
}
```

### Part B: Container
#### Container.java
```java
import java.lang.reflect.Field;
import java.util.*;

public class Container {
    private final Map<String, Object> beans = new LinkedHashMap<>();

    public void scan(Class<?>... classes) {
        for (Class<?> c : classes) {
            Component comp = c.getAnnotation(Component.class);
            if (comp == null) {
                System.out.println("[Container] Skipped: " + c.getSimpleName() + " (no @Component)");
                continue;
            }
            try {
                String name = comp.value();
                if (name.isEmpty()) {
                    String s = c.getSimpleName();
                    name = Character.toLowerCase(s.charAt(0)) + s.substring(1);
                }
                Object instance = c.getDeclaredConstructor().newInstance();
                beans.put(name, instance);
                System.out.println("[Container] Registered: " + name + " (" + c.getSimpleName() + ")");
            } catch (Exception e) {
                throw new RuntimeException("Cannot create bean for " + c.getName(), e);
            }
        }
    }

    public void injectValues(Map<String, String> config) {
        for (Object bean : beans.values()) {
            for (Field f : bean.getClass().getDeclaredFields()) {
                Value v = f.getAnnotation(Value.class);
                if (v == null) continue;
                String raw = config.get(v.value());
                if (raw == null) continue;
                try {
                    f.setAccessible(true);
                    if (f.getType() == int.class || f.getType() == Integer.class) {
                        f.set(bean, Integer.parseInt(raw.trim()));
                    } else {
                        f.set(bean, raw);
                    }
                    System.out.println("[Container] Injected: " + v.value() + " -> "
                            + bean.getClass().getSimpleName() + "." + f.getName());
                } catch (Exception e) {
                    throw new RuntimeException("Cannot inject " + f.getName(), e);
                }
            }
        }
    }

    public Object getBean(String name) {
        return beans.get(name);
    }

    Collection<Object> getAllBeans() {
        return beans.values();
    }
}
```

### Part C: Validator
#### Validator.java
```java
import java.lang.reflect.Field;
import java.util.*;

public class Validator {
    public static List<String> validate(Object obj) {
        List<String> errors = new ArrayList<>();
        for (Field f : obj.getClass().getDeclaredFields()) {
            Range r = f.getAnnotation(Range.class);
            if (r == null || f.getType() != int.class) continue;
            try {
                f.setAccessible(true);
                int value = f.getInt(obj);
                if (value < r.min() || value > r.max()) {
                    errors.add(f.getName() + " - " + r.message() + " (actual: " + value + ")");
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return errors;
    }
}
```

### Part D: Router
#### Router.java
```java
import java.lang.reflect.Method;

public class Router {
    public static String dispatch(Container c, String path) {
        for (Object bean : c.getAllBeans()) {
            for (Method m : bean.getClass().getDeclaredMethods()) {
                Route r = m.getAnnotation(Route.class);
                if (r != null && r.path().equals(path)) {
                    try {
                        m.setAccessible(true);
                        return (String) m.invoke(bean);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return "404 Not Found: " + path;
    }
}
```

## Starter Code
### GradeController.java
```java
@Component("gradeController")
public class GradeController {
    @Value("app.name")     private String appName;
    @Value("app.passMark") private int passMark;

    @Route("/hello")
    public String hello() { return "Welcome to " + appName; }

    @Route("/status")
    public String status() { return "Pass mark is " + passMark; }
}
```

### ReportPrinter.java
```java
public class ReportPrinter { }
```

### Student.java
```java
public class Student {
    @Range(min = 0, max = 100, message = "Score must be between 0 and 100")
    private int score;

    @Range(min = 1, max = 12)
    private int semester;

    public Student(int score, int semester) { this.score = score; this.semester = semester; }
}
```

### Main.java
```java
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Map<String,String> config = new HashMap<>();
        config.put("app.name", "CampusBoot");
        config.put("app.passMark", "40");

        Container container = new Container();
        container.scan(GradeController.class, ReportPrinter.class);
        container.injectValues(config);

        System.out.println("--- Validation ---");
        for (Student s : List.of(new Student(120, 5), new Student(85, 5), new Student(70, 15))) {
            List<String> errors = Validator.validate(s);
            if (errors.isEmpty()) System.out.println("OK");
            else errors.forEach(e -> System.out.println("VIOLATION: " + e));
        }

        System.out.println("--- Routing ---");
        for (String p : new String[]{"/hello", "/status", "/unknown"})
            System.out.println("[Router] " + p + " -> " + Router.dispatch(container, p));
    }
}
```

## Part E: Reflection Questions
**1. What would happen if @Retention(RetentionPolicy.RUNTIME) were forgotten?**

The default retention is CLASS, which means the annotation is stored in the .class file but is NOT available to the JVM at runtime. Therefore getAnnotation() would return null for every element. The container would skip every class, no values would be injected, the validator would find no @Range fields and the router would always return 404. The framework would silently do nothing.

**2. Mapping to Spring Boot, and what real Spring does that we do not.**

@Component -> @Component (or @Service/@Controller); @Value -> @Value; @Range -> Bean Validation annotations such as @Min/@Max (or @Range from Hibernate Validator); @Route -> @GetMapping / @RequestMapping. Real Spring also does dependency injection of beans into other beans (@Autowired, constructor injection), plus component scanning of whole packages, singleton/prototype scopes, and AOP proxies. Our mini-framework only creates objects and injects simple values.

**3. Why is setAccessible(true) both powerful and risky?**

It lets reflection read and write private fields and call private methods, which is what makes injection into private fields possible without setters. The risk is that it breaks encapsulation: code can change internal state and bypass validation or invariants, which makes bugs and security problems harder to detect. It can also fail or be blocked by the module system (Java 9+) or a SecurityManager.
