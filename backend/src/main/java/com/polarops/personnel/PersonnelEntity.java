package com.polarops.personnel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "personnel")
public class PersonnelEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "station_id", nullable = false)
    private UUID stationId;

    @NotBlank
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @NotBlank
    @Column(name = "role", nullable = false, length = 64)
    private String role; // COMMANDER, SCIENTIST, MEDICAL_OFFICER, ENGINEER, LOGISTICS

    @NotBlank
    @Column(name = "status", nullable = false, length = 32)
    private String status = "ON_STATION"; // ON_STATION, FIELD_SORTIE, EVACUATED

    public PersonnelEntity() {
    }

    public PersonnelEntity(UUID id, UUID stationId, String name, String role, String status) {
        this.id = id != null ? id : UUID.randomUUID();
        this.stationId = stationId;
        this.name = name;
        this.role = role;
        this.status = status != null ? status : "ON_STATION";
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersonnelEntity that = (PersonnelEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
