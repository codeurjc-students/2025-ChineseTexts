package com.chinesereads.backend.Model;

import java.sql.Blob;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;

/**
 * Entrada concreta dentro de una sección: un empleo, una titulación, un
 * proyecto o cualquier elemento de una sección personalizada. Puede llevar un
 * logo opcional (p.ej. el de la universidad o la empresa).
 */
@Entity
public class FounderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String heading;      // título del puesto / titulación / proyecto
    private String subheading;   // empresa / universidad / subtítulo
    private String period;       // "2025 — Present"
    private String location;

    @Lob
    private String description;

    private String linkUrl;
    private String linkLabel;
    private int displayOrder;

    @Lob
    private Blob logo;

    @ManyToOne
    @JoinColumn(name = "section_id", nullable = false)
    private FounderSection section;

    public FounderItem() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public String getSubheading() {
        return subheading;
    }

    public void setSubheading(String subheading) {
        this.subheading = subheading;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public String getLinkLabel() {
        return linkLabel;
    }

    public void setLinkLabel(String linkLabel) {
        this.linkLabel = linkLabel;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Blob getLogo() {
        return logo;
    }

    public void setLogo(Blob logo) {
        this.logo = logo;
    }

    public FounderSection getSection() {
        return section;
    }

    public void setSection(FounderSection section) {
        this.section = section;
    }
}
