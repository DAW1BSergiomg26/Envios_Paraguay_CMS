package com.monteastur.envios.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TrackingNoEncontradoException extends RuntimeException {

    private final String codigo;

    public TrackingNoEncontradoException(String codigo) {
        super("Tracking no encontrado: " + codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
