package com.polarops.sync;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public final class SyncDtos {

    private SyncDtos() {}

    public record SyncOperationRequest(
            @NotNull(message = "clientTransactionId is required")
            UUID clientTransactionId,

            @NotNull(message = "deviceId is required")
            UUID deviceId,

            @NotBlank(message = "operation is required")
            String operation,

            @NotNull(message = "payload is required")
            JsonNode payload
    ) {}

    public record SyncBatchRequest(
            @NotEmpty(message = "operations batch cannot be empty")
            @Valid
            List<SyncOperationRequest> operations
    ) {}

    public record SyncOperationResult(
            UUID clientTransactionId,
            String status, // APPLIED, DUPLICATE, REJECTED
            String message
    ) {
        public static SyncOperationResult applied(UUID clientTransactionId) {
            return new SyncOperationResult(clientTransactionId, "APPLIED", "Operation successfully processed and recorded");
        }

        public static SyncOperationResult duplicate(UUID clientTransactionId) {
            return new SyncOperationResult(clientTransactionId, "DUPLICATE", "Operation already processed previously");
        }

        public static SyncOperationResult rejected(UUID clientTransactionId, String reason) {
            return new SyncOperationResult(clientTransactionId, "REJECTED", reason);
        }
    }

    public record SyncBatchResponse(
            List<SyncOperationResult> results
    ) {}
}
