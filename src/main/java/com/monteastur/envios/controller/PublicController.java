package com.monteastur.envios.controller;

import com.monteastur.envios.model.Imagen;
import com.monteastur.envios.model.MensajeContacto;
import com.monteastur.envios.model.Reserva;
import com.monteastur.envios.model.TextoLegal;
import com.monteastur.envios.repository.ImagenRepository;
import com.monteastur.envios.repository.MensajeContactoRepository;
import com.monteastur.envios.repository.ReservaRepository;
import com.monteastur.envios.repository.TextoLegalRepository;
import com.monteastur.envios.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.time.LocalDate;
import java.util.*;

@Controller
public class PublicController {

    private static final String[] MESES_ES =
        {"Enero","Febrero","Marzo","Abril","Mayo","Junio",
         "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"};
    private static final String[] MESES_EN =
        {"January","February","March","April","May","June",
         "July","August","September","October","November","December"};

    private final MensajeContactoRepository mensajeRepo;
    private final ReservaRepository reservaRepo;
    private final ImagenRepository imagenRepo;
    private final TextoLegalRepository textoRepo;
    private final EmailService emailService;

    public PublicController(MensajeContactoRepository mensajeRepo, ReservaRepository reservaRepo,
                            ImagenRepository imagenRepo, TextoLegalRepository textoRepo,
                            EmailService emailService) {
        this.mensajeRepo = mensajeRepo;
        this.reservaRepo = reservaRepo;
        this.imagenRepo = imagenRepo;
        this.textoRepo = textoRepo;
        this.emailService = emailService;
    }

    @GetMapping({"/", "/en"})
    public String index(HttpServletRequest request) {
        return template("home", request);
    }

    @GetMapping({"/casa", "/en/casa"})
    public String laCasa(Model model, HttpServletRequest request) {
        model.addAttribute("imagenes", imagenRepo.findAllByOrderByOrdenAsc());
        return template("lacasa", request);
    }

    @GetMapping("/lacasa")
    public ResponseEntity<Void> redirigirAliasLaCasa() {
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .location(URI.create("/casa"))
                .build();
    }

    @GetMapping({"/entorno", "/en/entorno"})
    public String entorno(HttpServletRequest request) {
        return template("entorno", request);
    }

    @GetMapping({"/reservas", "/en/reservas"})
    public String reservas(Model model, HttpServletRequest request) {
        request.getSession();
        model.addAttribute("reservaEnviada", false);
        model.addAttribute("calendarios", generarCalendarios(occupiedDates(), monthNames(request)));
        return template("reservas", request);
    }

    @PostMapping({"/reservas", "/en/reservas"})
    public String enviarReserva(@RequestParam String nombreCliente,
                                 @RequestParam String email,
                                 @RequestParam(required = false) String telefono,
                                 @RequestParam String fechaEntrada,
                                 @RequestParam String fechaSalida,
                                 @RequestParam Integer numeroHuespedes,
                                 @RequestParam(required = false) String comentarios,
                                 Model model, HttpServletRequest request) {
        Reserva res = new Reserva(nombreCliente, email, telefono,
                                  LocalDate.parse(fechaEntrada), LocalDate.parse(fechaSalida),
                                  numeroHuespedes, comentarios);
        reservaRepo.save(res);
        emailService.notificarReserva(res);
        model.addAttribute("reservaEnviada", true);
        return template("reservas", request);
    }

    @GetMapping({"/contacto", "/en/contacto"})
    public String contacto(Model model, HttpServletRequest request) {
        model.addAttribute("mensajeEnviado", false);
        return template("contacto", request);
    }

    @PostMapping({"/contacto", "/en/contacto"})
    public String enviarContacto(@RequestParam String nombre,
                                  @RequestParam String email,
                                  @RequestParam(required = false) String telefono,
                                  @RequestParam String mensaje,
                                  Model model, HttpServletRequest request) {
        MensajeContacto msg = new MensajeContacto(nombre, email, telefono, mensaje);
        mensajeRepo.save(msg);
        emailService.notificarContacto(nombre, email, mensaje);
        model.addAttribute("mensajeEnviado", true);
        return template("contacto", request);
    }

    @GetMapping({"/operaciones", "/en/operaciones"})
    public String operaciones(HttpServletRequest request) {
        return template("operaciones", request);
    }

    @GetMapping({"/aviso-legal", "/en/aviso-legal"})
    public String avisoLegal(Model model, HttpServletRequest request) {
        model.addAttribute("texto", textoRepo.findBySlug("aviso-legal").orElse(null));
        return template("aviso-legal", request);
    }

    @GetMapping({"/politica-cookies", "/en/politica-cookies"})
    public String politicaCookies(Model model, HttpServletRequest request) {
        model.addAttribute("texto", textoRepo.findBySlug("politica-cookies").orElse(null));
        return template("politica-cookies", request);
    }

    // -----------------------------------------------------------
    //  CALENDARIO DE DISPONIBILIDAD
    // -----------------------------------------------------------

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

    private String template(String view, HttpServletRequest request) {
        return isEnglish(request) ? "en/" + view : view;
    }

    private boolean isEnglish(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/en/") || uri.equals("/en");
    }

    private String[] monthNames(HttpServletRequest request) {
        return isEnglish(request) ? MESES_EN : MESES_ES;
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
