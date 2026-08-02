package com.monteastur.envios.dto.web;

import java.util.ArrayList;
import java.util.List;

public class ClientDashboardView {

    private Long clienteId;
    private String clienteNombre;
    private String clienteEmail;
    private int totalEnvios;
    private int enviosActivos;
    private int enviosEntregados;
    private double pesoTotalKg;
    private double pesoActivoKg;
    private List<EnvioResumenView> envios = new ArrayList<>();

    public ClientDashboardView() {}

    public ClientDashboardView(Long clienteId, String clienteNombre, String clienteEmail,
                               int totalEnvios, int enviosActivos, int enviosEntregados,
                               double pesoTotalKg, double pesoActivoKg, List<EnvioResumenView> envios) {
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.clienteEmail = clienteEmail;
        this.totalEnvios = totalEnvios;
        this.enviosActivos = enviosActivos;
        this.enviosEntregados = enviosEntregados;
        this.pesoTotalKg = pesoTotalKg;
        this.pesoActivoKg = pesoActivoKg;
        this.envios = envios != null ? envios : new ArrayList<>();
    }

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public String getClienteEmail() { return clienteEmail; }
    public void setClienteEmail(String clienteEmail) { this.clienteEmail = clienteEmail; }
    public int getTotalEnvios() { return totalEnvios; }
    public void setTotalEnvios(int totalEnvios) { this.totalEnvios = totalEnvios; }
    public int getEnviosActivos() { return enviosActivos; }
    public void setEnviosActivos(int enviosActivos) { this.enviosActivos = enviosActivos; }
    public int getEnviosEntregados() { return enviosEntregados; }
    public void setEnviosEntregados(int enviosEntregados) { this.enviosEntregados = enviosEntregados; }
    public double getPesoTotalKg() { return pesoTotalKg; }
    public void setPesoTotalKg(double pesoTotalKg) { this.pesoTotalKg = pesoTotalKg; }
    public double getPesoActivoKg() { return pesoActivoKg; }
    public void setPesoActivoKg(double pesoActivoKg) { this.pesoActivoKg = pesoActivoKg; }
    public List<EnvioResumenView> getEnvios() { return envios; }
    public void setEnvios(List<EnvioResumenView> envios) { this.envios = envios; }
}
