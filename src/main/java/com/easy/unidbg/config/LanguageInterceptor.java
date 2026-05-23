package com.easy.unidbg.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * Interceptor that handles language switching via the "lang" query parameter.
 * Accepts "en" (English) and "zh" (Chinese Simplified).
 * Stores the selection in both a custom session attribute and the
 * SessionLocaleResolver attribute for Thymeleaf and MessageSource to use.
 */
@Component
public class LanguageInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String lang = request.getParameter("lang");
        if (lang != null && (lang.equals("en") || lang.equals("zh"))) {
            request.getSession().setAttribute("LANG", lang);
            Locale locale = lang.equals("zh") ? Locale.SIMPLIFIED_CHINESE : Locale.ENGLISH;
            request.getSession().setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, locale);
        }
        return true;
    }
}
