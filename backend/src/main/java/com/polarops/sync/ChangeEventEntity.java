package com.polarops.sync;

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
@Table(name = "change_events")
public class ChangeEventEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "client_transaction_id", nullable = false, unique = true)
    private UUID clientTransactionId;

    @NotNull
    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @NotBlank
    @Column(name = "operation", nullable = false, length = 64)
    private String operation;

    @NotBlank
    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @NotNull
    @Column(name = "server_timestamp", nullable = false)
    private Instant serverTimestamp;

    @NotBlank
    @Column(name = "status", nullable = false, length = 32)
    private String status; // COMMITTED, REJECTED

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public ChangeEventEntity() {
    }

    public ChangeEventEntity(UUID id, UUID clientTransactionId, UUID deviceId, String operation,
                             String payloadJson, Instant serverTimestamp, String status, String errorMessage) {
        this.id = id != null ? id : UUID.randomUUID();
        this.clientTransactionId = clientTransactionId;
        this.deviceId = deviceId;
        this.operation = operation;
        this.payloadJson = payloadJson;
        this.serverTimestamp = serverTimestamp != null ? serverTimestamp : Instant.now();
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getClientTransactionId() {
        return clientTransactionId;
    }

    public void setClientTransactionId(UUID clientTransactionId) {
        this.clientTransactionId = clientTransactionId;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public Instant getServerTimestamp() {
        return serverTimestamp;
    }

    public void setServerTimestamp(Instant serverTimestamp) {
        this.serverTimestamp = serverTimestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChangeEventEntity that = (ChangeEventEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
