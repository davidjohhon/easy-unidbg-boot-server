package com.easy.unidbg.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Simple redirect from the root URL to the admin login page. */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/admin/login";
    }
}
