package com.monteastur.envios.dto.api;

import com.monteastur.envios.model.BatchImport;

import java.time.LocalDateTime;

public class BatchImportResponseDto {

    private Long id;
    private Long clienteId;
    private String nombreArchivo;
    private int totalRegistros;
    private int procesados;
    private int exitosos;
    private int fallidos;
    private String estado;
    private String errorResumen;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaFin;

    public BatchImportResponseDto() {}

    public static BatchImportResponseDto from(BatchImport lote) {
        BatchImportResponseDto dto = new BatchImportResponseDto();
        dto.setId(lote.getId());
        dto.setClienteId(lote.getClienteId());
        dto.setNombreArchivo(lote.getNombreArchivo());
        dto.setTotalRegistros(lote.getTotalRegistros());
        dto.setProcesados(lote.getProcesados());
        dto.setExitosos(lote.getExitosos());
        dto.setFallidos(lote.getFallidos());
        dto.setEstado(lote.getEstado().name());
        dto.setErrorResumen(lote.getErrorResumen());
        dto.setFechaCreacion(lote.getFechaCreacion());
        dto.setFechaFin(lote.getFechaFin());
        return dto;
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
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getErrorResumen() { return errorResumen; }
    public void setErrorResumen(String errorResumen) { this.errorResumen = errorResumen; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }
}
