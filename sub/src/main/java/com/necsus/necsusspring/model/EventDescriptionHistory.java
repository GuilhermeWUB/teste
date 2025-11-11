package com.necsus.necsusspring.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_description_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDescriptionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "previous_description", columnDefinition = "TEXT")
    private String previousDescription;

    @Column(name = "new_description", columnDefinition = "TEXT")
    private String newDescription;

    @Column(name = "modified_by", length = 100)
    private String modifiedBy;

    @Column(name = "modified_at", nullable = false)
    private LocalDateTime modifiedAt;

    public EventDescriptionHistory(Event event, String previousDescription, String newDescription, String modifiedBy) {
        this.event = event;
        this.previousDescription = previousDescription;
        this.newDescription = newDescription;
        this.modifiedBy = modifiedBy;
        this.modifiedAt = LocalDateTime.now();
    }
}
