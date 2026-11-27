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
