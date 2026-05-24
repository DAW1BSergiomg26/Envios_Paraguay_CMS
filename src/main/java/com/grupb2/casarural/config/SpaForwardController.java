package com.grupb2.casarural.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping(value = {"/login-react", "/dashboard", "/dashboard/**"})
    public String forward() {
        return "forward:/index.html";
    }
}
