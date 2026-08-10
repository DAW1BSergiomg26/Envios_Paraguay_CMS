package com.monteastur.envios.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Mensaje de contacto visible para el administrador")
public class MensajeContactoAdminDto {
    @Schema(description = "ID único del mensaje", example = "7")
    private Long id;

    @Schema(description = "Nombre de quien escribió", example = "Ana López")
    private String nombre;

    @Schema(description = "Email de contacto", example = "ana@example.com")
    private String email;

    @Schema(description = "Teléfono de contacto", example = "+34 644 444 444")
    private String telefono;

    @Schema(description = "Contenido del mensaje", example = "Hola, quiero información sobre envíos a Asunción")
    private String mensaje;

    @Schema(description = "Fecha de envío del mensaje")
    private LocalDateTime fechaEnvio;

    @Schema(description = "Indica si el mensaje ha sido leído por el administrador", example = "false")
    private boolean leido;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(LocalDateTime fechaEnvio) { this.fechaEnvio = fechaEnvio; }
    public boolean isLeido() { return leido; }
    public void setLeido(boolean leido) { this.leido = leido; }
}
