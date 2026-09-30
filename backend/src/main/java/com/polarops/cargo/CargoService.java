package com.polarops.cargo;

import com.polarops.cargo.CargoDtos.AddCargoItemRequest;
import com.polarops.cargo.CargoDtos.CargoConsignmentDto;
import com.polarops.cargo.CargoDtos.CargoItemDto;
import com.polarops.cargo.CargoDtos.CreateCargoConsignmentRequest;
import com.polarops.cargo.CargoDtos.ReceiveCargoResponse;
import com.polarops.common.DomainException;
import com.polarops.common.ResourceNotFoundException;
import com.polarops.expedition.StationEntity;
import com.polarops.expedition.StationRepository;
import com.polarops.inventory.InventoryDtos.CreateTransactionRequest;
import com.polarops.inventory.InventoryItemEntity;
import com.polarops.inventory.InventoryItemRepository;
import com.polarops.inventory.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CargoService {

    private final CargoConsignmentRepository consignmentRepository;
    private final CargoItemRepository cargoItemRepository;
    private final StationRepository stationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryService inventoryService;

    public CargoService(CargoConsignmentRepository consignmentRepository,
                        CargoItemRepository cargoItemRepository,
                        StationRepository stationRepository,
                        InventoryItemRepository inventoryItemRepository,
                        InventoryService inventoryService) {
        this.consignmentRepository = consignmentRepository;
        this.cargoItemRepository = cargoItemRepository;
        this.stationRepository = stationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public CargoConsignmentDto createConsignment(CreateCargoConsignmentRequest req) {
        String tracking = req.trackingNumber().trim().toUpperCase();
        if (consignmentRepository.findByTrackingNumber(tracking).isPresent()) {
            throw new DomainException("Cargo consignment with tracking number '" + tracking + "' already exists");
        }

        StationEntity station = stationRepository.findById(req.destinationStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination station not found with id: " + req.destinationStationId()));

        String status = req.status() != null && !req.status().isBlank()
                ? req.status().trim().toUpperCase()
                : "PLANNED";

        CargoConsignmentEntity consignment = new CargoConsignmentEntity(
                UUID.randomUUID(),
                tracking,
                req.destinationStationId(),
                status,
                req.departureDate(),
                req.estimatedArrival(),
                status.equals("RECEIVED") ? LocalDate.now() : null
        );

        CargoConsignmentEntity saved = consignmentRepository.save(consignment);
        List<CargoItemDto> items = new ArrayList<>();

        if (req.initialItems() != null && !req.initialItems().isEmpty()) {
            for (AddCargoItemRequest itemReq : req.initialItems()) {
                CargoItemDto itemDto = addCargoItemInternal(saved.getId(), itemReq);
                items.add(itemDto);
            }
        }

        return mapToConsignmentDto(saved, station.getName(), items);
    }

    @Transactional(readOnly = true)
    public List<CargoConsignmentDto> listConsignments(UUID stationId) {
        List<CargoConsignmentEntity> consignments = stationId != null
                ? consignmentRepository.findByDestinationStationId(stationId)
                : consignmentRepository.findAll();

        if (consignments.isEmpty()) {
            return List.of();
        }

        Map<UUID, StationEntity> stationMap = stationRepository.findAll().stream()
                .collect(Collectors.toMap(StationEntity::getId, Function.identity()));

        Map<UUID, InventoryItemEntity> itemMap = inventoryItemRepository.findAll().stream()
                .collect(Collectors.toMap(InventoryItemEntity::getId, Function.identity()));

        return consignments.stream()
                .map(c -> {
                    StationEntity st = stationMap.get(c.getDestinationStationId());
                    String stName = st != null ? st.getName() : "UNKNOWN";
                    List<CargoItemEntity> items = cargoItemRepository.findByConsignmentId(c.getId());
                    List<CargoItemDto> itemDtos = items.stream()
                            .map(item -> mapToItemDto(item, itemMap.get(item.getInventoryItemId())))
                            .toList();
                    return mapToConsignmentDto(c, stName, itemDtos);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public CargoConsignmentDto getConsignment(UUID id) {
        CargoConsignmentEntity consignment = consignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cargo consignment not found with id: " + id));

        StationEntity station = stationRepository.findById(consignment.getDestinationStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + consignment.getDestinationStationId()));

        List<CargoItemEntity> items = cargoItemRepository.findByConsignmentId(id);
        Map<UUID, InventoryItemEntity> itemMap = inventoryItemRepository.findAllById(
                items.stream().map(CargoItemEntity::getInventoryItemId).toList()
        ).stream().collect(Collectors.toMap(InventoryItemEntity::getId, Function.identity()));

        List<CargoItemDto> itemDtos = items.stream()
                .map(item -> mapToItemDto(item, itemMap.get(item.getInventoryItemId())))
                .toList();

        return mapToConsignmentDto(consignment, station.getName(), itemDtos);
    }

    @Transactional
    public CargoItemDto addCargoItem(UUID consignmentId, AddCargoItemRequest req) {
        CargoConsignmentEntity consignment = consignmentRepository.findById(consignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Cargo consignment not found with id: " + consignmentId));

        if ("RECEIVED".equalsIgnoreCase(consignment.getStatus()) || "CANCELLED".equalsIgnoreCase(consignment.getStatus())) {
            throw new DomainException("Cannot add items to consignment in status: " + consignment.getStatus());
        }

        return addCargoItemInternal(consignmentId, req);
    }

    private CargoItemDto addCargoItemInternal(UUID consignmentId, AddCargoItemRequest req) {
        InventoryItemEntity item = inventoryItemRepository.findById(req.inventoryItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found with id: " + req.inventoryItemId()));

        if (req.quantity() == null || req.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Cargo item quantity must be strictly greater than 0");
        }

        CargoItemEntity entity = new CargoItemEntity(
                UUID.randomUUID(),
                consignmentId,
                req.inventoryItemId(),
                req.quantity()
        );

        CargoItemEntity saved = cargoItemRepository.save(entity);
        return mapToItemDto(saved, item);
    }

    @Transactional
    public ReceiveCargoResponse receiveConsignment(UUID consignmentId, String operatorId) {
        CargoConsignmentEntity consignment = consignmentRepository.findById(consignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Cargo consignment not found with id: " + consignmentId));

        String previousStatus = consignment.getStatus().toUpperCase();

        if ("RECEIVED".equals(previousStatus)) {
            throw new DomainException("Consignment has already been received: " + consignment.getTrackingNumber());
        }

        if (!"ARRIVED".equals(previousStatus)) {
            throw new DomainException("Consignment cannot be received in status: " + consignment.getStatus() + ". Must be ARRIVED.");
        }

        List<CargoItemEntity> items = cargoItemRepository.findByConsignmentId(consignmentId);
        if (items.isEmpty()) {
            throw new DomainException("Cannot receive consignment with no cargo items: " + consignment.getTrackingNumber());
        }

        StationEntity station = stationRepository.findById(consignment.getDestinationStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + consignment.getDestinationStationId()));

        String opId = operatorId != null && !operatorId.isBlank() ? operatorId.trim() : "CARGO-RECEIVER";
        BigDecimal totalQty = BigDecimal.ZERO;

        // Atomically record InventoryTransaction (RECEIPT) for each cargo item
        for (CargoItemEntity item : items) {
            CreateTransactionRequest txReq = new CreateTransactionRequest(
                    consignment.getDestinationStationId(),
                    item.getInventoryItemId(),
                    "RECEIPT",
                    item.getQuantity(),
                    "CARGO_RECEIPT",
                    consignment.getId().toString(),
                    opId
            );

            inventoryService.recordTransaction(txReq);
            totalQty = totalQty.add(item.getQuantity());
        }

        // Transition consignment status: ARRIVED -> RECEIVED
        consignment.setStatus("RECEIVED");
        consignment.setActualArrival(LocalDate.now());
        consignmentRepository.save(consignment);

        return new ReceiveCargoResponse(
                consignment.getId(),
                consignment.getTrackingNumber(),
                previousStatus,
                "RECEIVED",
                station.getId(),
                station.getName(),
                items.size(),
                totalQty,
                Instant.now()
        );
    }

    private CargoConsignmentDto mapToConsignmentDto(CargoConsignmentEntity c, String stationName, List<CargoItemDto> items) {
        return new CargoConsignmentDto(
                c.getId(),
                c.getTrackingNumber(),
                c.getDestinationStationId(),
                stationName,
                c.getStatus(),
                c.getDepartureDate(),
                c.getEstimatedArrival(),
                c.getActualArrival(),
                items
        );
    }

    private CargoItemDto mapToItemDto(CargoItemEntity item, InventoryItemEntity invItem) {
        String itemCode = invItem != null ? invItem.getItemCode() : "UNKNOWN";
        String itemName = invItem != null ? invItem.getName() : "UNKNOWN";
        String category = invItem != null ? invItem.getCategory() : "UNKNOWN";
        String unit = invItem != null ? invItem.getUnit() : "UNITS";

        return new CargoItemDto(
                item.getId(),
                item.getConsignmentId(),
                item.getInventoryItemId(),
                itemCode,
                itemName,
                category,
                unit,
                item.getQuantity()
        );
    }
}
