package com.grupb2.casarural.controller;

import com.grupb2.casarural.model.Cliente;
import com.grupb2.casarural.model.EnvioTracking;
import com.grupb2.casarural.repository.EnvioTrackingRepository;
import com.grupb2.casarural.service.ClienteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/cliente")
public class ClienteController {

    private final ClienteService clienteService;
    private final EnvioTrackingRepository trackingRepo;

    public ClienteController(ClienteService clienteService, EnvioTrackingRepository trackingRepo) {
        this.clienteService = clienteService;
        this.trackingRepo = trackingRepo;
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

    @GetMapping("/panel")
    public String panel(HttpSession session, Model model) {
        Long clienteId = (Long) session.getAttribute("clienteId");
        if (clienteId == null) {
            return "redirect:/cliente/login";
        }
        var opt = clienteService.buscarPorId(clienteId);
        if (opt.isEmpty()) {
            session.invalidate();
            return "redirect:/cliente/login";
        }
        Cliente cliente = opt.get();
        List<EnvioTracking> envios = trackingRepo.findByClienteIdOrderByUltimaActualizacionDesc(clienteId);
        model.addAttribute("cliente", cliente);
        model.addAttribute("envios", envios);
        return "cliente/panel";
    }
}
