package com.chinesereads.backend.Model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Flashcard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;


    @ManyToOne
    @JoinColumn(name = "word_id")
    private Word word;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "text_id")
    private Text example;

    @ManyToOne
    @JoinColumn(name = "collection_id")
    private Collection collection;

    // ——— SRS (SM-2) ——— Wrapper types on purpose: cards created before this feature
    // hold NULL in these columns (ddl-auto=update only adds columns), and the service
    // treats NULL as "brand-new card" (0 reps, ease 2.5, due immediately). Primitives
    // would crash on load for those legacy rows.
    private Integer srsRepetitions;
    private Double srsEase;
    private Integer srsIntervalDays;
    private LocalDate srsDueDate;

    public Flashcard() {
    }

    public Flashcard(Word word, Text example, Collection collection){
        this.word = word;
        this.example = example;
        this.collection = collection;
    }

    public Flashcard(long id, Word word, Text example) {
        this.id = id;
        this.word = word;
        this.example = example;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Word getWord() {
        return word;
    }

    public void setWord(Word word) {
        this.word = word;
    }

    public Text getExample() {
        return example;
    }

    public void setExample(Text example) {
        this.example = example;
    }

    public Collection getCollection(){
        return collection;
    }

    public void setCollection(Collection collection){
        this.collection = collection;
    }

    public Integer getSrsRepetitions() {
        return srsRepetitions;
    }

    public void setSrsRepetitions(Integer srsRepetitions) {
        this.srsRepetitions = srsRepetitions;
    }

    public Double getSrsEase() {
        return srsEase;
    }

    public void setSrsEase(Double srsEase) {
        this.srsEase = srsEase;
    }

    public Integer getSrsIntervalDays() {
        return srsIntervalDays;
    }

    public void setSrsIntervalDays(Integer srsIntervalDays) {
        this.srsIntervalDays = srsIntervalDays;
    }

    public LocalDate getSrsDueDate() {
        return srsDueDate;
    }

    public void setSrsDueDate(LocalDate srsDueDate) {
        this.srsDueDate = srsDueDate;
    }
}
