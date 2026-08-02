package com.monteastur.envios.service.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

@Service
public class BarcodeService {

    private static final Map<EncodeHintType, Object> HINTS_CODE128 = new EnumMap<>(EncodeHintType.class);
    private static final Map<EncodeHintType, Object> HINTS_QR = new EnumMap<>(EncodeHintType.class);

    static {
        HINTS_CODE128.put(EncodeHintType.CHARACTER_SET, StandardCharsets.ISO_8859_1.name());
        HINTS_CODE128.put(EncodeHintType.MARGIN, 0);
        HINTS_QR.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        HINTS_QR.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        HINTS_QR.put(EncodeHintType.MARGIN, 1);
    }

    public BufferedImage generarCode128(String contenido, int ancho, int alto) {
        validar(contenido, "El contenido del código de barras no puede estar vacío");
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(contenido, BarcodeFormat.CODE_128,
                    ancho, alto, HINTS_CODE128);
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (WriterException ex) {
            throw new IllegalArgumentException("No se pudo generar el código Code128", ex);
        }
    }

    public BufferedImage generarQr(String contenido, int lado) {
        validar(contenido, "El contenido del código QR no puede estar vacío");
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(contenido, BarcodeFormat.QR_CODE,
                    lado, lado, HINTS_QR);
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (WriterException ex) {
            throw new IllegalArgumentException("No se pudo generar el código QR", ex);
        }
    }

    public byte[] toPng(BufferedImage imagen) {
        if (imagen == null) {
            throw new IllegalArgumentException("La imagen no puede ser nula");
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(imagen, "png", out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo codificar la imagen PNG", ex);
        }
    }

    private void validar(String contenido, String mensaje) {
        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
    }
}
