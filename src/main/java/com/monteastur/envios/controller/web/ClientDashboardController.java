package com.monteastur.envios.controller.web;

import com.monteastur.envios.dto.web.ClientDashboardView;
import com.monteastur.envios.exception.ForbiddenException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.service.ClienteService;
import com.monteastur.envios.service.DocumentoPdfService;
import com.monteastur.envios.service.web.ClientDashboardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URI;

/**
 * Panel de cliente autenticado por sesión (clienteId en HttpSession).
 * El dashboard se cachea en Redis y la etiqueta PDF solo es descargable
 * por el propietario del envío (ajeno -> 403, inexistente -> 404).
 */
@Controller
@RequestMapping("/cliente")
public class ClientDashboardController {

    private final ClientDashboardService dashboardService;
    private final DocumentoPdfService documentoPdfService;
    private final EnvioTrackingRepository envioTrackingRepository;
    private final ClienteService clienteService;

    public ClientDashboardController(ClientDashboardService dashboardService,
                                     DocumentoPdfService documentoPdfService,
                                     EnvioTrackingRepository envioTrackingRepository,
                                     ClienteService clienteService) {
        this.dashboardService = dashboardService;
        this.documentoPdfService = documentoPdfService;
        this.envioTrackingRepository = envioTrackingRepository;
        this.clienteService = clienteService;
    }

    @GetMapping("/panel")
    public String panel(HttpSession session, Model model) {
        Cliente cliente = clienteAutenticado(session);
        if (cliente == null) {
            return "redirect:/cliente/login";
        }
        ClientDashboardView dashboard = dashboardService.cargarDashboard(cliente.getId());
        model.addAttribute("panel", dashboard);
        return "cliente/panel";
    }

    @GetMapping("/panel/envio/{codigo}/etiqueta")
    public ResponseEntity<byte[]> descargarEtiqueta(@PathVariable String codigo, HttpSession session) {
        Cliente cliente = clienteAutenticado(session);
        if (cliente == null) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/cliente/login")).build();
        }
        String codigoNormalizado = codigo.trim().toUpperCase();
        EnvioTracking envio = envioTrackingRepository.findWithClienteByCodigoUnico(codigoNormalizado)
                .orElseThrow(() -> new ResourceNotFoundException("Tracking no encontrado: " + codigoNormalizado));
        boolean propio = envio.getCliente() != null && cliente.getId().equals(envio.getCliente().getId());
        if (!propio) {
            throw new ForbiddenException("El envío " + codigoNormalizado + " no pertenece al cliente autenticado");
        }
        byte[] pdf = documentoPdfService.generarEtiqueta(codigoNormalizado, "cliente:" + cliente.getEmail());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"etiqueta-" + codigoNormalizado + ".pdf\"")
                .body(pdf);
    }

    private Cliente clienteAutenticado(HttpSession session) {
        Long clienteId = (Long) session.getAttribute("clienteId");
        if (clienteId == null) {
            return null;
        }
        return clienteService.buscarPorId(clienteId)
                .orElseGet(() -> {
                    session.invalidate();
                    return null;
                });
    }
}
