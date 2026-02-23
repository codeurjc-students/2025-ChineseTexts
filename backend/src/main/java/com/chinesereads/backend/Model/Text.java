package com.chinesereads.backend.Model;

import java.sql.Blob;
import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;

@Entity
public class Text {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String titleEnglish;
    private String titleSpanish;

    @Lob
    private String text;
    @Lob
    private String spanishTranslation;
    @Lob
    private String englishTranslation;
    
    private String level;

    @Lob
    private String englishDescription;
    @Lob
    private String spanishDescription;

    @OneToMany(mappedBy = "example", cascade = CascadeType.REMOVE)  // Elimina las flashcards si se borra el texto
    private List<Flashcard> flashcards;

    private LocalDate creationDate;

    @Lob
    private Blob image;

    public Text(){}

    public Text(String titleEnglish, String titleSpanish, String text, String englishTranslation, String spanishTranslation, String englishDescription, String spanishDescription, String level, LocalDate creationDate, Blob image){
        this.titleEnglish = titleEnglish;
        this.titleSpanish = titleSpanish;
        this.text = text;
        this.englishTranslation = englishTranslation;
        this.spanishTranslation = spanishTranslation;
        this.englishDescription = englishDescription;
        this.spanishDescription = spanishDescription;
        this.level = level;
        this.creationDate = creationDate;
        this.image = image;
    }

    public long getId(){
        return this.id;
    }

    public String getTitleEnglish(){
        return this.titleEnglish;
    }

    public String getTitleSpanish(){
        return this.titleSpanish;
    }

    public String getText(){
        return this.text;
    }

    public String getEnglishTranslation(){
        return this.englishTranslation;
    }

    public String getSpanishTranslation(){
        return this.spanishTranslation;
    }

    public String getEnglishDescription() {
        return this.englishDescription;
    }

    public String getSpanishDescription() {
        return this.spanishDescription;
    }

    public String getLevel(){
        return this.level;
    }

    public LocalDate getCreationDate() {
        return this.creationDate;
    }

    public Blob getImage() {
        return this.image;
    }

    public void setId(long id){
        this.id = id;
    }

    public void setTitleEnglish(String titleEnglish){
        this.titleEnglish = titleEnglish;
    }

    public void setTitleSpanish(String titleSpanish){
        this.titleSpanish = titleSpanish;
    }

    public void setText(String text){
        this.text = text;
    }
    
    public void setEnglishTranslation(String englishTranslation){
        this.englishTranslation = englishTranslation;
    }

    public void setSpanishTranslation(String spanishTranslation){
        this.spanishTranslation = spanishTranslation;
    }

    public void setEnglishDescription(String englishDescription){
        this.englishDescription = englishDescription;
    }

    public void setSpanishDescription(String spanishDescription){
        this.spanishDescription = englishTranslation;
    }

    public void setLevel(String level){
        this.level = level;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public void setImage(Blob image) {
        this.image = image;
    }
}
