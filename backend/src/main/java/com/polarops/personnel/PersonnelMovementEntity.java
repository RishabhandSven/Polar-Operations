package com.polarops.personnel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "personnel_movements")
public class PersonnelMovementEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "personnel_id", nullable = false)
    private UUID personnelId;

    @NotBlank
    @Column(name = "type", nullable = false, length = 32)
    private String type; // CHECK_IN, CHECK_OUT, SORTIE_DEPARTURE, SORTIE_RETURN

    @Column(name = "destination", length = 128)
    private String destination;

    @NotNull
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public PersonnelMovementEntity() {
    }

    public PersonnelMovementEntity(UUID id, UUID personnelId, String type, String destination,
                                   Instant timestamp, String notes) {
        this.id = id != null ? id : UUID.randomUUID();
        this.personnelId = personnelId;
        this.type = type;
        this.destination = destination;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.notes = notes;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPersonnelId() {
        return personnelId;
    }

    public void setPersonnelId(UUID personnelId) {
        this.personnelId = personnelId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersonnelMovementEntity that = (PersonnelMovementEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
