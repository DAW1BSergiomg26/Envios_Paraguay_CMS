package com.monteastur.envios.controller;

import com.monteastur.envios.dto.api.ErrorDto;
import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.exception.ConflictException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.time.format.DateTimeParseException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request, Model model) {
        if (isRestRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorDto(Instant.now().toString(), 404, ex.getMessage()));
        }
        return mvcError(request, model, HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public Object handleBadRequest(BadRequestException ex, HttpServletRequest request, Model model) {
        if (isRestRequest(request)) {
            return ResponseEntity.badRequest()
                .body(new ErrorDto(Instant.now().toString(), 400, ex.getMessage()));
        }
        return mvcError(request, model, HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public Object handleConflict(ConflictException ex, HttpServletRequest request, Model model) {
        if (isRestRequest(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorDto(Instant.now().toString(), 409, ex.getMessage()));
        }
        return mvcError(request, model, HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(DateTimeParseException.class)
    public Object handleDateTimeParse(DateTimeParseException ex, HttpServletRequest request, Model model) {
        if (isRestRequest(request)) {
            return ResponseEntity.badRequest()
                .body(new ErrorDto(Instant.now().toString(), 400, "Formato de fecha inválido. Use YYYY-MM-DD."));
        }
        return mvcError(request, model, HttpStatus.BAD_REQUEST, "Bad Request", "Formato de fecha inválido.");
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneric(Exception ex, HttpServletRequest request, Model model) {
        if (isRestRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorDto(Instant.now().toString(), 500, "Error interno del servidor"));
        }
        return mvcError(request, model, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Ha ocurrido un error inesperado. Por favor, inténtelo de nuevo más tarde.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request, Model model) {
        if (isRestRequest(request)) {
            return ResponseEntity.badRequest()
                .body(new ErrorDto(Instant.now().toString(), 400, ex.getMessage()));
        }
        return mvcError(request, model, HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public Object handleIllegalState(IllegalStateException ex, HttpServletRequest request, Model model) {
        if (isRestRequest(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorDto(Instant.now().toString(), 409, ex.getMessage()));
        }
        return mvcError(request, model, HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    private boolean isRestRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/");
    }

    private String mvcError(HttpServletRequest request, Model model, HttpStatus status, String error, String message) {
        model.addAttribute("status", status.value());
        model.addAttribute("error", error);
        model.addAttribute("message", message);
        model.addAttribute("timestamp", Instant.now().toString());
        boolean english = request.getRequestURI().startsWith("/en/") || request.getRequestURI().equals("/en");
        return english ? "en/error" : "error";
    }
}
