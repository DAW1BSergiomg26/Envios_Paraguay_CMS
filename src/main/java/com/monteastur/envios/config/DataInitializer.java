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
        sincronizarDemoPrincipal();
        crearTextosLegales();
        crearMensajes();
        crearReservas();
        crearImagenes();
    }

    private Cliente obtenerClienteDemo() {
        if (clienteRepo.findByEmail("cliente@monteastur.com").isEmpty()) {
            return clienteService.guardar(new Cliente("cliente@monteastur.com", "demo2026",
                "María González", "+34 612 345 678"));
        }
        return clienteRepo.findByEmail("cliente@monteastur.com").get();
    }

    private void crearEnvios(Cliente demo) {
        if (trackingRepo.findByCodigoUnico("MT-2026-0001").isEmpty()) {
            EnvioTracking e = saveEnvio("MT-2026-0001", "EN_TRANSITO",
                "María González", "Pola de Siero, Asturias", "Asunción, Paraguay",
                "120 kg", "Maquinaria industrial",
                LocalDateTime.of(2026, 5, 10, 9, 0),
                LocalDateTime.of(2026, 5, 15, 14, 30),
                "El buque zarpó del puerto de Gijón con destino a Montevideo. Escala técnica prevista en las Islas Canarias.",
                demo);
            crearEventos(e, new EventoInfo[]{
                new EventoInfo("RECIBIDO", "Recogida en origen", "El envío ha sido recogido en nuestras instalaciones de Pola de Siero.", "Pola de Siero, Asturias", "package", "blue", LocalDateTime.of(2026, 5, 10, 9, 0)),
                new EventoInfo("EN_ADUANA_ORIGEN", "Salida de aduana española", "Documentación aprobada por la aduana española. El contenedor ha sido precintado.", "Gijón, Asturias", "customs", "yellow", LocalDateTime.of(2026, 5, 12, 16, 0)),
                new EventoInfo("EN_TRANSITO", "Buque en ruta", "El buque MSC Olivia ha zarpado del puerto de Gijón con destino a Montevideo. Escala técnica en Santa Cruz de Tenerife.", "Océano Atlántico", "ship", "blue", LocalDateTime.of(2026, 5, 15, 14, 30)),
            });
            crearEvidencia(e, "Guía de embarque", "Documento de embarque firmado por el capitán del buque.", "DOCUMENTO", "/uploads/evidencias/demo-guia-mt0001.pdf", LocalDateTime.of(2026, 5, 15, 14, 0));
        }

        if (trackingRepo.findByCodigoUnico("MT-2026-0002").isEmpty()) {
            EnvioTracking e = saveEnvio("MT-2026-0002", "EN_ADUANA_DESTINO",
                "Carlos Mendoza", "Gijón, Asturias", "Encarnación, Paraguay",
                "85 kg", "Electrodomésticos y menaje",
                LocalDateTime.of(2026, 5, 8, 11, 0),
                LocalDateTime.of(2026, 5, 17, 10, 15),
                "Documentación en revisión por la Dirección Nacional de Aduanas. Pendiente de pago de tasas.",
                demo);
            crearEventos(e, new EventoInfo[]{
                new EventoInfo("RECIBIDO", "Recogida en origen", "Recogemos sus electrodomésticos y menaje en nuestras instalaciones de Gijón.", "Gijón, Asturias", "package", "blue", LocalDateTime.of(2026, 5, 8, 11, 0)),
                new EventoInfo("EN_ADUANA_ORIGEN", "Aduana española", "Superados los controles de exportación. Todo en regla.", "Gijón, Asturias", "customs", "yellow", LocalDateTime.of(2026, 5, 10, 14, 0)),
                new EventoInfo("EN_TRANSITO", "Llegada a Paraguay", "El contenedor ha llegado al puerto de Asunción y está pendiente de descarga.", "Asunción, Paraguay", "ship", "blue", LocalDateTime.of(2026, 5, 15, 8, 0)),
                new EventoInfo("EN_ADUANA_DESTINO", "En aduana paraguaya", "Documentación en revisión por la DNA. Pendiente de pago de tasas de importación.", "Asunción, Paraguay", "customs", "yellow", LocalDateTime.of(2026, 5, 17, 10, 15)),
            });
        }

        if (trackingRepo.findByCodigoUnico("MT-2026-0003").isEmpty()) {
            EnvioTracking e = saveEnvio("MT-2026-0003", "EN_REPARTO",
                "Ana Lucía Romero", "Oviedo, Asturias", "Ciudad del Este, Paraguay",
                "45 kg", "Ropa y accesorios",
                LocalDateTime.of(2026, 5, 5, 10, 0),
                LocalDateTime.of(2026, 5, 18, 9, 0),
                "Envío en última milla. Reparto previsto en las próximas 24-48 horas.",
                demo);
            crearEventos(e, new EventoInfo[]{
                new EventoInfo("RECIBIDO", "Recogida en Oviedo", "Paquetes recogidos en tienda de Oviedo. 5 cajas de ropa y accesorios.", "Oviedo, Asturias", "package", "blue", LocalDateTime.of(2026, 5, 5, 10, 0)),
                new EventoInfo("EN_ADUANA_ORIGEN", "Exportación autorizada", "Aduana española autoriza exportación sin incidencias.", "Gijón, Asturias", "customs", "yellow", LocalDateTime.of(2026, 5, 7, 12, 0)),
                new EventoInfo("EN_TRANSITO", "Vuelo Madrid-Asunción", "Mercancía transportada vía aérea. Vuelo IB 6725 Madrid-Asunción.", "Madrid, España", "airplane", "blue", LocalDateTime.of(2026, 5, 9, 6, 0)),
                new EventoInfo("EN_ADUANA_DESTINO", "Despacho aduanero completado", "Aduana paraguaya libera la mercancía. Tasas de importación pagadas.", "Asunción, Paraguay", "customs", "green", LocalDateTime.of(2026, 5, 16, 11, 0)),
                new EventoInfo("EN_REPARTO", "En reparto local", "Última milla: reparto en Ciudad del Este. Asignado a flota local.", "Ciudad del Este, Paraguay", "truck", "blue", LocalDateTime.of(2026, 5, 18, 9, 0)),
            });
        }

        if (trackingRepo.findByCodigoUnico("MT-2026-0004").isEmpty()) {
            EnvioTracking e = saveEnvio("MT-2026-0004", "ENTREGADO",
                "Pedro Ramírez", "Avilés, Asturias", "Asunción, Paraguay",
                "200 kg", "Recambios industriales",
                LocalDateTime.of(2026, 4, 20, 8, 0),
                LocalDateTime.of(2026, 5, 12, 16, 45),
                "Entrega completada. Firma recibida. Todo correcto.",
                demo);
            crearEventos(e, new EventoInfo[]{
                new EventoInfo("RECIBIDO", "Recogida en Avilés", "Recambios industriales recogidos en almacén de Avilés. 2 palets.", "Avilés, Asturias", "package", "blue", LocalDateTime.of(2026, 4, 20, 8, 0)),
                new EventoInfo("EN_ADUANA_ORIGEN", "Exportación ok", "Todo en orden para exportación. Documentación verificada.", "Gijón, Asturias", "customs", "green", LocalDateTime.of(2026, 4, 22, 15, 0)),
                new EventoInfo("EN_TRANSITO", "Vuelo Madrid-Asunción", "Transportado vía aérea. Vuelo directo Madrid-Barajas a Asunción.", "Madrid, España", "airplane", "blue", LocalDateTime.of(2026, 4, 25, 23, 0)),
                new EventoInfo("EN_ADUANA_DESTINO", "Aduana destino ok", "Control aduanero superado sin incidencias.", "Asunción, Paraguay", "customs", "green", LocalDateTime.of(2026, 5, 2, 10, 0)),
                new EventoInfo("EN_REPARTO", "Reparto asignado", "Asignado a flota de reparto local. Reparto programado.", "Asunción, Paraguay", "truck", "blue", LocalDateTime.of(2026, 5, 10, 8, 0)),
                new EventoInfo("ENTREGADO", "¡Entregado!", "Entregado a Pedro Ramírez. Firma recibida. Todo correcto.", "Asunción, Paraguay", "check", "green", LocalDateTime.of(2026, 5, 12, 16, 45)),
            });
            crearEvidencia(e, "Firma de entrega", "PDF con firma del destinatario.", "DOCUMENTO", "/uploads/evidencias/demo-firma-mt0004.pdf", LocalDateTime.of(2026, 5, 12, 17, 0));
            crearEvidencia(e, "Foto del envío entregado", "Fotografía del estado de la mercancía en el momento de la entrega.", "FOTO", "/uploads/evidencias/demo-foto-mt0004.jpg", LocalDateTime.of(2026, 5, 12, 17, 0));
        }
    }

    private void sincronizarDemoPrincipal() {
        trackingRepo.findByCodigoUnico("MT-2026-0001").ifPresent(envio -> {
            if (eventoRepo.findByEnvioTrackingIdOrderByFechaEventoDesc(envio.getId()).size() >= 5) {
                return;
            }

            eventoRepo.deleteAll(eventoRepo.findByEnvioTrackingIdOrderByFechaEventoDesc(envio.getId()));
            envio.setEstado("EN_REPARTO");
            envio.setDestinatario("María González");
            envio.setOrigen("Pola de Siero, Asturias");
            envio.setDestino("Asunción, Paraguay");
            envio.setPeso("120 kg");
            envio.setContenido("Maquinaria industrial");
            envio.setFechaCreacion(LocalDateTime.of(2026, 5, 10, 9, 0));
            envio.setUltimaActualizacion(LocalDateTime.of(2026, 5, 18, 9, 0));
            envio.setUbicacionActual("Ciudad del Este, Paraguay");
            envio.setObservaciones("Envío en última milla. Reparto previsto en las próximas 24-48 horas.");
            trackingRepo.save(envio);

            crearEventos(envio, new EventoInfo[]{
                new EventoInfo("RECIBIDO", "Recogida en Pola de Siero", "Mercancía industrial recogida y registrada en el almacén de MONTEASTUR.", "Pola de Siero, Asturias", "package", "blue", LocalDateTime.of(2026, 5, 10, 9, 0)),
                new EventoInfo("EN_ADUANA_ORIGEN", "Exportación autorizada", "Aduana española autoriza la exportación sin incidencias.", "Gijón, Asturias", "customs", "yellow", LocalDateTime.of(2026, 5, 12, 16, 0)),
                new EventoInfo("EN_TRANSITO", "Ruta atlántica iniciada", "La mercancía salió de Asturias y continúa su ruta internacional hacia Paraguay.", "Océano Atlántico", "ship", "blue", LocalDateTime.of(2026, 5, 15, 14, 30)),
                new EventoInfo("EN_ADUANA_DESTINO", "Despacho aduanero completado", "Aduana paraguaya libera la mercancía. Tasas de importación pagadas.", "Asunción, Paraguay", "customs", "green", LocalDateTime.of(2026, 5, 17, 10, 15)),
                new EventoInfo("EN_REPARTO", "En reparto local", "Última milla: reparto local asignado en Paraguay.", "Ciudad del Este, Paraguay", "truck", "blue", LocalDateTime.of(2026, 5, 18, 9, 0)),
            });
        });
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
                "<p>El presente sitio web es propiedad del Grupo B2, con domicilio en Asturias, España.</p>\n" +
                "<p>Correo electrónico de contacto: info@casarrural.com</p>\n" +
                "<h3>2. Propiedad intelectual</h3>\n" +
                "<p>Todos los contenidos del sitio web (textos, imágenes, logotipos, diseño) están protegidos por derechos de propiedad intelectual. Queda prohibida su reproducción total o parcial sin autorización expresa.</p>\n" +
                "<h3>3. Uso del sitio web</h3>\n" +
                "<p>El usuario se compromete a hacer un uso adecuado del sitio web y a no emplearlo para realizar actividades ilícitas o contrarias a la buena fe.</p>\n" +
                "<h3>4. Protección de datos</h3>\n" +
                "<p>Los datos personales recogidos a través de los formularios serán tratados conforme al Reglamento General de Protección de Datos (RGPD). El usuario podrá ejercer sus derechos de acceso, rectificación, cancelación y oposición contactando por correo electrónico.</p>\n" +
                "<h3>5. Legislación aplicable</h3>\n" +
                "<p>Este aviso legal se rige por la legislación española vigente. Cualquier controversia se someterá a los juzgados y tribunales de Asturias.</p>"));
        }
        if (repo.findBySlug("politica-cookies").isEmpty()) {
            repo.save(new TextoLegal("politica-cookies", "Política de Cookies",
                "<h3>¿Qué son las cookies?</h3>\n" +
                "<p>Las cookies son pequeños archivos de texto que se almacenan en tu navegador cuando visitas un sitio web. Permiten recordar tus preferencias y mejorar tu experiencia de navegación.</p>\n" +
                "<h3>Tipos de cookies utilizadas</h3>\n" +
                "<p><strong>Cookies técnicas:</strong> necesarias para el funcionamiento del sitio web. No requieren consentimiento del usuario.</p>\n" +
                "<p><strong>Cookies de personalización:</strong> permiten recordar tus preferencias (como el idioma).</p>\n" +
                "<p><strong>Cookies de análisis:</strong> nos ayudan a medir y mejorar el rendimiento del sitio.</p>\n" +
                "<h3>Cómo gestionar las cookies</h3>\n" +
                "<p>Puedes configurar tu navegador para bloquear o eliminar las cookies. A continuación, los enlaces a las guías de los navegadores más utilizados:</p>\n" +
                "<ul>\n" +
                "    <li><a href=\"https://support.google.com/chrome/answer/95647\" target=\"_blank\">Google Chrome</a></li>\n" +
                "    <li><a href=\"https://support.mozilla.org/es/kb/habilitar-y-deshabilitar-cookies-sitios-web-rastrear-preferencias\" target=\"_blank\">Mozilla Firefox</a></li>\n" +
                "    <li><a href=\"https://support.microsoft.com/es-es/windows/eliminar-y-administrar-cookies-168dab11-0753-043d-7c16-ede5947fc64d\" target=\"_blank\">Microsoft Edge</a></li>\n" +
                "</ul>\n" +
                "<h3>Consentimiento</h3>\n" +
                "<p>Al hacer clic en \"Aceptar cookies\", consientes el uso de las cookies descritas en esta política.</p>"));
        }
    }

    private void crearMensajes() {
        if (mensajeRepo.count() == 0) {
            MensajeContacto m1 = new MensajeContacto("Juan Pérez", "juan@example.com", "+34 611 111 111",
                "Buenos días, me gustaría solicitar un presupuesto para enviar un contenedor de 20 pies desde Gijón hasta Asunción. ¿Podrían enviarme información sobre tarifas y tiempos de tránsito?");
            m1.setFechaEnvio(LocalDateTime.of(2026, 5, 20, 10, 30));
            mensajeRepo.save(m1);

            MensajeContacto m2 = new MensajeContacto("Laura Martínez", "laura@example.com", "+34 622 222 222",
                "Hola, estoy interesada en enviar un paquete de documentación importante a Paraguay. ¿Ofrecéis servicio expresa? Necesito que llegue en menos de 5 días. ¡Gracias!");
            m2.setFechaEnvio(LocalDateTime.of(2026, 5, 18, 15, 45));
            mensajeRepo.save(m2);

            MensajeContacto m3 = new MensajeContacto("Roberto García", "roberto@example.com", "+34 633 333 333",
                "Buenas tardes, soy representante de una empresa de alimentación y queremos establecer una ruta regular de envíos a Paraguay. Necesitaríamos un volumen mensual aproximado de 500 kg. ¿Trabajáis con cuentas corporativas?");
            m3.setFechaEnvio(LocalDateTime.of(2026, 5, 15, 9, 0));
            mensajeRepo.save(m3);

            MensajeContacto m4 = new MensajeContacto("Ana López", "ana@example.com", "+34 644 444 444",
                "Hola, hace una semana contraté un envío con código MT-2026-0003 pero aún no recibo noticias. ¿Podrían indicarme el estado actual? Gracias.");
            m4.setFechaEnvio(LocalDateTime.of(2026, 5, 12, 12, 0));
            mensajeRepo.save(m4);
        }
    }

    private void crearReservas() {
        if (reservaRepo.count() == 0) {
            Reserva r1 = new Reserva("Juan Pérez", "juan@example.com", "+34 611 111 111",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), 4,
                "Solicito envío de 4 palets de mercancía industrial desde Gijón a Asunción. Contenedor compartido.");
            r1.setCreatedAt(LocalDateTime.of(2026, 5, 20, 10, 30));
            reservaRepo.save(r1);

            Reserva r2 = new Reserva("Laura Martínez", "laura@example.com", "+34 622 222 222",
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 15), 2,
                "Documentación corporativa urgente. Servicio expresa.");
            r2.setCreatedAt(LocalDateTime.of(2026, 5, 18, 15, 45));
            r2.setEstado("confirmada");
            reservaRepo.save(r2);

            Reserva r3 = new Reserva("Carlos Ruiz", "carlos@example.com", "+34 655 555 555",
                LocalDate.of(2026, 5, 5), LocalDate.of(2026, 5, 10), 3,
                "Cancelado por cambio de proveedor logístico.");
            r3.setCreatedAt(LocalDateTime.of(2026, 4, 20, 8, 0));
            r3.setEstado("cancelada");
            reservaRepo.save(r3);

            Reserva r4 = new Reserva("Ana Torres", "ana.torres@example.com", "+34 666 666 666",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 15), 6,
                "Mudanza completa de Asturias a Paraguay. Muebles, electrodomésticos y enseres personales. Solicito presupuesto detallado.");
            r4.setCreatedAt(LocalDateTime.of(2026, 5, 25, 9, 0));
            reservaRepo.save(r4);
        }
    }

    private void crearImagenes() {
        if (imagenRepo.count() == 0) {
            imagenRepo.save(new Imagen("Oficinas Centrales", "Nuestras instalaciones en Pola de Siero",
                "/img/demo-gallery/oficinas-centrales.svg", "instalaciones", 1));
            imagenRepo.save(new Imagen("Flota de reparto", "Vehículos de reparto local",
                "/img/demo-gallery/flota-reparto.svg", "flota", 2));
            imagenRepo.save(new Imagen("Almacén logístico", "Centro de distribución en Gijón",
                "/img/demo-gallery/almacen-logistico.svg", "instalaciones", 3));
            imagenRepo.save(new Imagen("Puerto de Gijón", "Operativa portuaria para envíos internacionales",
                "/img/demo-gallery/puerto-gijon.svg", "operativa", 4));
        }
    }

    private record EventoInfo(String estado, String titulo, String descripcion,
                              String ubicacion, String icono, String color, LocalDateTime fechaEvento) {}
}
