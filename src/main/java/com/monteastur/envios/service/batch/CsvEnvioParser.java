package com.monteastur.envios.service.batch;

import com.monteastur.envios.model.EnvioTracking;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Set;

@Component
public final class CsvEnvioParser {

    private static final int NUMERO_COLUMNAS = 8;
    private static final int MAX_LONGITUD_CAMPO = 255;
    private static final String BOM = "\uFEFF";

    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            "RECIBIDO", "EN_ADUANA_ORIGEN", "EN_TRANSITO",
            "EN_ADUANA_DESTINO", "EN_REPARTO", "ENTREGADO");

    private final int maxLineLength;

    public CsvEnvioParser(@Value("${app.batch.max-line-length:10000}") int maxLineLength) {
        this.maxLineLength = maxLineLength;
    }

    public int procesar(Reader reader, CsvLineConsumer consumer) throws IOException {
        CSVReader csvReader = new CSVReaderBuilder(reader).build();
        int filas = 0;
        int numeroLinea = 1;
        try {
            String[] cabecera = csvReader.readNext();
            if (cabecera != null && cabecera.length > 0) {
                quitarBom(cabecera[0]);
            }
            String[] campos;
            while ((campos = csvReader.readNext()) != null) {
                numeroLinea++;
                filas++;
                consumer.aceptar(parseLine(campos, numeroLinea));
            }
        } catch (CsvException ex) {
            throw new IOException("Error de formato CSV en la línea " + numeroLinea, ex);
        }
        return filas;
    }

    private CsvEnvioRow parseLine(String[] campos, int numeroLinea) {
        int longitudLinea = campos.length - 1;
        for (String campo : campos) {
            if (campo != null) {
                longitudLinea += campo.length();
            }
        }
        if (longitudLinea > maxLineLength) {
            return CsvEnvioRow.conError(new CsvImportLineError(numeroLinea, null,
                    "Línea demasiado larga: supera el máximo de " + maxLineLength + " caracteres"));
        }
        if (campos.length < 3 || campos.length > NUMERO_COLUMNAS) {
            return CsvEnvioRow.conError(new CsvImportLineError(numeroLinea, null,
                    "Número de columnas incorrecto: se esperaban " + NUMERO_COLUMNAS + " columnas"));
        }
        campos = Arrays.copyOf(campos, NUMERO_COLUMNAS);
        String codigo = trim(campos[0]);
        if (codigo.isEmpty()) {
            return CsvEnvioRow.conError(new CsvImportLineError(numeroLinea, null,
                    "El campo codigo es obligatorio"));
        }
        if (codigo.length() > MAX_LONGITUD_CAMPO) {
            return CsvEnvioRow.conError(new CsvImportLineError(numeroLinea, codigo,
                    "El campo codigo supera los " + MAX_LONGITUD_CAMPO + " caracteres"));
        }
        String estado = trim(campos[1]).toUpperCase();
        if (estado.isEmpty()) {
            return CsvEnvioRow.conError(new CsvImportLineError(numeroLinea, codigo,
                    "El campo estado es obligatorio"));
        }
        if (!ESTADOS_VALIDOS.contains(estado)) {
            return CsvEnvioRow.conError(new CsvImportLineError(numeroLinea, codigo,
                    "El campo estado no es válido: " + estado));
        }
        String destinatario = trim(campos[2]);
        if (destinatario.isEmpty()) {
            return CsvEnvioRow.conError(new CsvImportLineError(numeroLinea, codigo,
                    "El campo destinatario es obligatorio"));
        }
        if (destinatario.length() > MAX_LONGITUD_CAMPO) {
            return CsvEnvioRow.conError(new CsvImportLineError(numeroLinea, codigo,
                    "El campo destinatario supera los " + MAX_LONGITUD_CAMPO + " caracteres"));
        }
        EnvioTracking envio = new EnvioTracking(codigo, estado, destinatario,
                nuloSiVacio(campos[3]), nuloSiVacio(campos[4]),
                nuloSiVacio(campos[5]), nuloSiVacio(campos[6]));
        envio.setObservaciones(nuloSiVacio(campos[7]));
        return CsvEnvioRow.valida(envio, numeroLinea);
    }

    private static String trim(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static String nuloSiVacio(String valor) {
        String limpio = trim(valor);
        return limpio.isEmpty() ? null : limpio;
    }

    private static String quitarBom(String valor) {
        return (valor != null && valor.startsWith(BOM)) ? valor.substring(BOM.length()) : valor;
    }
}
