package com.chinesereads.backend.Model;

import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

/**
 * Perfil personal del creador de la aplicación (página pública /founder).
 *
 * Existe una única fila de esta entidad. Su contenido lo gestiona el admin
 * desde la propia web (estilo LinkedIn), por lo que aquí sólo se define la
 * estructura; el texto y las imágenes viven en la base de datos.
 */
@Entity
public class FounderProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;
    private String role;
    private String tagline;
    private String location;

    @Lob
    private String summary;

    @Lob
    private Blob photo;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<FounderSocial> socials;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<FounderSection> sections;

    public FounderProfile() {
        this.socials = new ArrayList<>();
        this.sections = new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Blob getPhoto() {
        return photo;
    }

    public void setPhoto(Blob photo) {
        this.photo = photo;
    }

    public List<FounderSocial> getSocials() {
        return socials;
    }

    public void setSocials(List<FounderSocial> socials) {
        this.socials = socials;
    }

    public List<FounderSection> getSections() {
        return sections;
    }

    public void setSections(List<FounderSection> sections) {
        this.sections = sections;
    }
}
