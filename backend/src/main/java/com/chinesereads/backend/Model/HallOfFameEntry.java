package com.chinesereads.backend.Model;

import java.sql.Blob;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

/**
 * Entrada del Hall of Fame (página pública /hall-of-fame): un influencer o
 * colaborador destacado, con foto, bio bilingüe, redes sociales, su código de
 * descuento y badges de reconocimiento. El contenido lo gestiona el admin
 * desde la propia web (patrón de la página /founder).
 *
 * Nota: es un dominio distinto del de códigos de descuento Stripe
 * (InfluencerService / /api/influencers); aquí el código es texto plano
 * que se muestra en la tarjeta pública.
 */
@Entity
public class HallOfFameEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;

    // Identificador estable para URLs (futuras páginas /hall-of-fame/:slug).
    @Column(unique = true, nullable = false)
    private String slug;

    // Frase corta bilingüe (mismo patrón de fallback que la bio). taglineEn
    // reutiliza la columna original "tagline" para conservar sin migración los
    // datos escritos antes de que el campo fuese bilingüe.
    @Column(name = "tagline")
    private String taglineEn;

    private String taglineEs;

    // Bio bilingüe (patrón de los textos): el frontend muestra la del idioma
    // activo y cae a la otra si está vacía.
    @Column(columnDefinition = "TEXT")
    private String bioEn;

    @Column(columnDefinition = "TEXT")
    private String bioEs;

    private String discountCode;

    private int displayOrder;

    @Lob
    private Blob photo;

    // Badge keys from HallOfFameBadges.ALLOWED (e.g. "pioneer"). Labels are
    // translated in the frontend. LinkedHashSet keeps the canonical order set
    // at save time.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hall_of_fame_entry_badges", joinColumns = @JoinColumn(name = "entry_id"))
    @Column(name = "badge")
    private Set<String> badges = new LinkedHashSet<>();

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<HallOfFameSocial> socials;

    public HallOfFameEntry() {
        this.socials = new ArrayList<>();
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

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTaglineEn() {
        return taglineEn;
    }

    public void setTaglineEn(String taglineEn) {
        this.taglineEn = taglineEn;
    }

    public String getTaglineEs() {
        return taglineEs;
    }

    public void setTaglineEs(String taglineEs) {
        this.taglineEs = taglineEs;
    }

    public String getBioEn() {
        return bioEn;
    }

    public void setBioEn(String bioEn) {
        this.bioEn = bioEn;
    }

    public String getBioEs() {
        return bioEs;
    }

    public void setBioEs(String bioEs) {
        this.bioEs = bioEs;
    }

    public String getDiscountCode() {
        return discountCode;
    }

    public void setDiscountCode(String discountCode) {
        this.discountCode = discountCode;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Blob getPhoto() {
        return photo;
    }

    public void setPhoto(Blob photo) {
        this.photo = photo;
    }

    public Set<String> getBadges() {
        return badges;
    }

    public void setBadges(Set<String> badges) {
        this.badges = badges != null ? badges : new LinkedHashSet<>();
    }

    public List<HallOfFameSocial> getSocials() {
        return socials;
    }

    public void setSocials(List<HallOfFameSocial> socials) {
        this.socials = socials;
    }
}
