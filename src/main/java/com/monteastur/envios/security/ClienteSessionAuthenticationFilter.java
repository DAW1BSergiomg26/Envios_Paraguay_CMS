package com.monteastur.envios.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro ligero de autenticación del portal de cliente.
 * Si la HttpSession contiene "clienteId" y no existe autenticación previa,
 * crea y fija una Authentication con la authority ROLE_CLIENTE, integrando
 * el portal de cliente en la cadena de Spring Security sin migrar credenciales.
 * No consulta la base de datos (la baja de un cliente invalida su sesión).
 */
public class ClienteSessionAuthenticationFilter extends OncePerRequestFilter {

    public ClienteSessionAuthenticationFilter() {
        // Constructor vacío explícito (patrón de inyección por constructor del proyecto).
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing == null || !existing.isAuthenticated()) {
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("clienteId") instanceof Long clienteId) {
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        clienteId, null, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}
