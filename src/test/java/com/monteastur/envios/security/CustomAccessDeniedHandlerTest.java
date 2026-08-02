package com.monteastur.envios.security;

import com.monteastur.envios.config.RBACAccessLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomAccessDeniedHandlerTest {

    @Test
    void handle_rest_loggeaFalloYEscribe400Json() throws Exception {
        RBACAccessLogger logger = mock(RBACAccessLogger.class);
        CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler(logger, new ObjectMapper());
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter body = new StringWriter();

        when(request.getRequestURI()).thenReturn("/api/v1/deliveries/MT-1/pod");
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        handler.handle(request, response, new AccessDeniedException("denegado"));

        verify(response).setStatus(400);
        verify(response).setContentType("application/json");
        verify(logger).logFailure("ANONYMOUS", "ACCESS_DENIED", "/api/v1/deliveries/MT-1/pod", request, "Access denied");
        assertTrue(body.toString().contains("\"status\":400"));
        assertTrue(body.toString().contains("\"error\":\"Access denied\""));
    }

    @Test
    void handle_noRest_enviaError400() throws Exception {
        RBACAccessLogger logger = mock(RBACAccessLogger.class);
        CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler(logger, new ObjectMapper());
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/admin/dashboard");

        handler.handle(request, response, new AccessDeniedException("denegado"));

        verify(response).sendError(400, "Access denied");
        verify(logger).logFailure("ANONYMOUS", "ACCESS_DENIED", "/admin/dashboard", request, "Access denied");
    }

    @Test
    void handle_autenticado_usaNombreDeUsuario() throws Exception {
        RBACAccessLogger logger = mock(RBACAccessLogger.class);
        CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler(logger, new ObjectMapper());
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/api/v1/admin/envios");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "operador", "pass", java.util.List.of()));

        try {
            handler.handle(request, response, new AccessDeniedException("denegado"));
            verify(logger).logFailure("operador", "ACCESS_DENIED", "/api/v1/admin/envios", request, "Access denied");
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}
