package com.polarops.personnel;

import com.polarops.common.ResourceNotFoundException;
import com.polarops.personnel.PersonnelDtos.CreateMovementRequest;
import com.polarops.personnel.PersonnelDtos.CreatePersonnelRequest;
import com.polarops.personnel.PersonnelDtos.PersonnelDto;
import com.polarops.personnel.PersonnelDtos.PersonnelMovementDto;
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
@RequestMapping("/api/personnel")
public class PersonnelController {

    private final PersonnelRepository personnelRepository;
    private final PersonnelMovementRepository movementRepository;

    public PersonnelController(PersonnelRepository personnelRepository,
                               PersonnelMovementRepository movementRepository) {
        this.personnelRepository = personnelRepository;
        this.movementRepository = movementRepository;
    }

    @GetMapping
    public ResponseEntity<List<PersonnelDto>> listPersonnel(
            @RequestParam(name = "stationId", required = false) UUID stationId
    ) {
        List<PersonnelEntity> entities = stationId != null
                ? personnelRepository.findByStationId(stationId)
                : personnelRepository.findAll();

        return ResponseEntity.ok(entities.stream()
                .map(p -> new PersonnelDto(p.getId(), p.getStationId(), p.getName(), p.getRole(), p.getStatus()))
                .toList());
    }

    @PostMapping
    public ResponseEntity<PersonnelDto> createPersonnel(@Valid @RequestBody CreatePersonnelRequest req) {
        PersonnelEntity entity = new PersonnelEntity(
                UUID.randomUUID(),
                req.stationId(),
                req.name().trim(),
                req.role().trim().toUpperCase(),
                req.status() != null ? req.status().trim().toUpperCase() : "ON_STATION"
        );
        PersonnelEntity saved = personnelRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new PersonnelDto(saved.getId(), saved.getStationId(), saved.getName(), saved.getRole(), saved.getStatus()));
    }

    @GetMapping("/{id}/movements")
    public ResponseEntity<List<PersonnelMovementDto>> listMovements(@PathVariable("id") UUID id) {
        if (!personnelRepository.existsById(id)) {
            throw new ResourceNotFoundException("Personnel not found with id: " + id);
        }
        return ResponseEntity.ok(movementRepository.findByPersonnelIdOrderByTimestampDesc(id).stream()
                .map(m -> new PersonnelMovementDto(m.getId(), m.getPersonnelId(), m.getType(), m.getDestination(), m.getTimestamp(), m.getNotes()))
                .toList());
    }

    @PostMapping("/{id}/movements")
    public ResponseEntity<PersonnelMovementDto> recordMovement(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreateMovementRequest req
    ) {
        PersonnelEntity person = personnelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personnel not found with id: " + id));

        PersonnelMovementEntity movement = new PersonnelMovementEntity(
                UUID.randomUUID(),
                id,
                req.type().trim().toUpperCase(),
                req.destination(),
                Instant.now(),
                req.notes()
        );
        PersonnelMovementEntity saved = movementRepository.save(movement);

        // Update personnel status if sortie or check-in
        if ("SORTIE_DEPARTURE".equalsIgnoreCase(req.type())) {
            person.setStatus("FIELD_SORTIE");
            personnelRepository.save(person);
        } else if ("SORTIE_RETURN".equalsIgnoreCase(req.type()) || "CHECK_IN".equalsIgnoreCase(req.type())) {
            person.setStatus("ON_STATION");
            personnelRepository.save(person);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new PersonnelMovementDto(saved.getId(), saved.getPersonnelId(), saved.getType(), saved.getDestination(), saved.getTimestamp(), saved.getNotes()));
    }
}
