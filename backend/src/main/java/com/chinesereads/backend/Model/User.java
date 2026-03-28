package com.chinesereads.backend.Model;

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

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Collection> collections = new ArrayList<>();
    
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
}