package com.chinesereads.backend.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Word {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long id;
    public String chinese;
    public String pinyin;
    public String english;
    public String spanish;

    public Word(){}

    public Word(String chinese, String pinyin, String english, String spanish){
        this.chinese = chinese;
        this.pinyin = pinyin;
        this.english = english;
        this.spanish = spanish;
    }

    public long getId() {
        return id;
    }

    public String getChinese() {
        return chinese;
    }

    public String getPinyin() {
        return pinyin;
    }

    public String getEnglish() {
        return english;
    }

    public String getSpanish() {
        return spanish;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setChinese(String chinese) {
        this.chinese = chinese;
    }

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }

    public void setEnglish(String english) {
        this.english = english;
    }

    public void setSpanish(String spanish) {
        this.spanish = spanish;
    }

    public void copy(Word word){
        this.chinese = word.chinese;
        this.english = word.english;
        this.spanish = word.spanish;
    }

}
