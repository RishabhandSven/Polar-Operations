package com.polarops.emergency;

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
@Table(name = "alerts")
public class AlertEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "station_id", nullable = false)
    private UUID stationId;

    @NotBlank
    @Column(name = "type", nullable = false, length = 32)
    private String type; // SOS, WEATHER_WARNING, GEOFENCE_BREACH, SAFETY_STOCK_BREACH

    @NotBlank
    @Column(name = "severity", nullable = false, length = 32)
    private String severity; // CRITICAL, WARNING, INFO

    @NotBlank
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @Column(name = "resolved", nullable = false)
    private boolean resolved = false;

    public AlertEntity() {
    }

    public AlertEntity(UUID id, UUID stationId, String type, String severity, String message, Instant createdAt, boolean resolved) {
        this.id = id != null ? id : UUID.randomUUID();
        this.stationId = stationId;
        this.type = type;
        this.severity = severity;
        this.message = message;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.resolved = resolved;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getStationId() {
        return stationId;
    }

    public void setStationId(UUID stationId) {
        this.stationId = stationId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlertEntity that = (AlertEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
