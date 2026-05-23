package com.easy.unidbg.config;

import com.easy.unidbg.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interceptor that validates API keys for /api/** requests.
 * Extracts the "apikey" query parameter and verifies it against
 * SHA-256 hashed keys stored in the database via AdminService.
 * Returns 401 JSON response with "Invalid or missing API key" if validation fails.
 */
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    @Autowired
    private AdminService adminService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String apikey = request.getParameter("apikey");
        if (apikey == null || !adminService.validateApiKey(apikey)) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"errorCode\":401,\"errorMsg\":\"Invalid or missing API key\",\"status\":\"fail\"}");
            return false;
        }
        return true;
    }
}
