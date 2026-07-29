package com.monteastur.envios.controller;

import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.exception.ConflictException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/test/exception")
public class TestExceptionController {

    @GetMapping("/resource-not-found")
    public void resourceNotFound() {
        throw new ResourceNotFoundException("Test 404");
    }

    @GetMapping("/bad-request")
    public void badRequest() {
        throw new BadRequestException("Test 400");
    }

    @GetMapping("/conflict")
    public void conflict() {
        throw new ConflictException("Test 409");
    }

    @GetMapping("/illegal-argument")
    public void illegalArgument() {
        throw new IllegalArgumentException("Test illegal argument");
    }

    @GetMapping("/illegal-state")
    public void illegalState() {
        throw new IllegalStateException("Test illegal state");
    }

    @GetMapping("/date-time-parse")
    public void dateTimeParse() {
        throw new DateTimeParseException("Test bad date", "invalid", 0);
    }

    @GetMapping("/generic")
    public void generic() {
        throw new RuntimeException("Test 500");
    }
}
