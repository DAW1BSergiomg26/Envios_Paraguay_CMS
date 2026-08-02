package com.monteastur.envios.service.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

@Component
public class ManifiestoPdfGenerator {

    public static final float ANCHO_A4_PT = 595.28f;
    public static final float ALTO_A4_PT = 841.89f;

    private static final Font TITULO = new Font(Font.HELVETICA, 18, Font.BOLD);
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 11, Font.NORMAL);
    private static final Font CABECERA_TABLA = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font CELDA = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Font TOTAL = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);

    public byte[] generar(Long batchId, List<EnvioTracking> envios, String clienteNombre) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40f, 40f, 40f, 40f);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setCompressionLevel(PdfStream.NO_COMPRESSION);
            document.open();
            anadirCabecera(document, batchId, clienteNombre);
            anadirTabla(document, envios);
            anadirFirma(document);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el manifiesto PDF", ex);
        }
    }

    private void anadirCabecera(Document document, Long batchId, String clienteNombre) {
        document.add(new Paragraph("MONTEASTUR ENVÍOS", TITULO));
        document.add(new Paragraph("MANIFIESTO DE CARGA / GUÍA DE REMISIÓN", SUBTITULO));
        document.add(new Paragraph("Lote: " + batchId + "   Fecha: " + FECHA.format(LocalDate.now()), SUBTITULO));
        document.add(new Paragraph("Cliente: " + (clienteNombre != null ? clienteNombre : "—"), SUBTITULO));
    }

    private void anadirTabla(Document document, List<EnvioTracking> envios) {
        PdfPTable tabla = new PdfPTable(5);
        tabla.setWidths(new float[]{2.2f, 2.6f, 2.8f, 1.0f, 1.8f});
        tabla.setWidthPercentage(100);
        for (String cabecera : new String[]{"CÓDIGO", "DESTINATARIO", "CONTENIDO", "PESO", "ESTADO"}) {
            PdfPCell cell = new PdfPCell(new Phrase(cabecera, CABECERA_TABLA));
            cell.setBackgroundColor(new java.awt.Color(230, 230, 230));
            tabla.addCell(cell);
        }
        double totalPeso = 0;
        boolean hayPesoValido = false;
        for (EnvioTracking e : envios) {
            tabla.addCell(new PdfPCell(new Phrase(e.getCodigoUnico(), CELDA)));
            tabla.addCell(new PdfPCell(new Phrase(normalizar(e.getDestinatario()), CELDA)));
            tabla.addCell(new PdfPCell(new Phrase(normalizar(e.getContenido()), CELDA)));
            tabla.addCell(new PdfPCell(new Phrase(normalizar(e.getPeso()), CELDA)));
            tabla.addCell(new PdfPCell(new Phrase(normalizar(e.getEstado()), CELDA)));
            OptionalDouble peso = PesoUtil.parsear(e.getPeso());
            if (peso.isPresent()) {
                totalPeso += peso.getAsDouble();
                hayPesoValido = true;
            }
        }
        PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL (" + envios.size() + " bultos)", TOTAL));
        totalLabel.setColspan(4);
        String pesoTotal = hayPesoValido
                ? String.format(Locale.ROOT, "%.2f kg", totalPeso) : "—";
        PdfPCell totalPesoCell = new PdfPCell(new Phrase(pesoTotal, TOTAL));
        tabla.addCell(totalLabel);
        tabla.addCell(totalPesoCell);
        document.add(tabla);
    }

    private void anadirFirma(Document document) {
        document.add(new Paragraph("\n\n"));
        document.add(new Paragraph("Firma de despacho", new Font(Font.HELVETICA, 12, Font.BOLD)));
        document.add(new Paragraph("_______________________________________", SUBTITULO));
        document.add(new Paragraph("Firma y aclaración del agente autorizado", SUBTITULO));
    }

    private String normalizar(String valor) {
        return valor != null ? valor : "—";
    }
}
