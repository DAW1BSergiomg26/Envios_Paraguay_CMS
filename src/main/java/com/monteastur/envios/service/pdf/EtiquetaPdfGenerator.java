package com.monteastur.envios.service.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfStream;
import com.lowagie.text.pdf.PdfWriter;
import com.monteastur.envios.model.EnvioTracking;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class EtiquetaPdfGenerator {

    public static final float ANCHO_PT = 100f * 72f / 25.4f;
    public static final float ALTO_PT = 150f * 72f / 25.4f;

    private static final Font TITULO = new Font(Font.HELVETICA, 15, Font.BOLD);
    private static final Font CODIGO = new Font(Font.HELVETICA, 13, Font.BOLD);
    private static final Font LABEL = new Font(Font.HELVETICA, 8, Font.BOLD);
    private static final Font VALOR = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font PIE = new Font(Font.HELVETICA, 8, Font.NORMAL);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT);

    private final BarcodeService barcodeService;

    public EtiquetaPdfGenerator(BarcodeService barcodeService) {
        this.barcodeService = barcodeService;
    }

    public byte[] generar(EnvioTracking envio, String trackingUrl) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(tamanoPagina(), 18f, 18f, 18f, 18f);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setCompressionLevel(PdfStream.NO_COMPRESSION);
            document.open();
            anadirEtiqueta(document, envio, trackingUrl);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar la etiqueta PDF", ex);
        }
    }

    public Rectangle tamanoPagina() {
        return new Rectangle(ANCHO_PT, ALTO_PT);
    }

    public void anadirEtiqueta(Document document, EnvioTracking envio, String trackingUrl) {
        document.add(new Paragraph("MONTEASTUR ENVÍOS", TITULO));
        document.add(new Paragraph("ETIQUETA DE ENVÍO", LABEL));
        document.add(new Paragraph("Código: " + envio.getCodigoUnico(), CODIGO));
        try {
            Image code128 = Image.getInstance(
                    barcodeService.generarCode128(envio.getCodigoUnico(), 500, 120), null, true);
            code128.scaleToFit(235f, 55f);
            code128.setAlignment(Element.ALIGN_CENTER);
            document.add(code128);

            Image qr = Image.getInstance(barcodeService.generarQr(trackingUrl, 200), null, true);
            qr.scaleToFit(60f, 60f);
            qr.setAlignment(Element.ALIGN_CENTER);
            document.add(qr);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudieron incrustar los códigos en la etiqueta", ex);
        }
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        anadirFila(tabla, "DESTINATARIO", envio.getDestinatario());
        anadirFila(tabla, "RUTA", (envio.getOrigen() != null ? envio.getOrigen() : "—")
                + " → " + (envio.getDestino() != null ? envio.getDestino() : "—"));
        anadirFila(tabla, "PESO", envio.getPeso() != null ? envio.getPeso() : "—");
        anadirFila(tabla, "CONTENIDO", envio.getContenido() != null ? envio.getContenido() : "—");
        anadirFila(tabla, "ESTADO", envio.getEstado() + " — " + envio.getUbicacionActual());
        document.add(tabla);
        document.add(new Paragraph("Emitida: " + FECHA.format(LocalDateTime.now()), PIE));
    }

    private void anadirFila(PdfPTable tabla, String label, String valor) {
        PdfPCell celdaLabel = new PdfPCell(new Phrase(label, LABEL));
        celdaLabel.setBorder(Rectangle.NO_BORDER);
        PdfPCell celdaValor = new PdfPCell(new Phrase(valor != null ? valor : "—", VALOR));
        celdaValor.setBorder(Rectangle.NO_BORDER);
        tabla.addCell(celdaLabel);
        tabla.addCell(celdaValor);
    }
}
