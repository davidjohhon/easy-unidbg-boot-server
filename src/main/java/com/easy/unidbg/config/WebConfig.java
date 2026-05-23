package com.easy.unidbg.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

/**
 * Spring MVC configuration.
 * Registers three interceptors:
 * 1. LanguageInterceptor — handles ?lang=en|zh for i18n
 * 2. ApiKeyInterceptor — validates apikey on /api/** requests
 * 3. AccessLogInterceptor — logs all /api/** request/response data
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AccessLogInterceptor accessLogInterceptor;

    @Autowired
    private LanguageInterceptor languageInterceptor;

    @Autowired
    @Lazy
    private ApiKeyInterceptor apiKeyInterceptor;

    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(languageInterceptor);
        registry.addInterceptor(apiKeyInterceptor)
                .addPathPatterns("/api/**");
        registry.addInterceptor(accessLogInterceptor)
                .addPathPatterns("/api/**");
    }
}
