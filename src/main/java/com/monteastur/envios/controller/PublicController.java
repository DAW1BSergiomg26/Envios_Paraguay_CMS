package com.monteastur.envios.controller;

import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.Imagen;
import com.monteastur.envios.model.MensajeContacto;
import com.monteastur.envios.model.Reserva;
import com.monteastur.envios.model.TextoLegal;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.ImagenRepository;
import com.monteastur.envios.repository.MensajeContactoRepository;
import com.monteastur.envios.repository.ReservaRepository;
import com.monteastur.envios.repository.TextoLegalRepository;
import com.monteastur.envios.service.EmailService;
import com.monteastur.envios.service.EventoTrackingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.*;

@Controller
public class PublicController {

    private final MensajeContactoRepository mensajeRepo;
    private final ReservaRepository reservaRepo;
    private final ImagenRepository imagenRepo;
    private final TextoLegalRepository textoRepo;
    private final EnvioTrackingRepository trackingRepo;
    private final EmailService emailService;
    private final EventoTrackingService eventoTrackingService;

    public PublicController(MensajeContactoRepository mensajeRepo, ReservaRepository reservaRepo,
                            ImagenRepository imagenRepo, TextoLegalRepository textoRepo,
                            EnvioTrackingRepository trackingRepo, EmailService emailService,
                            EventoTrackingService eventoTrackingService) {
        this.mensajeRepo = mensajeRepo;
        this.reservaRepo = reservaRepo;
        this.imagenRepo = imagenRepo;
        this.textoRepo = textoRepo;
        this.trackingRepo = trackingRepo;
        this.emailService = emailService;
        this.eventoTrackingService = eventoTrackingService;
    }

    @GetMapping("/")
    public String index() {
        return "home";
    }

    @GetMapping({"/casa", "/lacasa"})
    public String laCasa(Model model) {
        model.addAttribute("imagenes", imagenRepo.findAllByOrderByOrdenAsc());
        return "lacasa";
    }

    @GetMapping("/entorno")
    public String entorno() {
        return "entorno";
    }

    @GetMapping("/reservas")
    public String reservas(Model model, HttpServletRequest request) {
        request.getSession();
        model.addAttribute("reservaEnviada", false);
        model.addAttribute("calendarios", generarCalendarios(occupiedDates(), MESES_ES));
        return "reservas";
    }

    @PostMapping("/reservas")
    public String enviarReserva(@RequestParam String nombreCliente,
                                 @RequestParam String email,
                                 @RequestParam(required = false) String telefono,
                                 @RequestParam String fechaEntrada,
                                 @RequestParam String fechaSalida,
                                 @RequestParam Integer numeroHuespedes,
                                 @RequestParam(required = false) String comentarios,
                                 Model model) {
        Reserva res = new Reserva(nombreCliente, email, telefono,
                                  LocalDate.parse(fechaEntrada), LocalDate.parse(fechaSalida),
                                  numeroHuespedes, comentarios);
        reservaRepo.save(res);
        emailService.notificarReserva(res);
        model.addAttribute("reservaEnviada", true);
        return "reservas";
    }

    @GetMapping("/contacto")
    public String contacto(Model model) {
        model.addAttribute("mensajeEnviado", false);
        return "contacto";
    }

    @PostMapping("/contacto")
    public String enviarContacto(@RequestParam String nombre,
                                  @RequestParam String email,
                                  @RequestParam(required = false) String telefono,
                                  @RequestParam String mensaje,
                                  Model model) {
        MensajeContacto msg = new MensajeContacto(nombre, email, telefono, mensaje);
        mensajeRepo.save(msg);
        emailService.notificarContacto(nombre, email, mensaje);
        model.addAttribute("mensajeEnviado", true);
        return "contacto";
    }

    @GetMapping("/operaciones")
    public String operaciones() {
        return "operaciones";
    }

    @GetMapping("/aviso-legal")
    public String avisoLegal(Model model) {
        model.addAttribute("texto", textoRepo.findBySlug("aviso-legal").orElse(null));
        return "aviso-legal";
    }

    @GetMapping("/politica-cookies")
    public String politicaCookies(Model model) {
        model.addAttribute("texto", textoRepo.findBySlug("politica-cookies").orElse(null));
        return "politica-cookies";
    }

    @GetMapping("/en")
    public String enIndex() {
        return "en/home";
    }

    @GetMapping("/en/casa")
    public String enCasa(Model model) {
        model.addAttribute("imagenes", imagenRepo.findAllByOrderByOrdenAsc());
        return "en/casa";
    }

    @GetMapping("/en/reservas")
    public String enReservas(Model model, HttpServletRequest request) {
        request.getSession();
        model.addAttribute("reservaEnviada", false);
        model.addAttribute("calendarios", generarCalendarios(occupiedDates(), MESES_EN));
        return "en/reservas";
    }

    @PostMapping("/en/reservas")
    public String enEnviarReserva(@RequestParam String nombreCliente,
                                   @RequestParam String email,
                                   @RequestParam(required = false) String telefono,
                                   @RequestParam String fechaEntrada,
                                   @RequestParam String fechaSalida,
                                   @RequestParam Integer numeroHuespedes,
                                   @RequestParam(required = false) String comentarios,
                                   Model model) {
        Reserva res = new Reserva(nombreCliente, email, telefono,
                                  LocalDate.parse(fechaEntrada), LocalDate.parse(fechaSalida),
                                  numeroHuespedes, comentarios);
        reservaRepo.save(res);
        emailService.notificarReserva(res);
        model.addAttribute("reservaEnviada", true);
        return "en/reservas";
    }

    @GetMapping("/en/contacto")
    public String enContacto(Model model) {
        model.addAttribute("mensajeEnviado", false);
        return "en/contacto";
    }

    @PostMapping("/en/contacto")
    public String enEnviarContacto(@RequestParam String nombre,
                                    @RequestParam String email,
                                    @RequestParam(required = false) String telefono,
                                    @RequestParam String mensaje,
                                    Model model) {
        MensajeContacto msg = new MensajeContacto(nombre, email, telefono, mensaje);
        mensajeRepo.save(msg);
        emailService.notificarContacto(nombre, email, mensaje);
        model.addAttribute("mensajeEnviado", true);
        return "en/contacto";
    }

    @GetMapping("/en/operaciones")
    public String enOperaciones() {
        return "en/operaciones";
    }

    @GetMapping("/en/aviso-legal")
    public String enAvisoLegal(Model model) {
        model.addAttribute("texto", textoRepo.findBySlug("aviso-legal").orElse(null));
        return "en/aviso-legal";
    }

    @GetMapping("/tracking")
    public String tracking(Model model) {
        model.addAttribute("envio", null);
        model.addAttribute("buscado", false);
        return "tracking";
    }

    @PostMapping("/tracking")
    public String buscarTracking(@RequestParam String codigo, Model model) {
        var optEnvio = trackingRepo.findByCodigoUnico(codigo.trim().toUpperCase());
        model.addAttribute("envio", optEnvio.orElse(null));
        model.addAttribute("buscado", true);
        optEnvio.ifPresent(e -> model.addAttribute("eventos", eventoTrackingService.listarPorEnvio(e.getId())));
        return "tracking";
    }

    @GetMapping("/en/politica-cookies")
    public String enPoliticaCookies(Model model) {
        model.addAttribute("texto", textoRepo.findBySlug("politica-cookies").orElse(null));
        return "en/politica-cookies";
    }

    @GetMapping("/en/tracking")
    public String enTracking(Model model) {
        model.addAttribute("envio", null);
        model.addAttribute("buscado", false);
        return "en/tracking";
    }

    @PostMapping("/en/tracking")
    public String enBuscarTracking(@RequestParam String codigo, Model model) {
        var optEnvio = trackingRepo.findByCodigoUnico(codigo.trim().toUpperCase());
        model.addAttribute("envio", optEnvio.orElse(null));
        model.addAttribute("buscado", true);
        optEnvio.ifPresent(e -> model.addAttribute("eventos", eventoTrackingService.listarPorEnvio(e.getId())));
        return "en/tracking";
    }

    // -----------------------------------------------------------
    //  CALENDARIO DE DISPONIBILIDAD
    // -----------------------------------------------------------

    private static final String[] MESES_ES =
        {"Enero","Febrero","Marzo","Abril","Mayo","Junio",
         "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"};
    private static final String[] MESES_EN =
        {"January","February","March","April","May","June",
         "July","August","September","October","November","December"};

    public static class MesCalendario {
        private final String nombre;
        private final int year;
        private final List<List<DiaCalendario>> semanas;
        public MesCalendario(String nombre, int year, List<List<DiaCalendario>> semanas) {
            this.nombre = nombre; this.year = year; this.semanas = semanas;
        }
        public String getNombre() { return nombre; }
        public int getYear() { return year; }
        public List<List<DiaCalendario>> getSemanas() { return semanas; }
    }

    public static class DiaCalendario {
        private final int numero;
        private final boolean ocupado;
        private final boolean pasado;
        private final boolean relleno;
        public DiaCalendario(int numero, boolean ocupado, boolean pasado, boolean relleno) {
            this.numero = numero; this.ocupado = ocupado; this.pasado = pasado; this.relleno = relleno;
        }
        public int getNumero() { return numero; }
        public boolean isOcupado() { return ocupado; }
        public boolean isPasado() { return pasado; }
        public boolean isRelleno() { return relleno; }
    }

    private Set<LocalDate> occupiedDates() {
        LocalDate today = LocalDate.now();
        LocalDate fin = today.plusMonths(3).withDayOfMonth(1).plusMonths(1).minusDays(1);
        List<Reserva> ocupadas = reservaRepo.findOcupadasEnRango(today, fin);
        Set<LocalDate> set = new HashSet<>();
        for (Reserva r : ocupadas) {
            LocalDate d = r.getFechaEntrada();
            while (d.isBefore(r.getFechaSalida())) {
                if (!d.isBefore(today)) set.add(d);
                d = d.plusDays(1);
            }
        }
        return set;
    }

    private List<MesCalendario> generarCalendarios(Set<LocalDate> occupied, String[] meses) {
        List<MesCalendario> calendarios = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate inicio = today.withDayOfMonth(1);
        for (int i = 0; i < 3; i++) {
            LocalDate mes = inicio.plusMonths(i);
            int year = mes.getYear();
            int month = mes.getMonthValue();
            int diasEnMes = mes.lengthOfMonth();
            List<List<DiaCalendario>> semanas = new ArrayList<>();
            List<DiaCalendario> semana = new ArrayList<>();
            LocalDate first = LocalDate.of(year, month, 1);
            int padding = first.getDayOfWeek().getValue() - 1;
            for (int p = 0; p < padding; p++)
                semana.add(new DiaCalendario(0, false, false, true));
            for (int d = 1; d <= diasEnMes; d++) {
                LocalDate fecha = LocalDate.of(year, month, d);
                boolean ocupado = occupied.contains(fecha);
                boolean pasado = fecha.isBefore(today);
                semana.add(new DiaCalendario(d, ocupado, pasado, false));
                if (semana.size() == 7) {
                    semanas.add(semana);
                    semana = new ArrayList<>();
                }
            }
            if (!semana.isEmpty()) {
                while (semana.size() < 7)
                    semana.add(new DiaCalendario(0, false, false, true));
                semanas.add(semana);
            }
            calendarios.add(new MesCalendario(meses[month - 1], year, semanas));
        }
        return calendarios;
    }
}



