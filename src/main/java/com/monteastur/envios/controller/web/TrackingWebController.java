package com.monteastur.envios.controller.web;

import com.monteastur.envios.dto.web.PublicTrackingView;
import com.monteastur.envios.exception.TrackingNoEncontradoException;
import com.monteastur.envios.service.web.PublicTrackingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Portal público de rastreo modernizado (Tailwind). El buscador sigue PRG:
 * POST -> redirect a /tracking/{codigo} -> GET con la vista cacheada.
 */
@Controller
public class TrackingWebController {

    private final PublicTrackingService publicTrackingService;

    public TrackingWebController(PublicTrackingService publicTrackingService) {
        this.publicTrackingService = publicTrackingService;
    }

    @GetMapping({"/tracking", "/en/tracking"})
    public String formulario(Model model) {
        model.addAttribute("buscado", false);
        return "tracking-search";
    }

    @PostMapping({"/tracking", "/en/tracking"})
    public String buscar(@RequestParam String codigo, Model model) {
        String codigoNormalizado = codigo.trim().toUpperCase();
        PublicTrackingView view = publicTrackingService.cargarPagina(codigoNormalizado);
        if (view == null) {
            model.addAttribute("buscado", true);
            model.addAttribute("error", true);
            model.addAttribute("codigo", codigoNormalizado);
            return "tracking-search";
        }
        return "redirect:/tracking/" + codigoNormalizado;
    }

    @GetMapping({"/tracking/{codigo}", "/en/tracking/{codigo}"})
    public String resultado(@PathVariable String codigo, Model model) {
        String codigoNormalizado = codigo.trim().toUpperCase();
        PublicTrackingView view = publicTrackingService.cargarPagina(codigoNormalizado);
        if (view == null) {
            throw new TrackingNoEncontradoException(codigoNormalizado);
        }
        model.addAttribute("view", view);
        return "tracking-result";
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(TrackingNoEncontradoException.class)
    public String handleNoEncontrado(TrackingNoEncontradoException ex, Model model) {
        model.addAttribute("codigo", ex.getCodigo());
        return "tracking-404";
    }
}
