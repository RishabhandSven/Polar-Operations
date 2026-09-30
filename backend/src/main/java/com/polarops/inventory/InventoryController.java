package com.polarops.inventory;

import com.polarops.inventory.InventoryDtos.CreateInventoryItemRequest;
import com.polarops.inventory.InventoryDtos.CreateStockLevelRequest;
import com.polarops.inventory.InventoryDtos.CreateTransactionRequest;
import com.polarops.inventory.InventoryDtos.InventoryItemDto;
import com.polarops.inventory.InventoryDtos.StationIntelligenceReport;
import com.polarops.inventory.InventoryDtos.StockLevelDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@Validated
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryIntelligenceService intelligenceService;

    public InventoryController(InventoryService inventoryService,
                               InventoryIntelligenceService intelligenceService) {
        this.inventoryService = inventoryService;
        this.intelligenceService = intelligenceService;
    }

    @GetMapping("/items")
    public ResponseEntity<List<InventoryItemDto>> getAllItems() {
        return ResponseEntity.ok(inventoryService.getAllItems());
    }

    @PostMapping("/items")
    public ResponseEntity<InventoryItemDto> createItem(@Valid @RequestBody CreateInventoryItemRequest req) {
        InventoryItemDto created = inventoryService.createItem(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<StockLevelDto>> getStationStockLevels(@RequestParam("stationId") UUID stationId) {
        return ResponseEntity.ok(inventoryService.getStationStockLevels(stationId));
    }

    @PostMapping("/stock-levels")
    public ResponseEntity<StockLevelDto> createStockLevel(@Valid @RequestBody CreateStockLevelRequest req) {
        StockLevelDto created = inventoryService.createStockLevel(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/transactions")
    public ResponseEntity<StockLevelDto> recordTransaction(@Valid @RequestBody CreateTransactionRequest req) {
        StockLevelDto updated = inventoryService.recordTransaction(req);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/intelligence")
    public ResponseEntity<StationIntelligenceReport> getStationIntelligence(
            @RequestParam("stationId") UUID stationId,
            @RequestParam(name = "delayDays", defaultValue = "0") @Min(value = 0, message = "delayDays must be >= 0") int delayDays
    ) {
        StationIntelligenceReport report = intelligenceService.evaluateStationIntelligence(stationId, delayDays);
        return ResponseEntity.ok(report);
    }
}
