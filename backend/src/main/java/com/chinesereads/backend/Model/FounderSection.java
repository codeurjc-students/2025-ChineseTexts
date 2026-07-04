package com.chinesereads.backend.Model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

/**
 * Sección del perfil (Experiencia, Educación, Proyectos, Skills o una sección
 * personalizada creada por el admin). El campo {@code type} permite a la web
 * decidir cómo renderizarla (p.ej. las skills como etiquetas).
 */
@Entity
public class FounderSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String title;
    private String type;   // "EXPERIENCE" | "EDUCATION" | "PROJECTS" | "SKILLS" | "CUSTOM"
    private int displayOrder;

    @ManyToOne
    @JoinColumn(name = "profile_id", nullable = false)
    private FounderProfile profile;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<FounderItem> items;

    public FounderSection() {
        this.items = new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public List<FounderItem> getItems() {
        return items;
    }

    public void setItems(List<FounderItem> items) {
        this.items = items;
    }
}
