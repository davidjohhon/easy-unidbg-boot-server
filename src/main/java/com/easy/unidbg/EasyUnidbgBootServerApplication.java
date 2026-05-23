package com.easy.unidbg;

import com.easy.unidbg.components.ModuleContainer;
import com.easy.unidbg.entity.ModuleEntity;
import com.easy.unidbg.repository.ModuleEntityRepository;
import com.easy.unidbg.service.AdminService;
import com.easy.unidbg.utils.Md5Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main Spring Boot application entry point.
 * 
 * On startup:
 * 1. Loads all ONLINE modules from the database into the container.
 * 2. Processes --F=<path> arguments to statically load .class files.
 * 3. If --U=<directory> is provided, starts a hot-reload scheduler
 *    that polls the directory every 5 seconds for changes.
 */
@SpringBootApplication
@EnableScheduling
@Slf4j
public class EasyUnidbgBootServerApplication {

    @Autowired
    private ModuleContainer moduleContainer;

    @Autowired
    private AdminService adminService;

    @Autowired
    private ModuleEntityRepository moduleEntityRepository;

    private ScheduledExecutorService hotReloadScheduler;

    public static void main(String[] args) {
        SpringApplication.run(EasyUnidbgBootServerApplication.class, args);
    }

    /**
     * Initializes modules from the database and starts hot-reload if requested.
     * Supports --F= (static class loading, repeatable) and --U= (hot-reload watch directory).
     */
    @Bean
    public CommandLineRunner run(ApplicationContext context) {
        return args -> {
            adminService.reloadAllFromDb();

            boolean isHotUpdate = false;
            String taskPath = "";
            List<String> fList = new ArrayList<>();
            for (String arg : args) {
                if (arg.startsWith("--F=")) {
                    String classPath = arg.split("=")[1];
                    addModule(classPath);
                    fList.add(classPath);
                }
                if (arg.startsWith("--U=") && !isHotUpdate) {
                    isHotUpdate = true;
                    taskPath = arg.split("=")[1];
                }
            }
            if (isHotUpdate) {
                hotReloadScheduler = Executors.newSingleThreadScheduledExecutor();
                String finalTaskPath = taskPath;
                hotReloadScheduler.scheduleAtFixedRate(() -> {
                    try {
                        List<String> fileList = getFileList(finalTaskPath);
                        fileList.addAll(fList);
                        List<String> existMd5List = new ArrayList<>();
                        for (String classPath : fileList) {
                            String md5 = addModule(classPath);
                            if (!md5.isEmpty()) {
                                existMd5List.add(md5);
                            }
                        }
                        // Unload modules whose files have been removed
                        for (String module : moduleContainer.getMapKey().keySet()) {
                            if (!existMd5List.contains(moduleContainer.getMapKey().get(module))) {
                                moduleContainer.deleteModule(module);
                                log.info("Unloaded module: {}", module);
                            }
                        }
                    } catch (Throwable e) {
                        log.error("Hot reload error", e);
                    }
                }, 0, 5, TimeUnit.SECONDS);
            }
        };
    }

    /**
     * Loads a .class file from disk, defines it via a custom ClassLoader,
     * and registers it in ModuleContainer. Skips modules marked as OFFLINE in DB.
     */
    private String addModule(String classPath) {
        try {
            byte[] classData = loadClassData(classPath);
            String md5 = Md5Utils.getMD5(classData);
            if (!moduleContainer.getMapKey().containsValue(md5)) {
                MyClassLoader cl = new MyClassLoader();
                Class<?> dynamicClass = cl.defineClassFromBytes(classData);
                String name = dynamicClass.getName();
                Optional<ModuleEntity> dbEntity = moduleEntityRepository.findByModuleName(name);
                if (dbEntity.isPresent() && "OFFLINE".equals(dbEntity.get().getStatus())) {
                    log.info("Skipping offline module: {}", name);
                    return md5;
                }
                if (!moduleContainer.getMapKey().containsKey(name)) {
                    moduleContainer.addModule(name, dynamicClass, md5);
                    log.info("Module loaded: {}", name);
                } else {
                    moduleContainer.updateModule(name, dynamicClass, md5);
                    log.info("Module updated: {}", name);
                }
            }
            return md5;
        } catch (Throwable e) {
            log.error("Failed to load module: {}", classPath, e);
        }
        return "";
    }

    /** Recursively collects all .class files under the given directory. */
    private List<String> getFileList(String taskPath) {
        File taskDir = new File(taskPath);
        File[] files = taskDir.listFiles();
        List<String> fileList = new ArrayList<>();
        if (files == null) return fileList;
        for (File file : files) {
            if (file.isDirectory()) {
                fileList.addAll(getFileList(file.getAbsolutePath()));
            }
            if (file.isFile() && file.getName().endsWith(".class")) {
                fileList.add(file.getAbsolutePath());
            }
        }
        return fileList;
    }

    /** Reads the full contents of a file into a byte array. */
    /** Reads the full contents of a class file, retrying until all bytes are read. */
    private byte[] loadClassData(String classPath) throws IOException {
        File file = new File(classPath);
        byte[] buffer = new byte[(int) file.length()];
        try (FileInputStream inputStream = new FileInputStream(file)) {
            int offset = 0;
            while (offset < buffer.length) {
                int read = inputStream.read(buffer, offset, buffer.length - offset);
                if (read == -1) break;
                offset += read;
            }
            return buffer;
        }
    }

    @PreDestroy
    public void shutdown() {
        if (hotReloadScheduler != null) {
            hotReloadScheduler.shutdownNow();
        }
    }
}

/** Custom ClassLoader that defines a class directly from raw bytecode bytes. */
class MyClassLoader extends ClassLoader {
    public Class<?> defineClassFromBytes(byte[] classData) {
        return defineClass(null, classData, 0, classData.length);
    }
}
