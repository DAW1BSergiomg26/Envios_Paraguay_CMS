package com.monteastur.envios.dto.api;

import com.monteastur.envios.model.Notificacion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Notificación de envío (incluye el errorMensaje solo para trazabilidad interna)")
public class NotificacionDto {

    @Schema(description = "Identificador de la notificación", example = "1")
    private Long id;

    @Schema(description = "Identificador del envío asociado", example = "10")
    private Long envioId;

    @Schema(description = "Destinatario de correo o nulo si fue omitida", example = "maria@correo.com")
    private String destinatario;

    @Schema(description = "Asunto del correo", example = "Cambio de estado")
    private String asunto;

    @Schema(description = "Cuerpo del mensaje")
    private String mensaje;

    @Schema(description = "Estado de la notificación", example = "ENVIADO", allowableValues = {"ENVIADO", "FALLIDO", "OMITIDO_SIN_DESTINATARIO"})
    private String estado;

    @Schema(description = "Mensaje de error en notificaciones fallidas")
    private String errorMensaje;

    @Schema(description = "Fecha de creación", example = "2026-07-29T12:00:00")
    private LocalDateTime fechaCreacion;

    public NotificacionDto() {}

    public static NotificacionDto from(Notificacion notificacion) {
        NotificacionDto dto = new NotificacionDto();
        dto.setId(notificacion.getId());
        dto.setEnvioId(notificacion.getEnvioId());
        dto.setDestinatario(notificacion.getDestinatario());
        dto.setAsunto(notificacion.getAsunto());
        dto.setMensaje(notificacion.getMensaje());
        dto.setEstado(notificacion.getEstado().name());
        dto.setErrorMensaje(notificacion.getErrorMensaje());
        dto.setFechaCreacion(notificacion.getFechaCreacion());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEnvioId() { return envioId; }
    public void setEnvioId(Long envioId) { this.envioId = envioId; }
    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }
    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getErrorMensaje() { return errorMensaje; }
    public void setErrorMensaje(String errorMensaje) { this.errorMensaje = errorMensaje; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
