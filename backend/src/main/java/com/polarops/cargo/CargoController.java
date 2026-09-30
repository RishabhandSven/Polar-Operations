package com.polarops.cargo;

import com.polarops.cargo.CargoDtos.AddCargoItemRequest;
import com.polarops.cargo.CargoDtos.CargoConsignmentDto;
import com.polarops.cargo.CargoDtos.CargoItemDto;
import com.polarops.cargo.CargoDtos.CreateCargoConsignmentRequest;
import com.polarops.cargo.CargoDtos.ReceiveCargoRequest;
import com.polarops.cargo.CargoDtos.ReceiveCargoResponse;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cargo")
public class CargoController {

    private final CargoService cargoService;

    public CargoController(CargoService cargoService) {
        this.cargoService = cargoService;
    }

    @PostMapping
    public ResponseEntity<CargoConsignmentDto> createConsignment(@Valid @RequestBody CreateCargoConsignmentRequest req) {
        CargoConsignmentDto created = cargoService.createConsignment(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<CargoConsignmentDto>> listConsignments(
            @RequestParam(name = "stationId", required = false) UUID stationId
    ) {
        return ResponseEntity.ok(cargoService.listConsignments(stationId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CargoConsignmentDto> getConsignment(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(cargoService.getConsignment(id));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<CargoItemDto> addCargoItem(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AddCargoItemRequest req
    ) {
        CargoItemDto created = cargoService.addCargoItem(id, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<ReceiveCargoResponse> receiveConsignment(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) ReceiveCargoRequest req
    ) {
        String operatorId = req != null ? req.operatorId() : "CARGO-OPERATOR";
        ReceiveCargoResponse response = cargoService.receiveConsignment(id, operatorId);
        return ResponseEntity.ok(response);
    }
}
