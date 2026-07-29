package com.chinesereads.backend.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Enlace a red social de una entrada del Hall of Fame (Instagram, TikTok,
 * YouTube…).
 */
@Entity
public class HallOfFameSocial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String label;
    private String icon;   // clase de Bootstrap Icons, p.ej. "bi-instagram"
    private String url;
    private int displayOrder;

    @ManyToOne
    @JoinColumn(name = "entry_id", nullable = false)
    private HallOfFameEntry entry;

    public HallOfFameSocial() {
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

    public HallOfFameEntry getEntry() {
        return entry;
    }

    public void setEntry(HallOfFameEntry entry) {
        this.entry = entry;
    }
}
