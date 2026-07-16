package com.chinesereads.backend.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;

/**
 * One sentence of a public {@link Text}, in reading order, with its English and
 * Spanish translation aligned 1:1 — the exact same design as
 * {@link UserTextSentence} in the private flow. Sentence boundaries are decided
 * ONCE at upload time (and validated against the translations), so the reader
 * never has to re-derive the pairing heuristically. Texts created before this
 * existed simply have no rows: the reader falls back to the old behaviour.
 */
@Entity
public class TextSentence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "text_id", nullable = false)
    private Text text;

    /** Position of this sentence within the text (0-based). */
    private int position;

    @Lob
    private String chinese;

    @Lob
    private String english;

    @Lob
    private String spanish;

    public TextSentence() {}

    public TextSentence(int position, String chinese, String english, String spanish) {
        this.position = position;
        this.chinese = chinese;
        this.english = english;
        this.spanish = spanish;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Text getText() { return text; }
    public void setText(Text text) { this.text = text; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public String getChinese() { return chinese; }
    public void setChinese(String chinese) { this.chinese = chinese; }

    public String getEnglish() { return english; }
    public void setEnglish(String english) { this.english = english; }

    public String getSpanish() { return spanish; }
    public void setSpanish(String spanish) { this.spanish = spanish; }
}
