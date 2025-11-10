package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_observation_history")
public class EventObservationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "previous_observation", columnDefinition = "TEXT")
    private String previousObservation;

    @Column(name = "new_observation", columnDefinition = "TEXT")
    private String newObservation;

    @Column(name = "modified_by", length = 100)
    private String modifiedBy;

    @Column(name = "modified_at", nullable = false)
    private LocalDateTime modifiedAt;

    public EventObservationHistory() {
        this.modifiedAt = LocalDateTime.now();
    }

    public EventObservationHistory(Event event, String previousObservation, String newObservation, String modifiedBy) {
        this.event = event;
        this.previousObservation = previousObservation;
        this.newObservation = newObservation;
        this.modifiedBy = modifiedBy;
        this.modifiedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getPreviousObservation() {
        return previousObservation;
    }

    public void setPreviousObservation(String previousObservation) {
        this.previousObservation = previousObservation;
    }

    public String getNewObservation() {
        return newObservation;
    }

    public void setNewObservation(String newObservation) {
        this.newObservation = newObservation;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(LocalDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }
}
