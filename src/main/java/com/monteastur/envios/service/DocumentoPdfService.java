package com.monteastur.envios.service;

import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.BatchImport;
import com.monteastur.envios.model.DocumentoGenerado;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.TipoDocumento;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.DocumentoGeneradoRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.service.batch.BatchImportPersistenceService;
import com.monteastur.envios.service.pdf.EtiquetaPdfGenerator;
import com.monteastur.envios.service.pdf.ManifiestoPdfGenerator;
import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Service
public class DocumentoPdfService {

    private final EnvioTrackingRepository envioTrackingRepository;
    private final ClienteRepository clienteRepository;
    private final DocumentoGeneradoRepository documentoRepository;
    private final BatchImportPersistenceService persistence;
    private final EtiquetaPdfGenerator etiquetaGenerator;
    private final ManifiestoPdfGenerator manifiestoGenerator;
    private final boolean enabled;
    private final int maxPages;
    private final String trackingBaseUrl;

    public DocumentoPdfService(EnvioTrackingRepository envioTrackingRepository,
                               ClienteRepository clienteRepository,
                               DocumentoGeneradoRepository documentoRepository,
                               BatchImportPersistenceService persistence,
                               EtiquetaPdfGenerator etiquetaGenerator,
                               ManifiestoPdfGenerator manifiestoGenerator,
                               @Value("${app.pdf.enabled:true}") boolean enabled,
                               @Value("${app.pdf.max-pages:5000}") int maxPages,
                               @Value("${app.pdf.tracking.base-url:http://localhost:8080/tracking}") String trackingBaseUrl) {
        this.envioTrackingRepository = envioTrackingRepository;
        this.clienteRepository = clienteRepository;
        this.documentoRepository = documentoRepository;
        this.persistence = persistence;
        this.etiquetaGenerator = etiquetaGenerator;
        this.manifiestoGenerator = manifiestoGenerator;
        this.enabled = enabled;
        this.maxPages = maxPages;
        this.trackingBaseUrl = trackingBaseUrl;
    }

    public byte[] generarEtiqueta(String codigoUnico, String usuario) {
        verificarHabilitado();
        EnvioTracking envio = envioTrackingRepository.findByCodigoUnico(codigoUnico)
                .orElseThrow(() -> new ResourceNotFoundException("Envío no encontrado: " + codigoUnico));
        byte[] pdf = etiquetaGenerator.generar(envio, urlTracking(codigoUnico));
        auditar(TipoDocumento.ETIQUETA_TERMICA, codigoUnico, "etiqueta-" + codigoUnico + ".pdf", pdf.length, usuario);
        return pdf;
    }

    public void generarEtiquetasLote(Long batchId, String usuario, OutputStream destino) {
        verificarHabilitado();
        persistence.obtenerLote(batchId);
        long total = envioTrackingRepository.countByBatchId(batchId);
        if (total > maxPages) {
            throw new BadRequestException("El lote tiene " + total
                    + " envíos y el máximo permitido por descarga es " + maxPages);
        }
        List<EnvioTracking> envios = envioTrackingRepository.findByBatchIdOrderByCodigoUnicoAsc(batchId);
        CountingOutputStream contador = new CountingOutputStream(destino);
        Document document = new Document(etiquetaGenerator.tamanoPagina(), 18f, 18f, 18f, 18f);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, contador);
            writer.setCloseStream(false);
            document.open();
            for (int i = 0; i < envios.size(); i++) {
                if (i > 0) {
                    document.newPage();
                }
                etiquetaGenerator.anadirEtiqueta(document, envios.get(i), urlTracking(envios.get(i).getCodigoUnico()));
            }
            document.close();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el PDF de etiquetas del lote " + batchId, ex);
        }
        auditar(TipoDocumento.ETIQUETAS_LOTE, String.valueOf(batchId),
                "etiquetas-lote-" + batchId + ".pdf", contador.getContador(), usuario);
    }

    public byte[] generarManifiesto(Long batchId, String usuario) {
        verificarHabilitado();
        BatchImport lote = persistence.obtenerLote(batchId);
        String clienteNombre = nombreCliente(lote.getClienteId());
        List<EnvioTracking> envios = envioTrackingRepository.findByBatchIdOrderByCodigoUnicoAsc(batchId);
        byte[] pdf = manifiestoGenerator.generar(batchId, envios, clienteNombre);
        auditar(TipoDocumento.MANIFIESTO_CARGA, String.valueOf(batchId),
                "manifiesto-lote-" + batchId + ".pdf", pdf.length, usuario);
        return pdf;
    }

    public List<DocumentoGenerado> listarEmisiones(TipoDocumento tipo) {
        if (tipo == null) {
            return documentoRepository.findByOrderByFechaCreacionDesc();
        }
        return documentoRepository.findAllByTipoOrderByFechaCreacionDesc(tipo);
    }

    private String nombreCliente(Long clienteId) {
        if (clienteId == null) {
            return null;
        }
        return clienteRepository.findById(clienteId).map(cliente -> cliente.getNombre()).orElse(null);
    }

    private String urlTracking(String codigo) {
        String base = trackingBaseUrl.endsWith("/")
                ? trackingBaseUrl.substring(0, trackingBaseUrl.length() - 1)
                : trackingBaseUrl;
        return base + "/" + codigo;
    }

    private void auditar(TipoDocumento tipo, String referenciaId, String nombreArchivo,
                         int pesoBytes, String usuario) {
        documentoRepository.save(new DocumentoGenerado(tipo, referenciaId, nombreArchivo, pesoBytes, usuario));
    }

    private void verificarHabilitado() {
        if (!enabled) {
            throw new BadRequestException("La generación de documentos PDF está deshabilitada");
        }
    }

    private static final class CountingOutputStream extends OutputStream {
        private final OutputStream destino;
        private int contador;

        private CountingOutputStream(OutputStream destino) {
            this.destino = destino;
        }

        @Override
        public void write(int b) throws IOException {
            destino.write(b);
            contador++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            destino.write(b, off, len);
            contador += len;
        }

        @Override
        public void flush() throws IOException {
            destino.flush();
        }

        int getContador() {
            return contador;
        }
    }
}
