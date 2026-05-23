package com.easy.unidbg.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global controller advice that provides the application version
 * to all Thymeleaf templates via the "appVersion" model attribute.
 * The version is displayed in the admin login page and page footer.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private static final String APP_VERSION = "2.0.0-RELEASE";

    @ModelAttribute("appVersion")
    public String appVersion() {
        return APP_VERSION;
    }
}
