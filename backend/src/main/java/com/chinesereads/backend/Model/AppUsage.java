package com.chinesereads.backend.Model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * One row per calendar day counting the number of AI/OCR text generations across
 * ALL users that day. Backs the global daily "kill-switch": once the count reaches
 * the configured limit, generation is paused until the next day — bounding the
 * maximum possible API spend regardless of traffic or abuse.
 */
@Entity
public class AppUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true)
    private LocalDate day;

    private int count;

    public AppUsage() {}

    public AppUsage(LocalDate day, int count) {
        this.day = day;
        this.count = count;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public LocalDate getDay() { return day; }
    public void setDay(LocalDate day) { this.day = day; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
