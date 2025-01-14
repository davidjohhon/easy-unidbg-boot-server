package com.easy.unidbg.components;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
@Getter
public class ModuleContainer {
    private Map<String, Class<?>> map = new ConcurrentHashMap<>();
    private Map<String, String> mapKey = new ConcurrentHashMap<>();

    public void addModule(String module, Class<?> m, String key) {
        map.put(module, m);
        mapKey.put(module, key);
    }

    public void updateModule(String module, Class<?> m, String key) {
        map.put(module, m);
        mapKey.put(module, key);
    }

    public Class<?> getModule(String module) {
        return map.get(module);
    }

    public void deleteModule(String module) {
        map.remove(module);
        mapKey.remove(module);
    }
}
