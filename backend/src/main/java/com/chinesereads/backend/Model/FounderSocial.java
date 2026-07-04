package com.chinesereads.backend.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Enlace de contacto / red social del perfil (GitHub, LinkedIn, email…).
 */
@Entity
public class FounderSocial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String label;
    private String icon;   // clase de Bootstrap Icons, p.ej. "bi-github"
    private String url;
    private int displayOrder;

    @ManyToOne
    @JoinColumn(name = "profile_id", nullable = false)
    private FounderProfile profile;

    public FounderSocial() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public FounderProfile getProfile() {
        return profile;
    }

    public void setProfile(FounderProfile profile) {
        this.profile = profile;
    }
}
