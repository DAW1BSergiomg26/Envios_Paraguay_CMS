package com.monteastur.envios.security;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.dto.api.ErrorDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Punto de entrada de autenticación unificado. Para las peticiones a la API
 * (URI /api/ con Accept no HTML) responde 401 JSON con ErrorDto; para el resto
 * redirige a la página de login correspondiente (cliente o administración).
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final RBACAccessLogger rbacAccessLogger;
    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(RBACAccessLogger rbacAccessLogger, ObjectMapper objectMapper) {
        this.rbacAccessLogger = rbacAccessLogger;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String uri = request.getRequestURI();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "ANONYMOUS";
        rbacAccessLogger.logFailure(username, "AUTH_REQUIRED", uri, request, "Authentication required");

        if ((uri.startsWith("/api/") || uri.startsWith("/actuator")) && !aceptaHtml(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(),
                    new ErrorDto(Instant.now().toString(), 401, "Acceso no autenticado"));
        } else if (uri.startsWith("/cliente/")) {
            response.sendRedirect("/cliente/login");
        } else {
            response.sendRedirect("/login");
        }
    }

    private boolean aceptaHtml(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/html");
    }
}
