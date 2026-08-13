package com.monteastur.envios.controller;

import com.monteastur.envios.service.ClienteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cliente")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public String index(HttpSession session) {
        if (session.getAttribute("clienteId") != null) {
            return "redirect:/cliente/panel";
        }
        return "redirect:/cliente/login?redirect=/cliente/panel";
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "redirect", required = false) String redirect,
                        HttpSession session, Model model) {
        if (session.getAttribute("clienteId") != null) {
            return "redirect:/cliente/panel";
        }
        model.addAttribute("redirect", safeRedirect(redirect));
        return "cliente/login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email,
                          @RequestParam String password,
                          @RequestParam(value = "redirect", required = false) String redirect,
                          HttpSession session,
                          RedirectAttributes ra) {
        var opt = clienteService.autenticar(email, password);
        if (opt.isPresent()) {
            session.setAttribute("clienteId", opt.get().getId());
            session.setAttribute("clienteNombre", opt.get().getNombre());
            return "redirect:" + safeRedirect(redirect);
        }
        ra.addFlashAttribute("error", "Email o contraseña incorrectos.");
        return "redirect:/cliente/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/cliente/login";
    }

    @PostMapping("/logout")
    public String logoutPost(HttpSession session) {
        session.invalidate();
        return "redirect:/cliente/login";
    }

    private String safeRedirect(String redirect) {
        if (redirect != null
                && redirect.startsWith("/cliente")
                && !redirect.startsWith("//")
                && !redirect.contains(":")) {
            return redirect;
        }
        return "/cliente/panel";
    }
}
