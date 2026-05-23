package com.easy.unidbg.controller;

import com.easy.unidbg.components.ModuleContainer;
import com.easy.unidbg.components.SystemOutCapture;
import com.easy.unidbg.dto.InvokeVo;
import com.easy.unidbg.dto.ResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.lang.reflect.Method;

/**
 * Core API controller for invoking loaded unidbg modules.
 * Supports both GET and POST /api/common/invoke.
 * Authentication: requires a valid "apikey" query parameter (enforced by ApiKeyInterceptor).
 * Captures System.out output from the module's main() and returns it as JSON.
 * Error messages are i18n-aware based on the request locale.
 */
@RestController
@RequestMapping("/api/common")
@Slf4j
public class ApiCommonController {

    @Autowired
    private ModuleContainer moduleContainer;

    @Autowired
    private SystemOutCapture systemOutCapture;

    @Autowired
    private MessageSource messageSource;

    @GetMapping("/invoke")
    public ResultDTO<String> get(@Valid @NotBlank @RequestParam("module") String module,
                                  @RequestParam(value = "args", required = false, defaultValue = "") String[] params,
                                  HttpServletRequest request) {
        request.setAttribute("log_module", module);
        request.setAttribute("log_args", String.join(",", params));
        return handleRequest(module, params, request);
    }

    @PostMapping("/invoke")
    public ResultDTO<String> post(@Valid @RequestBody InvokeVo invokeVo, HttpServletRequest request) {
        request.setAttribute("log_module", invokeVo.getModule());
        request.setAttribute("log_args", String.join(",", invokeVo.getArgs() != null ? invokeVo.getArgs() : java.util.Collections.emptyList()));
        return handleRequest(invokeVo.getModule(), invokeVo.getArgs() != null
                ? invokeVo.getArgs().toArray(new String[0]) : new String[0], request);
    }

    private ResultDTO<String> handleRequest(String module, String[] params, HttpServletRequest request) {
        Class<?> dynamicClass = moduleContainer.getModule(module);
        if (dynamicClass == null) {
            return buildError(request, 500, "api.moduleNotFound");
        }
        try {
            Method mainMethod = dynamicClass.getMethod("main", String[].class);
            systemOutCapture.startCapture();
            try {
                mainMethod.invoke(null, (Object) params);
            } finally {
                systemOutCapture.stopCapture();
            }
            String result = systemOutCapture.getCapturedOutput();
            ResultDTO<String> success = ResultDTO.success(result);
            request.setAttribute("log_response", success.getData());
            request.setAttribute("log_status", "ok");
            return success;
        } catch (Throwable t) {
            log.error("Module execution failed: {}", module, t);
            return buildError(request, 505, "api.unknownError");
        }
    }

    private ResultDTO<String> buildError(HttpServletRequest request, int code, String msgKey) {
        String msg = messageSource.getMessage(msgKey, null, RequestContextUtils.getLocale(request));
        ResultDTO<String> err = ResultDTO.error(code, msg);
        request.setAttribute("log_response", err.getErrorMsg());
        request.setAttribute("log_status", "error");
        return err;
    }
}
