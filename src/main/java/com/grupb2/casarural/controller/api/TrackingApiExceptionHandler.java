package com.grupb2.casarural.controller.api;

import com.grupb2.casarural.dto.api.ErrorDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;

@ControllerAdvice(annotations = RestController.class)
public class TrackingApiExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleAllExceptions(Exception ex, WebRequest request) {
        ErrorDto errorDto = new ErrorDto(
                Instant.now().toString(),
                HttpStatus.NOT_FOUND.value(),
                "Error: " + ex.getMessage()
        );
        return new ResponseEntity<>(errorDto, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({Throwable.class})
    public ResponseEntity<ErrorDto> handleAllExceptions(Throwable ex, WebRequest request) {
        ErrorDto errorDto = new ErrorDto(
                Instant.now().toString(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Error interno del servidor: " + ex.getMessage()
        );
        return new ResponseEntity<>(errorDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
