package com.easy.unidbg.config;

import com.easy.unidbg.entity.AccessLog;
import com.easy.unidbg.service.AccessLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interceptor that records every API call to /api/common/invoke.
 * Captures module name, request args, response data, client IP, and status.
 * Uses ThreadLocal to pass data from preHandle to afterCompletion.
 * Data is set as request attributes by ApiCommonController.
 */
@Component
public class AccessLogInterceptor implements HandlerInterceptor {

    @Autowired
    private AccessLogService accessLogService;

    private static final ThreadLocal<AccessLog> logHolder = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        AccessLog log = new AccessLog();
        log.setClientIp(getClientIp(request));
        logHolder.set(log);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AccessLog log = logHolder.get();
        if (log != null) {
            String module = (String) request.getAttribute("log_module");
            if (module != null) log.setModuleName(module);
            String args = (String) request.getAttribute("log_args");
            if (args != null) log.setRequestArgs(args);
            String resp = (String) request.getAttribute("log_response");
            if (resp != null) log.setResponseData(resp);
            String status = (String) request.getAttribute("log_status");
            log.setResponseStatus(status != null ? status : (response.getStatus() < 400 ? "ok" : "fail"));
            accessLogService.save(log);
        }
        logHolder.remove();
    }

    /** Extracts client IP, respecting X-Forwarded-For header for reverse proxy setups. */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
