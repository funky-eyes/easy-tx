package icu.funkye.easy.tx.config;

import java.util.Map;

public interface ContextCore {

    /**
     * Put string.
     *
     * @param key   the key
     * @param value the value
     * @return the string
     */
    String put(String key, String value);

    /**
     * Get string.
     *
     * @param key the key
     * @return the string
     */
    String get(String key);

    /**
     * Remove string.
     *
     * @param key the key
     * @return the string
     */
    String remove(String key);

    /**
     * entries
     *
     * @return
     */
    Map<String, String> entries();
}
