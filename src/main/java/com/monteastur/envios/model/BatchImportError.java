package com.monteastur.envios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "batch_import_errors", indexes = {
    @Index(name = "idx_batch_import_errors_batch_id", columnList = "batch_id")
})
public class BatchImportError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "linea_numero", nullable = false)
    private int lineaNumero;

    @Column(name = "codigo_rastreo", length = 100)
    private String codigoRastreo;

    @Column(name = "error_mensaje", nullable = false, columnDefinition = "TEXT")
    private String errorMensaje;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    public BatchImportError() {}

    public BatchImportError(Long batchId, int lineaNumero, String codigoRastreo, String errorMensaje) {
        this.batchId = batchId;
        this.lineaNumero = lineaNumero;
        this.codigoRastreo = codigoRastreo;
        this.errorMensaje = errorMensaje;
    }

    @PrePersist
    void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public int getLineaNumero() { return lineaNumero; }
    public void setLineaNumero(int lineaNumero) { this.lineaNumero = lineaNumero; }
    public String getCodigoRastreo() { return codigoRastreo; }
    public void setCodigoRastreo(String codigoRastreo) { this.codigoRastreo = codigoRastreo; }
    public String getErrorMensaje() { return errorMensaje; }
    public void setErrorMensaje(String errorMensaje) { this.errorMensaje = errorMensaje; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
