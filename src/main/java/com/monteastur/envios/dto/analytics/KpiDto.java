package com.monteastur.envios.dto.analytics;

public class KpiDto {

    private String label;
    private double value;
    private String color;

    public KpiDto() {}

    public KpiDto(String label, double value, String color) {
        this.label = label;
        this.value = value;
        this.color = color;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
