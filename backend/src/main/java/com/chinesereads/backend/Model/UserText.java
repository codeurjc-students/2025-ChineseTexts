package com.chinesereads.backend.Model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

/**
 * A Chinese text uploaded (via OCR or pasted) by a registered user. Unlike the
 * official {@link Text}, it is PRIVATE to its owner and fully self-contained: it
 * stores its own segmented word list ({@link UserTextWord}) with pinyin and
 * translations, so rendering never touches — nor pollutes — the global {@link Word}
 * dictionary.
 */
@Entity
public class UserText {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    private String title;

    @Lob
    private String text;

    @Lob
    private String englishTranslation;

    @Lob
    private String spanishTranslation;

    private LocalDate creationDate;

    @OneToMany(mappedBy = "userText", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, id ASC")
    private List<UserTextWord> words = new ArrayList<>();

    public UserText() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getEnglishTranslation() { return englishTranslation; }
    public void setEnglishTranslation(String englishTranslation) { this.englishTranslation = englishTranslation; }

    public String getSpanishTranslation() { return spanishTranslation; }
    public void setSpanishTranslation(String spanishTranslation) { this.spanishTranslation = spanishTranslation; }

    public LocalDate getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDate creationDate) { this.creationDate = creationDate; }

    public List<UserTextWord> getWords() { return words; }
    public void setWords(List<UserTextWord> words) { this.words = words; }

    /** Adds a word and keeps the bidirectional link consistent. */
    public void addWord(UserTextWord word) {
        word.setUserText(this);
        this.words.add(word);
    }
}
