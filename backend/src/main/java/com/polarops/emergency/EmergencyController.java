package com.polarops.emergency;

import com.polarops.common.ResourceNotFoundException;
import com.polarops.emergency.AlertDtos.AlertDto;
import com.polarops.emergency.AlertDtos.CreateAlertRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/emergency")
public class EmergencyController {

    private final AlertRepository alertRepository;

    public EmergencyController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<AlertDto>> listAlerts(
            @RequestParam(name = "stationId", required = false) UUID stationId,
            @RequestParam(name = "resolved", required = false) Boolean resolved
    ) {
        List<AlertEntity> list;
        if (stationId != null) {
            if (resolved != null) {
                list = alertRepository.findByStationIdAndResolvedOrderByCreatedAtDesc(stationId, resolved);
            } else {
                list = alertRepository.findByStationIdOrderByCreatedAtDesc(stationId);
            }
        } else {
            list = alertRepository.findAll();
        }

        return ResponseEntity.ok(list.stream()
                .map(a -> new AlertDto(a.getId(), a.getStationId(), a.getType(), a.getSeverity(), a.getMessage(), a.getCreatedAt(), a.isResolved()))
                .toList());
    }

    @PostMapping("/alerts")
    public ResponseEntity<AlertDto> createAlert(@Valid @RequestBody CreateAlertRequest req) {
        AlertEntity entity = new AlertEntity(
                UUID.randomUUID(),
                req.stationId(),
                req.type().trim().toUpperCase(),
                req.severity().trim().toUpperCase(),
                req.message().trim(),
                Instant.now(),
                false
        );
        AlertEntity saved = alertRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AlertDto(saved.getId(), saved.getStationId(), saved.getType(), saved.getSeverity(), saved.getMessage(), saved.getCreatedAt(), saved.isResolved()));
    }

    @PostMapping("/alerts/{id}/resolve")
    public ResponseEntity<AlertDto> resolveAlert(@PathVariable("id") UUID id) {
        AlertEntity alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + id));

        alert.setResolved(true);
        AlertEntity saved = alertRepository.save(alert);
        return ResponseEntity.ok(new AlertDto(saved.getId(), saved.getStationId(), saved.getType(), saved.getSeverity(), saved.getMessage(), saved.getCreatedAt(), saved.isResolved()));
    }
}
