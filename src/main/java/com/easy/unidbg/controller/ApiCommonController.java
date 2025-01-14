package com.easy.unidbg.controller;

import com.easy.unidbg.components.ModuleContainer;
import com.easy.unidbg.components.SystemOutCapture;
import com.easy.unidbg.dto.InvokeVo;
import com.easy.unidbg.dto.ResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.lang.reflect.Method;


@RestController
@RequestMapping("/api/common")
@Slf4j
public class ApiCommonController {
    @Autowired
    private ModuleContainer moduleContainer;
    @Autowired
    public SystemOutCapture systemOutCapture;

    @GetMapping("/invoke")
    public ResultDTO<String> get(@Valid @NotBlank @RequestParam("module") String module, @RequestParam(value = "args", required = false, defaultValue = "") String[] params) {
        return handleRequest(module, params);
    }

    @PostMapping("/invoke")
    public ResultDTO<String> post(@Valid @RequestBody InvokeVo invokeVo) {
        return handleRequest(invokeVo.getModule(), invokeVo.getArgs().toArray(new String[0]));
    }

    private ResultDTO<String> handleRequest(String module, String[] params) {
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

