package com.polarops.emergency;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class AlertDtos {

    private AlertDtos() {}

    public record AlertDto(
            UUID id,
            UUID stationId,
            String type,
            String severity,
            String message,
            Instant createdAt,
            boolean resolved
    ) {}

    public record CreateAlertRequest(
            @NotNull(message = "stationId is required")
            UUID stationId,

            @NotBlank(message = "type is required")
            String type,

            @NotBlank(message = "severity is required")
            String severity,

            @NotBlank(message = "message is required")
            String message
    ) {}
}
