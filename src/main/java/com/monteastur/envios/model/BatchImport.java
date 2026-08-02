package com.monteastur.envios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "batch_imports", indexes = {
    @Index(name = "idx_batch_imports_cliente_id", columnList = "cliente_id"),
    @Index(name = "idx_batch_imports_estado", columnList = "estado")
})
public class BatchImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "total_registros", nullable = false)
    private int totalRegistros;

    @Column(nullable = false)
    private int procesados;

    @Column(nullable = false)
    private int exitosos;

    @Column(nullable = false)
    private int fallidos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchImportEstado estado;

    @Column(name = "error_resumen", columnDefinition = "TEXT")
    private String errorResumen;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    public BatchImport() {}

    public BatchImport(Long clienteId, String nombreArchivo, BatchImportEstado estado) {
        this.clienteId = clienteId;
        this.nombreArchivo = nombreArchivo;
        this.estado = estado;
    }

    @PrePersist
    void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
    public int getTotalRegistros() { return totalRegistros; }
    public void setTotalRegistros(int totalRegistros) { this.totalRegistros = totalRegistros; }
    public int getProcesados() { return procesados; }
    public void setProcesados(int procesados) { this.procesados = procesados; }
    public int getExitosos() { return exitosos; }
    public void setExitosos(int exitosos) { this.exitosos = exitosos; }
    public int getFallidos() { return fallidos; }
    public void setFallidos(int fallidos) { this.fallidos = fallidos; }
    public BatchImportEstado getEstado() { return estado; }
    public void setEstado(BatchImportEstado estado) { this.estado = estado; }
    public String getErrorResumen() { return errorResumen; }
    public void setErrorResumen(String errorResumen) { this.errorResumen = errorResumen; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }
}
