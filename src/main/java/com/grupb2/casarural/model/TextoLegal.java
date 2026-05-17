package com.grupb2.casarural.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "textos_legales")
public class TextoLegal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public TextoLegal() {}

    public TextoLegal(String slug, String titulo, String contenido) {
        this.slug = slug;
        this.titulo = titulo;
        this.contenido = contenido;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
