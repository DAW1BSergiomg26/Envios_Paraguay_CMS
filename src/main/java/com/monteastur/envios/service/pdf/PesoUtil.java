package com.monteastur.envios.service.pdf;

import java.util.OptionalDouble;

public final class PesoUtil {

    private PesoUtil() {}

    public static OptionalDouble parsear(String peso) {
        if (peso == null || peso.isBlank()) {
            return OptionalDouble.empty();
        }
        String normalizado = peso.trim().replace(',', '.');
        int i = 0;
        while (i < normalizado.length() && (Character.isDigit(normalizado.charAt(i))
                || normalizado.charAt(i) == '.')) {
            i++;
        }
        if (i == 0) {
            return OptionalDouble.empty();
        }
        try {
            double valor = Double.parseDouble(normalizado.substring(0, i));
            return valor >= 0 ? OptionalDouble.of(valor) : OptionalDouble.empty();
        } catch (NumberFormatException ex) {
            return OptionalDouble.empty();
        }
    }
}
