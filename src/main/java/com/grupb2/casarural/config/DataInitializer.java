package com.grupb2.casarural.config;

import com.grupb2.casarural.model.Cliente;
import com.grupb2.casarural.model.EnvioTracking;
import com.grupb2.casarural.model.TextoLegal;
import com.grupb2.casarural.repository.ClienteRepository;
import com.grupb2.casarural.repository.EnvioTrackingRepository;
import com.grupb2.casarural.repository.TextoLegalRepository;
import com.grupb2.casarural.service.ClienteService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final TextoLegalRepository repo;
    private final EnvioTrackingRepository trackingRepo;
    private final ClienteRepository clienteRepo;
    private final ClienteService clienteService;

    public DataInitializer(TextoLegalRepository repo, EnvioTrackingRepository trackingRepo,
                           ClienteRepository clienteRepo, ClienteService clienteService) {
        this.repo = repo;
        this.trackingRepo = trackingRepo;
        this.clienteRepo = clienteRepo;
        this.clienteService = clienteService;
    }

    @Override
    public void run(String... args) {
        Cliente demo = null;
        if (clienteRepo.findByEmail("cliente@monteastur.com").isEmpty()) {
            demo = clienteService.guardar(new Cliente("cliente@monteastur.com", "demo2026",
                "María González", "+34 612 345 678"));
        } else {
            demo = clienteRepo.findByEmail("cliente@monteastur.com").get();
        }

        if (trackingRepo.findByCodigoUnico("MT-2026-0001").isEmpty()) {
            EnvioTracking e1 = new EnvioTracking("MT-2026-0001", "EN_TRANSITO",
                "María González", "Pola de Siero, Asturias", "Asunción, Paraguay",
                "120 kg", "Maquinaria industrial");
            e1.setFechaCreacion(LocalDateTime.of(2026, 5, 10, 9, 0));
            e1.setUltimaActualizacion(LocalDateTime.of(2026, 5, 15, 14, 30));
            e1.setObservaciones("El buque zarpó del puerto de Gijón con destino a Montevideo. Escala técnica prevista en las Islas Canarias.");
            e1.setCliente(demo);
            trackingRepo.save(e1);
        }
        if (trackingRepo.findByCodigoUnico("MT-2026-0002").isEmpty()) {
            EnvioTracking e2 = new EnvioTracking("MT-2026-0002", "EN_ADUANA_DESTINO",
                "Carlos Mendoza", "Gijón, Asturias", "Encarnación, Paraguay",
                "85 kg", "Electrodomésticos y menaje");
            e2.setFechaCreacion(LocalDateTime.of(2026, 5, 8, 11, 0));
            e2.setUltimaActualizacion(LocalDateTime.of(2026, 5, 17, 10, 15));
            e2.setObservaciones("Documentación en revisión por la Dirección Nacional de Aduanas. Pendiente de pago de tasas.");
            e2.setCliente(demo);
            trackingRepo.save(e2);
        }
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
}
