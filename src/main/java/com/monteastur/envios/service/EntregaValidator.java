package com.monteastur.envios.service;

import com.monteastur.envios.dto.api.RegistrarEntregaRequest;
import com.monteastur.envios.exception.BadRequestException;

import java.math.BigDecimal;
import java.util.Base64;

public final class EntregaValidator {

    private static final int MAX_NOMBRE_LENGTH = 150;
    private static final int MAX_DOCUMENTO_LENGTH = 50;
    private static final int MAX_FIRMA_BASE64_LENGTH = 5_242_880;
    private static final int MAX_NOTAS_LENGTH = 2000;
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final BigDecimal LATITUD_MIN = new BigDecimal("-90");
    private static final BigDecimal LATITUD_MAX = new BigDecimal("90");
    private static final BigDecimal LONGITUD_MIN = new BigDecimal("-180");
    private static final BigDecimal LONGITUD_MAX = new BigDecimal("180");

    private EntregaValidator() {}

    public static void validar(RegistrarEntregaRequest request) {
        validarReceptorNombre(request.getReceptorNombre());
        validarReceptorDocumento(request.getReceptorDocumento());
        validarFirmaBase64(request.getFirmaBase64());
        validarCoordenadas(request.getLatitud(), request.getLongitud());
        validarNotas(request.getNotas());
    }

    public static void validarFirmaBase64(String firmaBase64) {
        if (firmaBase64 == null || firmaBase64.isBlank()) {
            throw new BadRequestException("La firma es obligatoria");
        }
        if (firmaBase64.length() > MAX_FIRMA_BASE64_LENGTH) {
            throw new BadRequestException("La firma supera el tamaño máximo permitido");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(firmaBase64);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("La firma no es un Base64 válido");
        }
        if (bytes.length < PNG_MAGIC.length
                || bytes[0] != PNG_MAGIC[0] || bytes[1] != PNG_MAGIC[1]
                || bytes[2] != PNG_MAGIC[2] || bytes[3] != PNG_MAGIC[3]) {
            throw new BadRequestException("La firma debe ser una imagen PNG");
        }
    }

    public static void validarCoordenadas(BigDecimal latitud, BigDecimal longitud) {
        if (latitud != null && (latitud.compareTo(LATITUD_MIN) < 0 || latitud.compareTo(LATITUD_MAX) > 0)) {
            throw new BadRequestException("Latitud fuera de rango: debe estar entre -90 y 90");
        }
        if (longitud != null && (longitud.compareTo(LONGITUD_MIN) < 0 || longitud.compareTo(LONGITUD_MAX) > 0)) {
            throw new BadRequestException("Longitud fuera de rango: debe estar entre -180 y 180");
        }
    }

    private static void validarReceptorNombre(String receptorNombre) {
        if (receptorNombre == null || receptorNombre.isBlank()) {
            throw new BadRequestException("El nombre del receptor es obligatorio");
        }
        if (receptorNombre.length() > MAX_NOMBRE_LENGTH) {
            throw new BadRequestException("El nombre del receptor no puede superar 150 caracteres");
        }
    }

    private static void validarReceptorDocumento(String receptorDocumento) {
        if (receptorDocumento == null || receptorDocumento.isBlank()) {
            throw new BadRequestException("El documento del receptor es obligatorio");
        }
        if (receptorDocumento.length() > MAX_DOCUMENTO_LENGTH) {
            throw new BadRequestException("El documento del receptor no puede superar 50 caracteres");
        }
    }

    private static void validarNotas(String notas) {
        if (notas != null && notas.length() > MAX_NOTAS_LENGTH) {
            throw new BadRequestException("Las notas no pueden superar 2000 caracteres");
        }
    }
}
