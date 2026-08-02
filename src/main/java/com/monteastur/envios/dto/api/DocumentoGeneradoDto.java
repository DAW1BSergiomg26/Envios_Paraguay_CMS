package com.monteastur.envios.dto.api;

import com.monteastur.envios.model.DocumentoGenerado;

public class DocumentoGeneradoDto {

    private final Long id;
    private final String tipo;
    private final String referenciaId;
    private final String nombreArchivo;
    private final int pesoBytes;
    private final String usuarioGeneracion;
    private final String fechaCreacion;

    private DocumentoGeneradoDto(Long id, String tipo, String referenciaId, String nombreArchivo,
                                 int pesoBytes, String usuarioGeneracion, String fechaCreacion) {
        this.id = id;
        this.tipo = tipo;
        this.referenciaId = referenciaId;
        this.nombreArchivo = nombreArchivo;
        this.pesoBytes = pesoBytes;
        this.usuarioGeneracion = usuarioGeneracion;
        this.fechaCreacion = fechaCreacion;
    }

    public static DocumentoGeneradoDto from(DocumentoGenerado doc) {
        return new DocumentoGeneradoDto(doc.getId(), doc.getTipo().name(), doc.getReferenciaId(),
                doc.getNombreArchivo(), doc.getPesoBytes(), doc.getUsuarioGeneracion(),
                doc.getFechaCreacion() != null ? doc.getFechaCreacion().toString() : null);
    }

    public Long getId() { return id; }
    public String getTipo() { return tipo; }
    public String getReferenciaId() { return referenciaId; }
    public String getNombreArchivo() { return nombreArchivo; }
    public int getPesoBytes() { return pesoBytes; }
    public String getUsuarioGeneracion() { return usuarioGeneracion; }
    public String getFechaCreacion() { return fechaCreacion; }
}
