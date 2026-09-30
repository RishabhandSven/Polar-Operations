package com.polarops.cargo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CargoConsignmentRepository extends JpaRepository<CargoConsignmentEntity, UUID> {
    Optional<CargoConsignmentEntity> findByTrackingNumber(String trackingNumber);
    List<CargoConsignmentEntity> findByDestinationStationId(UUID destinationStationId);
    List<CargoConsignmentEntity> findByDestinationStationIdAndStatus(UUID destinationStationId, String status);
}
