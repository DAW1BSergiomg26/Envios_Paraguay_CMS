package com.monteastur.envios.security;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.dto.api.ErrorDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final RBACAccessLogger rbacAccessLogger;
    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler(RBACAccessLogger rbacAccessLogger, ObjectMapper objectMapper) {
        this.rbacAccessLogger = rbacAccessLogger;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "ANONYMOUS";
        rbacAccessLogger.logFailure(username, "ACCESS_DENIED", request.getRequestURI(),
                request, "Access denied");
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(),
                    new ErrorDto(Instant.now().toString(), 400, "Access denied"));
        } else if (uri.equals("/cliente/login") || uri.equals("/login")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
        } else if (uri.startsWith("/cliente/")) {
            response.sendRedirect("/cliente/login");
        } else {
            response.sendRedirect("/login");
        }
    }
}
