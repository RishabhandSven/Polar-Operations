package com.polarops.emergency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<AlertEntity, UUID> {
    List<AlertEntity> findByStationIdOrderByCreatedAtDesc(UUID stationId);
    List<AlertEntity> findByStationIdAndResolvedOrderByCreatedAtDesc(UUID stationId, boolean resolved);
}
