package com.easy.unidbg.controller;

import com.easy.unidbg.components.ModuleContainer;
import com.easy.unidbg.components.SystemOutCapture;
import com.easy.unidbg.dto.ResultDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;


@RestController
@RequestMapping("/api/common")
public class ApiCommonController {
    @Autowired
    private ModuleContainer moduleContainer;
    @Autowired
    public SystemOutCapture systemOutCapture;

    @GetMapping("/get")
    public ResultDTO<String> get(@RequestParam("module") String module, @RequestParam(value = "params", required = false, defaultValue = "") String[] params) {
        Class<?> dynamicClass = moduleContainer.getModule(module);
        if (dynamicClass == null) {
            return ResultDTO.error(500, "模块不存在");
        }
        String result = null;
        try {
            Method mainMethod = dynamicClass.getMethod("main", String[].class);
            systemOutCapture.startCapture();
            mainMethod.invoke(null, (Object) params);
            result = systemOutCapture.getCapturedOutput();
        } catch (Throwable t) {
            t.printStackTrace();
            return ResultDTO.error(505, "未知异常");
        } finally {
            systemOutCapture.stopCapture();
        }

        return ResultDTO.success(result);
    }
}

