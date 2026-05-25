package com.monteastur.envios.controller;

import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.EvidenciaEnvio;
import com.monteastur.envios.model.EventoTracking;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.service.ClienteService;
import com.monteastur.envios.service.EvidenciaEnvioService;
import com.monteastur.envios.service.EventoTrackingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/cliente")
public class ClienteController {

    private final ClienteService clienteService;
    private final EnvioTrackingRepository trackingRepo;
    private final EvidenciaEnvioService evidenciaService;
    private final EventoTrackingService eventoTrackingService;

    public ClienteController(ClienteService clienteService, EnvioTrackingRepository trackingRepo,
                             EvidenciaEnvioService evidenciaService,
                             EventoTrackingService eventoTrackingService) {
        this.clienteService = clienteService;
        this.trackingRepo = trackingRepo;
        this.evidenciaService = evidenciaService;
        this.eventoTrackingService = eventoTrackingService;
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
        Map<Long, List<EvidenciaEnvio>> evidenciasPorEnvio = new HashMap<>();
        Map<Long, List<EventoTracking>> eventosPorEnvio = new HashMap<>();
        for (EnvioTracking e : envios) {
            evidenciasPorEnvio.put(e.getId(), evidenciaService.listarPorEnvioParaCliente(e.getId()));
            eventosPorEnvio.put(e.getId(), eventoTrackingService.listarPorEnvio(e.getId()));
        }
        model.addAttribute("cliente", cliente);
        model.addAttribute("envios", envios);
        model.addAttribute("evidenciasPorEnvio", evidenciasPorEnvio);
        model.addAttribute("eventosPorEnvio", eventosPorEnvio);
        return "cliente/panel";
    }
}
