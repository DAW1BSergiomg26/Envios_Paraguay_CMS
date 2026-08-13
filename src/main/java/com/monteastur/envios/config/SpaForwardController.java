package com.monteastur.envios.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping(value = {"/login-react", "/dashboard", "/dashboard/**",
            "/react-dashboard", "/react-dashboard/"})
    public String forward() {
        return "forward:/react-dashboard/index.html";
    }

    @GetMapping({"/admin", "/admin/**"})
    public String legacyAdminRedirect() {
        return "redirect:/dashboard";
    }
}
