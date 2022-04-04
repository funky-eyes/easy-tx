package icu.funkye.easy.tx.config;

import java.util.HashMap;
import java.util.Map;

public class ThreadLocalContextCore  implements ContextCore {

    private ThreadLocal<Map<String, String>> threadLocal = ThreadLocal.withInitial(() -> new HashMap<>());

    @Override
    public String put(String key, String value) {
        return threadLocal.get().put(key, value);
    }

    @Override
    public String get(String key) {
        return threadLocal.get().get(key);
    }

    @Override
    public String remove(String key) {
        return threadLocal.get().remove(key);
    }

    @Override
    public Map<String, String> entries() {
        return threadLocal.get();
    }
}
