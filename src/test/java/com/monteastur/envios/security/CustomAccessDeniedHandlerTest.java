package com.monteastur.envios.security;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomAccessDeniedHandlerTest {

    @Test
    void handle_loggeaFalloYLanzaBadRequest() throws Exception {
        RBACAccessLogger logger = mock(RBACAccessLogger.class);
        CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler(logger);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/api/v1/admin/envios");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> handler.handle(request, response, new AccessDeniedException("denegado")));

        assertTrue(ex.getMessage().contains("Access denied"));
        verify(logger).logFailure("ANONYMOUS", "ACCESS_DENIED", "/api/v1/admin/envios", request, "Access denied");
    }
}
