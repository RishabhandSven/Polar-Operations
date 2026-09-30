package com.polarops.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockLevelRepository extends JpaRepository<StockLevelEntity, UUID> {
    List<StockLevelEntity> findByStationId(UUID stationId);
    Optional<StockLevelEntity> findByStationIdAndInventoryItemId(UUID stationId, UUID inventoryItemId);
    List<StockLevelEntity> findByInventoryItemId(UUID inventoryItemId);
}
