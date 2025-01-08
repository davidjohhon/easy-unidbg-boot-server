package com.easy.unidbg.components;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class ModuleContainer extends ClassLoader {
    private Map<String, Class<?>> map = new ConcurrentHashMap<>();

    public Class<?> defineClassFromBytes(byte[] classData) {
        return defineClass(null, classData, 0, classData.length);
    }

    public void addModule(String module, Class<?> m) {
        map.put(module, m);
    }

    public Class<?> getModule(String module) {
        return map.get(module);
    }

}
