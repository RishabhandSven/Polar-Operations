package com.polarops.cargo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "cargo_consignments")
public class CargoConsignmentEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Column(name = "tracking_number", nullable = false, unique = true, length = 64)
    private String trackingNumber;

    @NotNull
    @Column(name = "destination_station_id", nullable = false)
    private UUID destinationStationId;

    @NotBlank
    @Column(name = "status", nullable = false, length = 32)
    private String status; // PLANNED, IN_TRANSIT, ARRIVED, RECEIVED, CANCELLED

    @Column(name = "departure_date")
    private LocalDate departureDate;

    @NotNull
    @Column(name = "estimated_arrival", nullable = false)
    private LocalDate estimatedArrival;

    @Column(name = "actual_arrival")
    private LocalDate actualArrival;

    public CargoConsignmentEntity() {
    }

    public CargoConsignmentEntity(UUID id, String trackingNumber, UUID destinationStationId,
                                  String status, LocalDate departureDate, LocalDate estimatedArrival,
                                  LocalDate actualArrival) {
        this.id = id != null ? id : UUID.randomUUID();
        this.trackingNumber = trackingNumber;
        this.destinationStationId = destinationStationId;
        this.status = status;
        this.departureDate = departureDate;
        this.estimatedArrival = estimatedArrival;
        this.actualArrival = actualArrival;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public UUID getDestinationStationId() {
        return destinationStationId;
    }

    public void setDestinationStationId(UUID destinationStationId) {
        this.destinationStationId = destinationStationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public LocalDate getEstimatedArrival() {
        return estimatedArrival;
    }

    public void setEstimatedArrival(LocalDate estimatedArrival) {
        this.estimatedArrival = estimatedArrival;
    }

    public LocalDate getActualArrival() {
        return actualArrival;
    }

    public void setActualArrival(LocalDate actualArrival) {
        this.actualArrival = actualArrival;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CargoConsignmentEntity that = (CargoConsignmentEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
