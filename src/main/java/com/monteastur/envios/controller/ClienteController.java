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

    @GetMapping("/login")
    public String login(HttpSession session, Model model) {
        if (session.getAttribute("clienteId") != null) {
            return "redirect:/cliente/panel";
        }
        return "cliente/login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email,
                          @RequestParam String password,
                          HttpSession session,
                          RedirectAttributes ra) {
        var opt = clienteService.autenticar(email, password);
        if (opt.isPresent()) {
            session.setAttribute("clienteId", opt.get().getId());
            session.setAttribute("clienteNombre", opt.get().getNombre());
            return "redirect:/cliente/panel";
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
}
