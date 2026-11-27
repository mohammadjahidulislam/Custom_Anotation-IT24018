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
