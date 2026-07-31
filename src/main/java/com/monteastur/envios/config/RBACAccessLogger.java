package com.monteastur.envios.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class RBACAccessLogger {

    private final JdbcTemplate jdbcTemplate;

    public RBACAccessLogger(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void logSuccess(String username, String accion, String recurso, HttpServletRequest request) {
        insert(username, accion, recurso, ipOrigen(request), userAgent(request), true, null);
    }

    public void logSuccess(String username, String accion, String recurso) {
        insert(username, accion, recurso, null, null, true, null);
    }

    public void logFailure(String username, String accion, String recurso, HttpServletRequest request, String descripcion) {
        insert(username, accion, recurso, ipOrigen(request), userAgent(request), false, descripcion);
    }

    public void logFailure(String username, String accion, String recurso, String descripcion) {
        insert(username, accion, recurso, null, null, false, descripcion);
    }

    private void insert(String username, String accion, String recurso, String ip, String userAgent,
                        boolean exitoso, String descripcion) {
        jdbcTemplate.update(
            "INSERT INTO auditoria_accesos " +
            "(user_id, username, accion, recurso, ip_origen, user_agent, exitoso, descripcion) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            resolveUserId(username), username, accion, recurso, ip, userAgent, exitoso, descripcion);
    }

    private Long resolveUserId(String username) {
        if (username == null) {
            return null;
        }
        List<Long> ids = jdbcTemplate.queryForList(
            "SELECT id FROM users WHERE username = ?", Long.class, username);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private String ipOrigen(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String userAgent(HttpServletRequest request) {
        return request == null ? null : request.getHeader("User-Agent");
    }
}
