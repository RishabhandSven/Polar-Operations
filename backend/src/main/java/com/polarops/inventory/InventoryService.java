package com.polarops.inventory;

import com.polarops.common.DomainException;
import com.polarops.common.ResourceNotFoundException;
import com.polarops.expedition.StationEntity;
import com.polarops.expedition.StationRepository;
import com.polarops.inventory.InventoryDtos.CreateInventoryItemRequest;
import com.polarops.inventory.InventoryDtos.CreateStockLevelRequest;
import com.polarops.inventory.InventoryDtos.CreateTransactionRequest;
import com.polarops.inventory.InventoryDtos.InventoryItemDto;
import com.polarops.inventory.InventoryDtos.StockLevelDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InventoryItemRepository itemRepository;
    private final StockLevelRepository stockLevelRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final StationRepository stationRepository;

    public InventoryService(InventoryItemRepository itemRepository,
                            StockLevelRepository stockLevelRepository,
                            InventoryTransactionRepository transactionRepository,
                            StationRepository stationRepository) {
        this.itemRepository = itemRepository;
        this.stockLevelRepository = stockLevelRepository;
        this.transactionRepository = transactionRepository;
        this.stationRepository = stationRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryItemDto> getAllItems() {
        return itemRepository.findAll().stream()
                .map(this::mapToItemDto)
                .toList();
    }

    @Transactional
    public InventoryItemDto createItem(CreateInventoryItemRequest req) {
        String normalizedCode = req.itemCode().trim().toUpperCase();
        if (itemRepository.findByItemCode(normalizedCode).isPresent()) {
            throw new DomainException("Inventory item with code '" + normalizedCode + "' already exists");
        }

        InventoryItemEntity entity = new InventoryItemEntity(
                UUID.randomUUID(),
                normalizedCode,
                req.name().trim(),
                req.category().trim().toUpperCase(),
                req.unit().trim().toUpperCase(),
                req.description() != null ? req.description().trim() : null
        );

        InventoryItemEntity saved = itemRepository.save(entity);
        return mapToItemDto(saved);
    }

    @Transactional(readOnly = true)
    public List<StockLevelDto> getStationStockLevels(UUID stationId) {
        StationEntity station = stationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + stationId));

        List<StockLevelEntity> stockLevels = stockLevelRepository.findByStationId(stationId);
        if (stockLevels.isEmpty()) {
            return List.of();
        }

        Map<UUID, InventoryItemEntity> itemsMap = itemRepository.findAllById(
                stockLevels.stream().map(StockLevelEntity::getInventoryItemId).toList()
        ).stream().collect(Collectors.toMap(InventoryItemEntity::getId, Function.identity()));

        return stockLevels.stream()
                .map(sl -> {
                    InventoryItemEntity item = itemsMap.get(sl.getInventoryItemId());
                    return mapToStockLevelDto(sl, station.getName(), item);
                })
                .toList();
    }

    @Transactional
    public StockLevelDto createStockLevel(CreateStockLevelRequest req) {
        StationEntity station = stationRepository.findById(req.stationId())
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + req.stationId()));

        InventoryItemEntity item = itemRepository.findById(req.inventoryItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found with id: " + req.inventoryItemId()));

        if (stockLevelRepository.findByStationIdAndInventoryItemId(req.stationId(), req.inventoryItemId()).isPresent()) {
            throw new DomainException("Stock level already exists for station '" + station.getCode() +
                    "' and item '" + item.getItemCode() + "'");
        }

        int leadTime = req.leadTimeDays() != null ? req.leadTimeDays() : 14;
        StockLevelEntity stockLevel = new StockLevelEntity(
                UUID.randomUUID(),
                req.stationId(),
                req.inventoryItemId(),
                req.currentStock(),
                req.dailyConsumption(),
                req.safetyStock(),
                req.reorderPoint(),
                leadTime,
                req.nextResupplyDate()
        );

        StockLevelEntity saved = stockLevelRepository.save(stockLevel);
        return mapToStockLevelDto(saved, station.getName(), item);
    }

    @Transactional
    public StockLevelDto recordTransaction(CreateTransactionRequest req) {
        StationEntity station = stationRepository.findById(req.stationId())
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + req.stationId()));

        InventoryItemEntity item = itemRepository.findById(req.inventoryItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found with id: " + req.inventoryItemId()));

        StockLevelEntity stockLevel = stockLevelRepository.findByStationIdAndInventoryItemId(req.stationId(), req.inventoryItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock level not found for station: " + req.stationId() +
                        " and item: " + req.inventoryItemId()));

        BigDecimal qty = req.quantity();
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Transaction quantity must be strictly greater than 0");
        }

        String txType = req.type().trim().toUpperCase();
        BigDecimal current = stockLevel.getCurrentStock();
        BigDecimal updatedStock;

        switch (txType) {
            case "CONSUMPTION", "TRANSFER_OUT", "WASTE" -> {
                if (current.compareTo(qty) < 0) {
                    throw new DomainException("Insufficient stock for " + txType +
                            ". Available: " + current + ", requested: " + qty);
                }
                updatedStock = current.subtract(qty);
            }
            case "RECEIPT", "TRANSFER_IN" -> {
                updatedStock = current.add(qty);
            }
            case "ADJUSTMENT" -> {
                // If caller supplies adjustment amount to add or if valid
                updatedStock = current.add(qty);
                if (updatedStock.compareTo(BigDecimal.ZERO) < 0) {
                    throw new DomainException("Adjustment would cause stock to become negative: " + updatedStock);
                }
            }
            default -> throw new DomainException("Unsupported transaction type: " + req.type() +
                    ". Allowed types: RECEIPT, CONSUMPTION, TRANSFER_OUT, TRANSFER_IN, ADJUSTMENT, WASTE");
        }

        // 1. Mutate stock level
        stockLevel.setCurrentStock(updatedStock);
        StockLevelEntity savedStockLevel = stockLevelRepository.save(stockLevel);

        // 2. Insert immutable transaction ledger entry
        String source = req.source() != null && !req.source().isBlank() ? req.source().trim() : "MANUAL";
        InventoryTransactionEntity txEntity = new InventoryTransactionEntity(
                UUID.randomUUID(),
                req.stationId(),
                req.inventoryItemId(),
                txType,
                qty,
                Instant.now(),
                source,
                req.referenceId(),
                req.operatorId()
        );
        transactionRepository.save(txEntity);

        return mapToStockLevelDto(savedStockLevel, station.getName(), item);
    }

    private InventoryItemDto mapToItemDto(InventoryItemEntity entity) {
        return new InventoryItemDto(
                entity.getId(),
                entity.getItemCode(),
                entity.getName(),
                entity.getCategory(),
                entity.getUnit(),
                entity.getDescription()
        );
    }

    private StockLevelDto mapToStockLevelDto(StockLevelEntity sl, String stationName, InventoryItemEntity item) {
        String itemCode = item != null ? item.getItemCode() : "UNKNOWN";
        String itemName = item != null ? item.getName() : "UNKNOWN";
        String category = item != null ? item.getCategory() : "UNKNOWN";
        String unit = item != null ? item.getUnit() : "UNKNOWN";

        return new StockLevelDto(
                sl.getId(),
                sl.getStationId(),
                stationName,
                sl.getInventoryItemId(),
                itemCode,
                itemName,
                category,
                unit,
                sl.getCurrentStock(),
                sl.getDailyConsumption(),
                sl.getSafetyStock(),
                sl.getReorderPoint(),
                sl.getLeadTimeDays(),
                sl.getNextResupplyDate(),
                sl.getVersion()
        );
    }
}
