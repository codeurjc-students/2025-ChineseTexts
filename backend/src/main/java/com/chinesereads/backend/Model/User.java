package com.chinesereads.backend.Model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String email;
    private String name;
    private String password;
    private String language;

    // Si está bloqueado, no puede iniciar sesión (Spring Security lo rechaza).
    private boolean blocked = false;
    // Fecha de alta y última conexión, usadas por el panel de administración.
    private LocalDate registrationDate;
    private LocalDateTime lastAccess;

    // Contador mensual de creaciones de texto propio (OCR + pegado) para el límite
    // por usuario. Se reinicia cuando cambia el mes (ver usagePeriodStart).
    private int monthlyTextCount = 0;
    private LocalDate usagePeriodStart;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Collection> collections = new ArrayList<>();

    // Textos privados del usuario. Cascade para que al borrar el usuario se borren
    // también sus textos (y sus palabras) sin violar la clave foránea.
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserText> userTexts = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
	private List<String> roles;

    public User(){}

    public User(String email, String name, String password, String language, String... roles){
        this.email = email;
        this.name = name;
        this.password = password;
        this.language = language;
        if (roles == null) {
            this.setRoles(Collections.singletonList("USER"));
        } else {
            this.roles = List.of(roles);
        }
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public String getLanguage(){
        return language;
    }

    public List<Collection> getCollections(){
        return this.collections;
    }

    public List<String> getRoles() {
		return roles;
	}

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setLanguage(String language){
        this.language = language;
    }

    public void setCollections(List<Collection> collections){
        this.collections = collections;
    }

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }

    public LocalDateTime getLastAccess() {
        return lastAccess;
    }

    public void setLastAccess(LocalDateTime lastAccess) {
        this.lastAccess = lastAccess;
    }

    public int getMonthlyTextCount() {
        return monthlyTextCount;
    }

    public void setMonthlyTextCount(int monthlyTextCount) {
        this.monthlyTextCount = monthlyTextCount;
    }

    public LocalDate getUsagePeriodStart() {
        return usagePeriodStart;
    }

    public void setUsagePeriodStart(LocalDate usagePeriodStart) {
        this.usagePeriodStart = usagePeriodStart;
    }

    public List<UserText> getUserTexts() {
        return userTexts;
    }

    public void setUserTexts(List<UserText> userTexts) {
        this.userTexts = userTexts;
    }
}