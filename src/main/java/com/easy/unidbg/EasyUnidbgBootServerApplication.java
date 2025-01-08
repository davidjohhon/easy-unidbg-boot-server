package com.easy.unidbg;

import com.easy.unidbg.components.ModuleContainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.io.FileInputStream;
import java.io.IOException;

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
            for (String arg : args) {
                if (arg.startsWith("--F=")) {
                    String classPath = arg.split("=")[1];
                    byte[] classData = loadClassData(classPath);
                    Class<?> dynamicClass = moduleContainer.defineClassFromBytes(classData);
                    log.info("模块==> " + dynamicClass.getName());
                    moduleContainer.addModule(dynamicClass.getName(), dynamicClass);
                }
            }
        };
    }

    private byte[] loadClassData(String classPath) throws IOException {
        FileInputStream inputStream = new FileInputStream(classPath);
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        inputStream.close();
        return buffer;
    }
}
