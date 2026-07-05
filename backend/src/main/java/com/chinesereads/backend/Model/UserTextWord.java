package com.chinesereads.backend.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * One segment of a {@link UserText}, in reading order, with its pinyin and
 * translations. These belong ONLY to the owning user's text — they are never
 * written to the shared {@link Word} dictionary. Punctuation / unknown segments
 * keep empty pinyin/translations and render as-is, matching the public reader.
 */
@Entity
public class UserTextWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "user_text_id", nullable = false)
    private UserText userText;

    /** Position of this segment within the text (0-based). */
    private int position;

    private String chinese;
    private String pinyin;
    private String english;
    private String spanish;

    /** True if a line break follows this word (preserves the original layout). */
    private boolean newlineAfter;

    public UserTextWord() {}

    public UserTextWord(int position, String chinese, String pinyin, String english, String spanish) {
        this.position = position;
        this.chinese = chinese;
        this.pinyin = pinyin;
        this.english = english;
        this.spanish = spanish;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public UserText getUserText() { return userText; }
    public void setUserText(UserText userText) { this.userText = userText; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public String getChinese() { return chinese; }
    public void setChinese(String chinese) { this.chinese = chinese; }

    public String getPinyin() { return pinyin; }
    public void setPinyin(String pinyin) { this.pinyin = pinyin; }

    public String getEnglish() { return english; }
    public void setEnglish(String english) { this.english = english; }

    public String getSpanish() { return spanish; }
    public void setSpanish(String spanish) { this.spanish = spanish; }

    public boolean isNewlineAfter() { return newlineAfter; }
    public void setNewlineAfter(boolean newlineAfter) { this.newlineAfter = newlineAfter; }
}
