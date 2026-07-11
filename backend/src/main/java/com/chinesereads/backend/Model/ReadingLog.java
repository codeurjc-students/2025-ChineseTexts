package com.chinesereads.backend.Model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One row per user, per text, per day: the raw record behind the reading streak and
 * the progress statistics. The unique constraint makes logging idempotent — reloading
 * the same text on the same day never inflates the stats.
 *
 * <p>{@code textKey} identifies what was read without foreign keys ("public:12" for a
 * catalog text, "own:34" for a private one), so logs survive text deletion and both
 * readers share one mechanism.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "day", "textKey" }))
public class ReadingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate day;

    private String textKey;

    public ReadingLog() {
    }

    public ReadingLog(User user, LocalDate day, String textKey) {
        this.user = user;
        this.day = day;
        this.textKey = textKey;
    }

    public long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LocalDate getDay() {
        return day;
    }

    public String getTextKey() {
        return textKey;
    }
}
