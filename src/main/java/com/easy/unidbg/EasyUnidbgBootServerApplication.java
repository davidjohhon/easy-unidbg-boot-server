package com.easy.unidbg;

import com.easy.unidbg.components.ModuleContainer;
import com.easy.unidbg.utils.Md5Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@Slf4j
public class EasyUnidbgBootServerApplication {
    @Autowired
    private ModuleContainer moduleContainer;

    public static void main(String[] args) {
        SpringApplication.run(EasyUnidbgBootServerApplication.class, args);
    }

    @Bean
    public CommandLineRunner run(ApplicationContext context) {
        return args -> {
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
                ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                String finalTaskPath = taskPath;
                scheduledExecutorService.scheduleAtFixedRate(() -> {
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
                        for (String module : moduleContainer.getMapKey().keySet()) {
                            if (!existMd5List.contains(moduleContainer.getMapKey().get(module))) {
                                moduleContainer.deleteModule(module);
                                log.info("模块已卸载==>" + module);
                            }
                        }

                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }, 0, 5, TimeUnit.SECONDS);
            }
        };
    }

    private String addModule(String classPath) {
        try {
            byte[] classData = loadClassData(classPath);
            String md5 = Md5Utils.getMD5(classData);

            if (!moduleContainer.getMapKey().containsValue(md5)) {
                MyClassLoader myClassLoader = new MyClassLoader();
                Class<?> dynamicClass = myClassLoader.defineClassFromBytes(classData);
                String name = dynamicClass.getName();
                log.info("name:" + name + "==>md5==>" + md5);
                if (!moduleContainer.getMapKey().containsKey(name)) {
                    moduleContainer.addModule(dynamicClass.getName(), dynamicClass, md5);
                    log.info("模块已新增==>" + dynamicClass.getName());
                } else {
                    moduleContainer.updateModule(dynamicClass.getName(), dynamicClass, md5);
                    log.info("模块已刷新==>" + dynamicClass.getName());
                }
            }
            return md5;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "";

    }

    private List<String> getFileList(String taskPath) {
        File taskDir = new File(taskPath);
        File[] files = taskDir.listFiles();

        List<String> fileList = new ArrayList<>();
        if (files == null) {
            return fileList;
        }
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

    private byte[] loadClassData(String classPath) throws IOException {
        FileInputStream inputStream = new FileInputStream(classPath);
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        inputStream.close();
        return buffer;
    }
}

class MyClassLoader extends ClassLoader {
    public Class<?> defineClassFromBytes(byte[] classData) {
        return defineClass(null, classData, 0, classData.length);
    }
}
