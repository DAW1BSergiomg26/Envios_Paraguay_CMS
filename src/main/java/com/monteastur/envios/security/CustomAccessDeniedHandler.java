package com.monteastur.envios.security;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final RBACAccessLogger rbacAccessLogger;

    public CustomAccessDeniedHandler(RBACAccessLogger rbacAccessLogger) {
        this.rbacAccessLogger = rbacAccessLogger;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "ANONYMOUS";
        rbacAccessLogger.logFailure(username, "ACCESS_DENIED", request.getRequestURI(),
                request, "Access denied");
        throw new BadRequestException("Access denied");
    }
}
