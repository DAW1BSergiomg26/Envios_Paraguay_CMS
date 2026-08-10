package com.monteastur.envios.dto.api;

import com.monteastur.envios.model.Cliente;

public class ClienteResumenDto {

    private Long id;
    private String nombre;

    public ClienteResumenDto() {}

    public static ClienteResumenDto from(Cliente cliente) {
        ClienteResumenDto dto = new ClienteResumenDto();
        dto.setId(cliente.getId());
        dto.setNombre(cliente.getNombre());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
