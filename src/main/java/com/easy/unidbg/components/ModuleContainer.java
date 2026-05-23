package com.easy.unidbg.components;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory registry for dynamically loaded service modules.
 * All mutations on the two internal maps are synchronized to maintain consistency.
 * Internal map references are exposed as unmodifiable for safe iteration.
 */
@Component
public class ModuleContainer {

    private final Map<String, Class<?>> map = new ConcurrentHashMap<>();
    private final Map<String, String> mapKey = new ConcurrentHashMap<>();

    /** Returns an unmodifiable view of the className -> Class mapping. */
    public Map<String, Class<?>> getMap() {
        return Collections.unmodifiableMap(map);
    }

    /** Returns an unmodifiable view of the className -> MD5 mapping. */
    public Map<String, String> getMapKey() {
        return Collections.unmodifiableMap(mapKey);
    }

    public synchronized void addModule(String module, Class<?> m, String key) {
        Objects.requireNonNull(module, "module name must not be null");
        Objects.requireNonNull(m, "class must not be null");
        map.put(module, m);
        mapKey.put(module, key);
    }

    /** Alias for addModule; both insert or update by key. */
    public synchronized void updateModule(String module, Class<?> m, String key) {
        addModule(module, m, key);
    }

    /** Returns the Class object for a module, or null if not loaded. */
    public Class<?> getModule(String module) {
        return map.get(module);
    }

    /** Atomically removes a module and its MD5 from both maps. */
    public synchronized void deleteModule(String module) {
        map.remove(module);
        mapKey.remove(module);
    }
}
