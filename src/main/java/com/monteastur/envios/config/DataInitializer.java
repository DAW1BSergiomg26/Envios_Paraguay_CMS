package com.monteastur.envios.config;

import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.EventoTracking;
import com.monteastur.envios.model.EvidenciaEnvio;
import com.monteastur.envios.model.Imagen;
import com.monteastur.envios.model.MensajeContacto;
import com.monteastur.envios.model.Reserva;
import com.monteastur.envios.model.TextoLegal;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.EventoTrackingRepository;
import com.monteastur.envios.repository.EvidenciaEnvioRepository;
import com.monteastur.envios.repository.ImagenRepository;
import com.monteastur.envios.repository.MensajeContactoRepository;
import com.monteastur.envios.repository.ReservaRepository;
import com.monteastur.envios.repository.TextoLegalRepository;
import com.monteastur.envios.service.ClienteService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "app.demo-data", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private final TextoLegalRepository repo;
    private final EnvioTrackingRepository trackingRepo;
    private final ClienteRepository clienteRepo;
    private final ClienteService clienteService;
    private final EventoTrackingRepository eventoRepo;
    private final EvidenciaEnvioRepository evidenciaRepo;
    private final ReservaRepository reservaRepo;
    private final MensajeContactoRepository mensajeRepo;
    private final ImagenRepository imagenRepo;

    public DataInitializer(TextoLegalRepository repo, EnvioTrackingRepository trackingRepo,
                           ClienteRepository clienteRepo, ClienteService clienteService,
                           EventoTrackingRepository eventoRepo, EvidenciaEnvioRepository evidenciaRepo,
                           ReservaRepository reservaRepo, MensajeContactoRepository mensajeRepo,
                           ImagenRepository imagenRepo) {
        this.repo = repo;
        this.trackingRepo = trackingRepo;
        this.clienteRepo = clienteRepo;
        this.clienteService = clienteService;
        this.eventoRepo = eventoRepo;
        this.evidenciaRepo = evidenciaRepo;
        this.reservaRepo = reservaRepo;
        this.mensajeRepo = mensajeRepo;
        this.imagenRepo = imagenRepo;
    }

    @Override
    public void run(String... args) {
        Cliente demo = obtenerClienteDemo();
        crearEnvios(demo);
        crearTextosLegales();
        crearMensajes();
        crearReservas();
        crearImagenes();
    }

    private Cliente obtenerClienteDemo() {
        if (clienteRepo.findByEmail("cliente@monteastur.com").isEmpty()) {
            return clienteService.guardar(new Cliente("cliente@monteastur.com", "demo2026",
                "Mar\u00eda Gonz\u00e1lez", "+34 612 345 678"));
        }
        return clienteRepo.findByEmail("cliente@monteastur.com").get();
    }

    private void crearEnvios(Cliente demo) {
        if (trackingRepo.findByCodigoUnico("MT-2026-0001").isEmpty()) {
            EnvioTracking e = saveEnvio("MT-2026-0001", "EN_TRANSITO",
                "Mar\u00eda Gonz\u00e1lez", "Pola de Siero, Asturias", "Asunci\u00f3n, Paraguay",
                "120 kg", "Maquinaria industrial",
                LocalDateTime.of(2026, 5, 10, 9, 0),
                LocalDateTime.of(2026, 5, 15, 14, 30),
                "El buque zarp\u00f3 del puerto de Gij\u00f3n con destino a Montevideo. Escala t\u00e9cnica prevista en las Islas Canarias.",
                demo);
            crearEventos(e, new EventoInfo[]{
                new EventoInfo("RECIBIDO", "Recogida en origen", "El env\u00edo ha sido recogido en nuestras instalaciones de Pola de Siero.", "Pola de Siero, Asturias", "package", "blue", LocalDateTime.of(2026, 5, 10, 9, 0)),
                new EventoInfo("EN_ADUANA_ORIGEN", "Salida de aduana espa\u00f1ola", "Documentaci\u00f3n aprobada por la aduana espa\u00f1ola. El contenedor ha sido precintado.", "Gij\u00f3n, Asturias", "customs", "yellow", LocalDateTime.of(2026, 5, 12, 16, 0)),
                new EventoInfo("EN_TRANSITO", "Buque en ruta", "El buque MSC Olivia ha zarpado del puerto de Gij\u00f3n con destino a Montevideo. Escala t\u00e9cnica en Santa Cruz de Tenerife.", "Oc\u00e9ano Atl\u00e1ntico", "ship", "blue", LocalDateTime.of(2026, 5, 15, 14, 30)),
            });
            crearEvidencia(e, "Gu\u00eda de embarque", "Documento de embarque firmado por el capit\u00e1n del buque.", "DOCUMENTO", "/uploads/evidencias/demo-guia-mt0001.pdf", LocalDateTime.of(2026, 5, 15, 14, 0));
        }

        if (trackingRepo.findByCodigoUnico("MT-2026-0002").isEmpty()) {
            EnvioTracking e = saveEnvio("MT-2026-0002", "EN_ADUANA_DESTINO",
                "Carlos Mendoza", "Gij\u00f3n, Asturias", "Encarnaci\u00f3n, Paraguay",
                "85 kg", "Electrodom\u00e9sticos y menaje",
                LocalDateTime.of(2026, 5, 8, 11, 0),
                LocalDateTime.of(2026, 5, 17, 10, 15),
                "Documentaci\u00f3n en revisi\u00f3n por la Direcci\u00f3n Nacional de Aduanas. Pendiente de pago de tasas.",
                demo);
            crearEventos(e, new EventoInfo[]{
                new EventoInfo("RECIBIDO", "Recogida en origen", "Recogemos sus electrodom\u00e9sticos y menaje en nuestras instalaciones de Gij\u00f3n.", "Gij\u00f3n, Asturias", "package", "blue", LocalDateTime.of(2026, 5, 8, 11, 0)),
                new EventoInfo("EN_ADUANA_ORIGEN", "Aduana espa\u00f1ola", "Superados los controles de exportaci\u00f3n. Todo en regla.", "Gij\u00f3n, Asturias", "customs", "yellow", LocalDateTime.of(2026, 5, 10, 14, 0)),
                new EventoInfo("EN_TRANSITO", "Llegada a Paraguay", "El contenedor ha llegado al puerto de Asunci\u00f3n y est\u00e1 pendiente de descarga.", "Asunci\u00f3n, Paraguay", "ship", "blue", LocalDateTime.of(2026, 5, 15, 8, 0)),
                new EventoInfo("EN_ADUANA_DESTINO", "En aduana paraguaya", "Documentaci\u00f3n en revisi\u00f3n por la DNA. Pendiente de pago de tasas de importaci\u00f3n.", "Asunci\u00f3n, Paraguay", "customs", "yellow", LocalDateTime.of(2026, 5, 17, 10, 15)),
            });
        }

        if (trackingRepo.findByCodigoUnico("MT-2026-0003").isEmpty()) {
            EnvioTracking e = saveEnvio("MT-2026-0003", "EN_REPARTO",
                "Ana Luc\u00eda Romero", "Oviedo, Asturias", "Ciudad del Este, Paraguay",
                "45 kg", "Ropa y accesorios",
                LocalDateTime.of(2026, 5, 5, 10, 0),
                LocalDateTime.of(2026, 5, 18, 9, 0),
                "Env\u00edo en \u00faltima milla. Reparto previsto en las pr\u00f3ximas 24-48 horas.",
                demo);
            crearEventos(e, new EventoInfo[]{
                new EventoInfo("RECIBIDO", "Recogida en Oviedo", "Paquetes recogidos en tienda de Oviedo. 5 cajas de ropa y accesorios.", "Oviedo, Asturias", "package", "blue", LocalDateTime.of(2026, 5, 5, 10, 0)),
                new EventoInfo("EN_ADUANA_ORIGEN", "Exportaci\u00f3n autorizada", "Aduana espa\u00f1ola autoriza exportaci\u00f3n sin incidencias.", "Gij\u00f3n, Asturias", "customs", "yellow", LocalDateTime.of(2026, 5, 7, 12, 0)),
                new EventoInfo("EN_TRANSITO", "Vuelo Madrid-Asunci\u00f3n", "Mercanc\u00eda transportada v\u00eda a\u00e9rea. Vuelo IB 6725 Madrid-Asunci\u00f3n.", "Madrid, Espa\u00f1a", "airplane", "blue", LocalDateTime.of(2026, 5, 9, 6, 0)),
                new EventoInfo("EN_ADUANA_DESTINO", "Despacho aduanero completado", "Aduana paraguaya libera la mercanc\u00eda. Tasas de importaci\u00f3n pagadas.", "Asunci\u00f3n, Paraguay", "customs", "green", LocalDateTime.of(2026, 5, 16, 11, 0)),
                new EventoInfo("EN_REPARTO", "En reparto local", "\u00daltima milla: reparto en Ciudad del Este. Asignado a flota local.", "Ciudad del Este, Paraguay", "truck", "blue", LocalDateTime.of(2026, 5, 18, 9, 0)),
            });
        }

        if (trackingRepo.findByCodigoUnico("MT-2026-0004").isEmpty()) {
            EnvioTracking e = saveEnvio("MT-2026-0004", "ENTREGADO",
                "Pedro Ram\u00edrez", "Avil\u00e9s, Asturias", "Asunci\u00f3n, Paraguay",
                "200 kg", "Recambios industriales",
                LocalDateTime.of(2026, 4, 20, 8, 0),
                LocalDateTime.of(2026, 5, 12, 16, 45),
                "Entrega completada. Firma recibida. Todo correcto.",
                demo);
            crearEventos(e, new EventoInfo[]{
                new EventoInfo("RECIBIDO", "Recogida en Avil\u00e9s", "Recambios industriales recogidos en almac\u00e9n de Avil\u00e9s. 2 palets.", "Avil\u00e9s, Asturias", "package", "blue", LocalDateTime.of(2026, 4, 20, 8, 0)),
                new EventoInfo("EN_ADUANA_ORIGEN", "Exportaci\u00f3n ok", "Todo en orden para exportaci\u00f3n. Documentaci\u00f3n verificada.", "Gij\u00f3n, Asturias", "customs", "green", LocalDateTime.of(2026, 4, 22, 15, 0)),
                new EventoInfo("EN_TRANSITO", "Vuelo Madrid-Asunci\u00f3n", "Transportado v\u00eda a\u00e9rea. Vuelo directo Madrid-Barajas a Asunci\u00f3n.", "Madrid, Espa\u00f1a", "airplane", "blue", LocalDateTime.of(2026, 4, 25, 23, 0)),
                new EventoInfo("EN_ADUANA_DESTINO", "Aduana destino ok", "Control aduanero superado sin incidencias.", "Asunci\u00f3n, Paraguay", "customs", "green", LocalDateTime.of(2026, 5, 2, 10, 0)),
                new EventoInfo("EN_REPARTO", "Reparto asignado", "Asignado a flota de reparto local. Reparto programado.", "Asunci\u00f3n, Paraguay", "truck", "blue", LocalDateTime.of(2026, 5, 10, 8, 0)),
                new EventoInfo("ENTREGADO", "\u00a1Entregado!", "Entregado a Pedro Ram\u00edrez. Firma recibida. Todo correcto.", "Asunci\u00f3n, Paraguay", "check", "green", LocalDateTime.of(2026, 5, 12, 16, 45)),
            });
            crearEvidencia(e, "Firma de entrega", "PDF con firma del destinatario.", "DOCUMENTO", "/uploads/evidencias/demo-firma-mt0004.pdf", LocalDateTime.of(2026, 5, 12, 17, 0));
            crearEvidencia(e, "Foto del env\u00edo entregado", "Fotograf\u00eda del estado de la mercanc\u00eda en el momento de la entrega.", "FOTO", "/uploads/evidencias/demo-foto-mt0004.jpg", LocalDateTime.of(2026, 5, 12, 17, 0));
        }
    }

    private EnvioTracking saveEnvio(String codigo, String estado, String destinatario,
                                     String origen, String destino, String peso, String contenido,
                                     LocalDateTime fechaCreacion, LocalDateTime ultimaActualizacion,
                                     String observaciones, Cliente cliente) {
        EnvioTracking e = new EnvioTracking(codigo, estado, destinatario, origen, destino, peso, contenido);
        e.setFechaCreacion(fechaCreacion);
        e.setUltimaActualizacion(ultimaActualizacion);
        e.setObservaciones(observaciones);
        e.setCliente(cliente);
        return trackingRepo.save(e);
    }

    private void crearEventos(EnvioTracking envio, EventoInfo... eventos) {
        for (EventoInfo ev : eventos) {
            EventoTracking evento = new EventoTracking();
            evento.setEnvioTracking(envio);
            evento.setEstado(ev.estado);
            evento.setTitulo(ev.titulo);
            evento.setDescripcion(ev.descripcion);
            evento.setUbicacion(ev.ubicacion);
            evento.setIcono(ev.icono);
            evento.setColor(ev.color);
            evento.setFechaEvento(ev.fechaEvento);
            evento.setCreadoPor("sistema");
            evento.setVisibleCliente(true);
            eventoRepo.save(evento);
        }
    }

    private void crearEvidencia(EnvioTracking envio, String titulo, String descripcion,
                                String tipo, String urlArchivo, LocalDateTime fechaSubida) {
        EvidenciaEnvio ev = new EvidenciaEnvio();
        ev.setEnvioTracking(envio);
        ev.setTitulo(titulo);
        ev.setDescripcion(descripcion);
        ev.setTipo(tipo);
        ev.setUrlArchivo(urlArchivo);
        ev.setFechaSubida(fechaSubida);
        ev.setVisibleCliente(true);
        evidenciaRepo.save(ev);
    }

    private void crearTextosLegales() {
        if (repo.findBySlug("aviso-legal").isEmpty()) {
            repo.save(new TextoLegal("aviso-legal", "Aviso Legal",
                "<h3>1. Datos identificativos</h3>\n" +
                "<p>El presente sitio web es propiedad del Grupo B2, con domicilio en Asturias, Espa\u00f1a.</p>\n" +
                "<p>Correo electr\u00f3nico de contacto: info@casarrural.com</p>\n" +
                "<h3>2. Propiedad intelectual</h3>\n" +
                "<p>Todos los contenidos del sitio web (textos, im\u00e1genes, logotipos, dise\u00f1o) est\u00e1n protegidos por derechos de propiedad intelectual. Queda prohibida su reproducci\u00f3n total o parcial sin autorizaci\u00f3n expresa.</p>\n" +
                "<h3>3. Uso del sitio web</h3>\n" +
                "<p>El usuario se compromete a hacer un uso adecuado del sitio web y a no emplearlo para realizar actividades il\u00edcitas o contrarias a la buena fe.</p>\n" +
                "<h3>4. Protecci\u00f3n de datos</h3>\n" +
                "<p>Los datos personales recogidos a trav\u00e9s de los formularios ser\u00e1n tratados conforme al Reglamento General de Protecci\u00f3n de Datos (RGPD). El usuario podr\u00e1 ejercer sus derechos de acceso, rectificaci\u00f3n, cancelaci\u00f3n y oposici\u00f3n contactando por correo electr\u00f3nico.</p>\n" +
                "<h3>5. Legislaci\u00f3n aplicable</h3>\n" +
                "<p>Este aviso legal se rige por la legislaci\u00f3n espa\u00f1ola vigente. Cualquier controversia se someter\u00e1 a los juzgados y tribunales de Asturias.</p>"));
        }
        if (repo.findBySlug("politica-cookies").isEmpty()) {
            repo.save(new TextoLegal("politica-cookies", "Pol\u00edtica de Cookies",
                "<h3>\u00bfQu\u00e9 son las cookies?</h3>\n" +
                "<p>Las cookies son peque\u00f1os archivos de texto que se almacenan en tu navegador cuando visitas un sitio web. Permiten recordar tus preferencias y mejorar tu experiencia de navegaci\u00f3n.</p>\n" +
                "<h3>Tipos de cookies utilizadas</h3>\n" +
                "<p><strong>Cookies t\u00e9cnicas:</strong> necesarias para el funcionamiento del sitio web. No requieren consentimiento del usuario.</p>\n" +
                "<p><strong>Cookies de personalizaci\u00f3n:</strong> permiten recordar tus preferencias (como el idioma).</p>\n" +
                "<p><strong>Cookies de an\u00e1lisis:</strong> nos ayudan a medir y mejorar el rendimiento del sitio.</p>\n" +
                "<h3>C\u00f3mo gestionar las cookies</h3>\n" +
                "<p>Puedes configurar tu navegador para bloquear o eliminar las cookies. A continuaci\u00f3n, los enlaces a las gu\u00edas de los navegadores m\u00e1s utilizados:</p>\n" +
                "<ul>\n" +
                "    <li><a href=\"https://support.google.com/chrome/answer/95647\" target=\"_blank\">Google Chrome</a></li>\n" +
                "    <li><a href=\"https://support.mozilla.org/es/kb/habilitar-y-deshabilitar-cookies-sitios-web-rastrear-preferencias\" target=\"_blank\">Mozilla Firefox</a></li>\n" +
                "    <li><a href=\"https://support.microsoft.com/es-es/windows/eliminar-y-administrar-cookies-168dab11-0753-043d-7c16-ede5947fc64d\" target=\"_blank\">Microsoft Edge</a></li>\n" +
                "</ul>\n" +
                "<h3>Consentimiento</h3>\n" +
                "<p>Al hacer clic en \"Aceptar cookies\", consientes el uso de las cookies descritas en esta pol\u00edtica.</p>"));
        }
    }

    private void crearMensajes() {
        if (mensajeRepo.count() == 0) {
            MensajeContacto m1 = new MensajeContacto("Juan P\u00e9rez", "juan@example.com", "+34 611 111 111",
                "Buenos d\u00edas, me gustar\u00eda solicitar un presupuesto para enviar un contenedor de 20 pies desde Gij\u00f3n hasta Asunci\u00f3n. \u00bfPodr\u00edan enviarme informaci\u00f3n sobre tarifas y tiempos de tr\u00e1nsito?");
            m1.setFechaEnvio(LocalDateTime.of(2026, 5, 20, 10, 30));
            mensajeRepo.save(m1);

            MensajeContacto m2 = new MensajeContacto("Laura Mart\u00ednez", "laura@example.com", "+34 622 222 222",
                "Hola, estoy interesada en enviar un paquete de documentaci\u00f3n importante a Paraguay. \u00bfOfrec\u00e9is servicio expresa? Necesito que llegue en menos de 5 d\u00edas. \u00a1Gracias!");
            m2.setFechaEnvio(LocalDateTime.of(2026, 5, 18, 15, 45));
            mensajeRepo.save(m2);

            MensajeContacto m3 = new MensajeContacto("Roberto Garc\u00eda", "roberto@example.com", "+34 633 333 333",
                "Buenas tardes, soy representante de una empresa de alimentaci\u00f3n y queremos establecer una ruta regular de env\u00edos a Paraguay. Necesitar\u00edamos un volumen mensual aproximado de 500 kg. \u00bfTrabaj\u00e1is con cuentas corporativas?");
            m3.setFechaEnvio(LocalDateTime.of(2026, 5, 15, 9, 0));
            mensajeRepo.save(m3);

            MensajeContacto m4 = new MensajeContacto("Ana L\u00f3pez", "ana@example.com", "+34 644 444 444",
                "Hola, hace una semana contrat\u00e9 un env\u00edo con c\u00f3digo MT-2026-0003 pero a\u00fan no recibo noticias. \u00bfPodr\u00edan indicarme el estado actual? Gracias.");
            m4.setFechaEnvio(LocalDateTime.of(2026, 5, 12, 12, 0));
            mensajeRepo.save(m4);
        }
    }

    private void crearReservas() {
        if (reservaRepo.count() == 0) {
            Reserva r1 = new Reserva("Juan P\u00e9rez", "juan@example.com", "+34 611 111 111",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), 4,
                "Solicito env\u00edo de 4 palets de mercanc\u00eda industrial desde Gij\u00f3n a Asunci\u00f3n. Contenedor compartido.");
            r1.setCreatedAt(LocalDateTime.of(2026, 5, 20, 10, 30));
            reservaRepo.save(r1);

            Reserva r2 = new Reserva("Laura Mart\u00ednez", "laura@example.com", "+34 622 222 222",
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 15), 2,
                "Documentaci\u00f3n corporativa urgente. Servicio expresa.");
            r2.setCreatedAt(LocalDateTime.of(2026, 5, 18, 15, 45));
            r2.setEstado("confirmada");
            reservaRepo.save(r2);

            Reserva r3 = new Reserva("Carlos Ruiz", "carlos@example.com", "+34 655 555 555",
                LocalDate.of(2026, 5, 5), LocalDate.of(2026, 5, 10), 3,
                "Cancelado por cambio de proveedor log\u00edstico.");
            r3.setCreatedAt(LocalDateTime.of(2026, 4, 20, 8, 0));
            r3.setEstado("cancelada");
            reservaRepo.save(r3);

            Reserva r4 = new Reserva("Ana Torres", "ana.torres@example.com", "+34 666 666 666",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15), 6,
                "Mudanza completa de Asturias a Paraguay. Muebles, electrodom\u00e9sticos y enseres personales. Solicito presupuesto detallado.");
            r4.setCreatedAt(LocalDateTime.of(2026, 5, 25, 9, 0));
            reservaRepo.save(r4);
        }
    }

    private void crearImagenes() {
        if (imagenRepo.count() == 0) {
            imagenRepo.save(new Imagen("Oficinas Centrales", "Nuestras instalaciones en Pola de Siero",
                "/img/demo-gallery/oficinas-centrales.svg", "instalaciones", 1));
            imagenRepo.save(new Imagen("Flota de reparto", "Veh\u00edculos de reparto local",
                "/img/demo-gallery/flota-reparto.svg", "flota", 2));
            imagenRepo.save(new Imagen("Almac\u00e9n log\u00edstico", "Centro de distribuci\u00f3n en Gij\u00f3n",
                "/img/demo-gallery/almacen-logistico.svg", "instalaciones", 3));
            imagenRepo.save(new Imagen("Puerto de Gij\u00f3n", "Operativa portuaria para env\u00edos internacionales",
                "/img/demo-gallery/puerto-gijon.svg", "operativa", 4));
        }
    }

    private record EventoInfo(String estado, String titulo, String descripcion,
                              String ubicacion, String icono, String color, LocalDateTime fechaEvento) {}
}
