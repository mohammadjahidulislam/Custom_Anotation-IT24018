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
