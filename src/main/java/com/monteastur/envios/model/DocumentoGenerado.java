package com.monteastur.envios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "documentos_generados")
public class DocumentoGenerado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoDocumento tipo;

    @Column(name = "referencia_id", nullable = false, length = 100)
    private String referenciaId;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "peso_bytes", nullable = false)
    private int pesoBytes;

    @Column(name = "usuario_generacion", length = 100)
    private String usuarioGeneracion;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    public DocumentoGenerado() {}

    public DocumentoGenerado(TipoDocumento tipo, String referenciaId, String nombreArchivo,
                             int pesoBytes, String usuarioGeneracion) {
        this.tipo = tipo;
        this.referenciaId = referenciaId;
        this.nombreArchivo = nombreArchivo;
        this.pesoBytes = pesoBytes;
        this.usuarioGeneracion = usuarioGeneracion;
    }

    @PrePersist
    void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TipoDocumento getTipo() { return tipo; }
    public void setTipo(TipoDocumento tipo) { this.tipo = tipo; }
    public String getReferenciaId() { return referenciaId; }
    public void setReferenciaId(String referenciaId) { this.referenciaId = referenciaId; }
    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
    public int getPesoBytes() { return pesoBytes; }
    public void setPesoBytes(int pesoBytes) { this.pesoBytes = pesoBytes; }
    public String getUsuarioGeneracion() { return usuarioGeneracion; }
    public void setUsuarioGeneracion(String usuarioGeneracion) { this.usuarioGeneracion = usuarioGeneracion; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
