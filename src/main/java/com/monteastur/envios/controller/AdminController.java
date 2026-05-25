package com.monteastur.envios.controller;

import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EvidenciaEnvio;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.Imagen;
import com.monteastur.envios.model.MensajeContacto;
import com.monteastur.envios.model.Reserva;
import com.monteastur.envios.model.TextoLegal;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.ImagenRepository;
import com.monteastur.envios.repository.MensajeContactoRepository;
import com.monteastur.envios.repository.ReservaRepository;
import com.monteastur.envios.repository.TextoLegalRepository;
import com.monteastur.envios.service.EmailService;
import com.monteastur.envios.service.EvidenciaEnvioService;
import com.monteastur.envios.service.EventoTrackingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ReservaRepository reservaRepo;
    private final ImagenRepository imagenRepo;
    private final MensajeContactoRepository mensajeRepo;
    private final TextoLegalRepository textoRepo;
    private final EnvioTrackingRepository trackingRepo;
    private final EmailService emailService;
    private final ClienteRepository clienteRepo;
    private final EvidenciaEnvioService evidenciaService;
    private final EventoTrackingService eventoTrackingService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public AdminController(ReservaRepository reservaRepo, ImagenRepository imagenRepo,
                           MensajeContactoRepository mensajeRepo, TextoLegalRepository textoRepo,
                           EnvioTrackingRepository trackingRepo, EmailService emailService,
                           ClienteRepository clienteRepo, EvidenciaEnvioService evidenciaService,
                           EventoTrackingService eventoTrackingService) {
        this.reservaRepo = reservaRepo;
        this.imagenRepo = imagenRepo;
        this.mensajeRepo = mensajeRepo;
        this.textoRepo = textoRepo;
        this.trackingRepo = trackingRepo;
        this.emailService = emailService;
        this.clienteRepo = clienteRepo;
        this.evidenciaService = evidenciaService;
        this.eventoTrackingService = eventoTrackingService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalReservas", reservaRepo.count());
        model.addAttribute("reservasPendientes", reservaRepo.countByEstado("pendiente"));
        model.addAttribute("totalMensajes", mensajeRepo.count());
        model.addAttribute("totalImagenes", imagenRepo.count());
        model.addAttribute("ultimasReservas", reservaRepo.findTop5ByOrderByCreatedAtDesc());
        model.addAttribute("ultimosMensajes", mensajeRepo.findTop5ByOrderByFechaEnvioDesc());
        return "cms/dashboard";
    }

    @GetMapping("/mensajesrecibidos")
    public String mensajesRecibidos(Model model) {
        model.addAttribute("mensajes", mensajeRepo.findAllByOrderByFechaEnvioDesc());
        return "cms/contactos";
    }

    @GetMapping("/reservas")
    public String reservas(Model model) {
        model.addAttribute("reservas", reservaRepo.findAllByOrderByCreatedAtDesc());
        return "cms/reservas";
    }

    @PostMapping("/reservas/aprobar/{id}")
    public String aprobarReserva(@PathVariable Long id) {
        reservaRepo.findById(id).ifPresent(r -> {
            r.setEstado("aprobada");
            reservaRepo.save(r);
            emailService.notificarReservaAprobada(r);
        });
        return "redirect:/admin/reservas";
    }

    @PostMapping("/reservas/cancelar/{id}")
    public String cancelarReserva(@PathVariable Long id) {
        reservaRepo.findById(id).ifPresent(r -> {
            r.setEstado("cancelada");
            reservaRepo.save(r);
        });
        return "redirect:/admin/reservas";
    }

    @PostMapping("/reservas/eliminar/{id}")
    public String eliminarReserva(@PathVariable Long id) {
        reservaRepo.deleteById(id);
        return "redirect:/admin/reservas";
    }

    @GetMapping("/imagenes")
    public String imagenes(Model model) {
        try {
            model.addAttribute("imagenes", imagenRepo.findAllByOrderByOrdenAsc());
        } catch (Exception e) {
            model.addAttribute("imagenes", Collections.emptyList());
            model.addAttribute("error", "Error al cargar las imágenes: " + e.getMessage());
        }
        return "cms/imagenes";
    }

    @PostMapping("/imagenes")
    public String subirImagen(@RequestParam String titulo,
                               @RequestParam(required = false) String descripcion,
                               @RequestParam(required = false) String categoria,
                               @RequestParam Integer orden,
                               @RequestParam("archivo") MultipartFile archivo,
                               RedirectAttributes ra) {

        if (archivo.isEmpty()) {
            ra.addFlashAttribute("error", "Debes seleccionar un archivo.");
            return "redirect:/admin/imagenes";
        }

        try {
            String dir = uploadDir.endsWith("/") || uploadDir.endsWith("\\") ? uploadDir : uploadDir + "/";
            File dirFile = new File(dir);
            if (!dirFile.exists()) dirFile.mkdirs();

            String originalName = archivo.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String uniqueName = UUID.randomUUID().toString() + extension;
            Path rutaCompleta = Paths.get(dir + uniqueName);
            Files.write(rutaCompleta, archivo.getBytes());

            String url = "/uploads/" + uniqueName;
            Imagen img = new Imagen(titulo, descripcion, url, categoria, orden);
            imagenRepo.save(img);

            ra.addFlashAttribute("exito", "Imagen subida correctamente.");
        } catch (IOException e) {
            ra.addFlashAttribute("error", "Error al guardar el archivo: " + e.getMessage());
        }

        return "redirect:/admin/imagenes";
    }

    @PostMapping("/imagenes/eliminar/{id}")
    public String eliminarImagen(@PathVariable Long id, RedirectAttributes ra) {
        try {
            var opt = imagenRepo.findById(id);
            if (opt.isPresent()) {
                Imagen img = opt.get();
                // Delete file from filesystem
                String url = img.getUrl();
                if (url != null && url.startsWith("/uploads/")) {
                    String fileName = url.substring("/uploads/".length());
                    String dir = uploadDir.endsWith("/") || uploadDir.endsWith("\\") ? uploadDir : uploadDir + "/";
                    Path filePath = Paths.get(dir + fileName);
                    try {
                        Files.deleteIfExists(filePath);
                    } catch (IOException ignored) {}
                }
                imagenRepo.delete(img);
                ra.addFlashAttribute("exito", "Imagen eliminada correctamente.");
            } else {
                ra.addFlashAttribute("error", "Imagen no encontrada.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al eliminar: " + e.getMessage());
        }
        return "redirect:/admin/imagenes";
    }

    @GetMapping("/textos")
    public String textos(Model model) {
        model.addAttribute("avisoLegal", textoRepo.findBySlug("aviso-legal").orElse(null));
        model.addAttribute("politicaCookies", textoRepo.findBySlug("politica-cookies").orElse(null));
        return "cms/textos";
    }

    @GetMapping("/tracking")
    public String listarTracking(Model model) {
        model.addAttribute("envios", trackingRepo.findAllByOrderByUltimaActualizacionDesc());
        return "cms/tracking";
    }

    @GetMapping("/tracking/nuevo")
    public String nuevoTracking(Model model) {
        long count = trackingRepo.count() + 1;
        String codigo = String.format("MT-%d-%04d", java.time.LocalDateTime.now().getYear(), count);
        model.addAttribute("envio", new EnvioTracking());
        model.addAttribute("codigoSugerido", codigo);
        model.addAttribute("clientes", clienteRepo.findAll());
        return "cms/tracking-form";
    }

    @GetMapping("/tracking/editar/{id}")
    public String editarTracking(@PathVariable Long id, Model model, RedirectAttributes ra) {
        EnvioTracking envio = trackingRepo.findWithClienteById(id).orElse(null);
        if (envio == null) {
            ra.addFlashAttribute("error", "Envío no encontrado.");
            return "redirect:/admin/tracking";
        }
        model.addAttribute("envio", envio);
        model.addAttribute("clientes", clienteRepo.findAll());
        model.addAttribute("evidencias", evidenciaService.listarPorEnvio(id));
        model.addAttribute("eventos", eventoTrackingService.listarPorEnvio(id));
        return "cms/tracking-form";
    }

    @PostMapping("/tracking/guardar")
    public String guardarTracking(@RequestParam(required = false) Long id,
                                   @RequestParam String codigoUnico,
                                   @RequestParam String estado,
                                   @RequestParam String destinatario,
                                   @RequestParam(required = false) String origen,
                                   @RequestParam(required = false) String destino,
                                   @RequestParam(required = false) String peso,
                                   @RequestParam(required = false) String contenido,
                                   @RequestParam(required = false) String observaciones,
                                   @RequestParam(required = false) Long clienteId,
                                   RedirectAttributes ra) {
        String estadoAnterior = null;
        boolean esNuevo = (id == null);
        EnvioTracking envio;
        if (id != null) {
            envio = trackingRepo.findById(id).orElse(new EnvioTracking());
            estadoAnterior = envio.getEstado();
            envio.setId(id);
        } else {
            envio = new EnvioTracking();
            envio.setFechaCreacion(java.time.LocalDateTime.now());
        }
        envio.setCodigoUnico(codigoUnico.trim().toUpperCase());
        envio.setEstado(estado);
        envio.setDestinatario(destinatario);
        envio.setOrigen(origen);
        envio.setDestino(destino);
        envio.setPeso(peso);
        envio.setContenido(contenido);
        envio.setObservaciones(observaciones);
        envio.setUltimaActualizacion(java.time.LocalDateTime.now());
        if (clienteId != null) {
            clienteRepo.findById(clienteId).ifPresent(envio::setCliente);
        }
        trackingRepo.save(envio);
        if (esNuevo) {
            eventoTrackingService.crearEventoInicial(envio);
        } else {
            eventoTrackingService.crearEvento(envio, estadoAnterior);
        }
        ra.addFlashAttribute("exito", "Envío guardado correctamente.");
        return "redirect:/admin/tracking";
    }

    @PostMapping("/tracking/eliminar/{id}")
    public String eliminarTracking(@PathVariable Long id, RedirectAttributes ra) {
        trackingRepo.deleteById(id);
        ra.addFlashAttribute("exito", "Envío eliminado.");
        return "redirect:/admin/tracking";
    }

    @PostMapping("/textos")
    public String guardarTexto(@RequestParam String slug,
                               @RequestParam String titulo,
                               @RequestParam String contenido) {
        textoRepo.findBySlug(slug).ifPresent(t -> {
            t.setTitulo(titulo);
            t.setContenido(contenido);
            t.setUpdatedAt(java.time.LocalDateTime.now());
            textoRepo.save(t);
        });
        return "redirect:/admin/textos";
    }

    @PostMapping("/tracking/evidencia/{envioId}")
    public String subirEvidencia(@PathVariable Long envioId,
                                  @RequestParam String titulo,
                                  @RequestParam(required = false) String descripcion,
                                  @RequestParam String tipo,
                                  @RequestParam("archivo") MultipartFile archivo,
                                  RedirectAttributes ra) {
        var optEnvio = trackingRepo.findById(envioId);
        if (optEnvio.isEmpty()) {
            ra.addFlashAttribute("error", "Envío no encontrado.");
            return "redirect:/admin/tracking";
        }
        if (archivo.isEmpty()) {
            ra.addFlashAttribute("error", "Debes seleccionar un archivo.");
            return "redirect:/admin/tracking/editar/" + envioId;
        }
        if (!"FOTO".equals(tipo) && !"DOCUMENTO".equals(tipo)) {
            ra.addFlashAttribute("error", "Tipo de evidencia no válido.");
            return "redirect:/admin/tracking/editar/" + envioId;
        }
        String originalName = archivo.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            ra.addFlashAttribute("error", "Nombre de archivo no válido.");
            return "redirect:/admin/tracking/editar/" + envioId;
        }
        String extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        String[] permitidas = {".jpg", ".jpeg", ".png", ".webp", ".pdf"};
        boolean extensionValida = false;
        for (String ext : permitidas) {
            if (ext.equals(extension)) { extensionValida = true; break; }
        }
        if (!extensionValida) {
            ra.addFlashAttribute("error", "Tipo de archivo no permitido. Solo se aceptan: JPG, PNG, WEBP, PDF.");
            return "redirect:/admin/tracking/editar/" + envioId;
        }
        try {
            String uploadsDir = System.getProperty("user.dir") + "/uploads/evidencias/";
            File dir = new File(uploadsDir);
            if (!dir.exists()) dir.mkdirs();

            String uniqueName = UUID.randomUUID().toString() + extension;
            Path rutaCompleta = Paths.get(uploadsDir + uniqueName);
            Files.write(rutaCompleta, archivo.getBytes());

            EvidenciaEnvio evidencia = new EvidenciaEnvio();
            evidencia.setEnvioTracking(optEnvio.get());
            evidencia.setTitulo(titulo);
            evidencia.setDescripcion(descripcion);
            evidencia.setTipo(tipo);
            evidencia.setUrlArchivo("/uploads/evidencias/" + uniqueName);
            evidenciaService.guardar(evidencia);

            ra.addFlashAttribute("exito", "Evidencia subida correctamente.");
        } catch (IOException e) {
            ra.addFlashAttribute("error", "Error al guardar el archivo: " + e.getMessage());
        }
        return "redirect:/admin/tracking/editar/" + envioId;
    }

    @PostMapping("/tracking/evidencia/toggle/{id}")
    public String toggleEvidencia(@PathVariable Long id, @RequestParam Long envioId, RedirectAttributes ra) {
        try {
            evidenciaService.toggleVisibilidad(id);
            ra.addFlashAttribute("exito", "Visibilidad actualizada.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al cambiar visibilidad: " + e.getMessage());
        }
        return "redirect:/admin/tracking/editar/" + envioId;
    }

    @PostMapping("/tracking/evidencia/eliminar/{id}")
    public String eliminarEvidencia(@PathVariable Long id,
                                     @RequestParam Long envioId,
                                     RedirectAttributes ra) {
        try {
            var opt = evidenciaService.buscar(id);
            if (opt.isPresent()) {
                EvidenciaEnvio ev = opt.get();
                String url = ev.getUrlArchivo();
                if (url != null && url.startsWith("/uploads/evidencias/")) {
                    String fileName = url.substring("/uploads/evidencias/".length());
                    Path filePath = Paths.get(System.getProperty("user.dir") + "/uploads/evidencias/" + fileName);
                    try {
                        Files.deleteIfExists(filePath);
                    } catch (IOException ignored) {}
                }
                evidenciaService.eliminar(id);
                ra.addFlashAttribute("exito", "Evidencia eliminada.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al eliminar: " + e.getMessage());
        }
        return "redirect:/admin/tracking/editar/" + envioId;
    }
}
