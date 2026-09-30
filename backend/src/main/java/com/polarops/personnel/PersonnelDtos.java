package com.polarops.personnel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class PersonnelDtos {

    private PersonnelDtos() {}

    public record PersonnelDto(
            UUID id,
            UUID stationId,
            String name,
            String role,
            String status
    ) {}

    public record CreatePersonnelRequest(
            @NotNull(message = "stationId is required")
            UUID stationId,

            @NotBlank(message = "name is required")
            String name,

            @NotBlank(message = "role is required")
            String role,

            String status
    ) {}

    public record PersonnelMovementDto(
            UUID id,
            UUID personnelId,
            String type,
            String destination,
            Instant timestamp,
            String notes
    ) {}

    public record CreateMovementRequest(
            @NotBlank(message = "type is required")
            String type,

            String destination,
            String notes
    ) {}
}
