package com.polarops.personnel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PersonnelRepository extends JpaRepository<PersonnelEntity, UUID> {
    List<PersonnelEntity> findByStationId(UUID stationId);
    List<PersonnelEntity> findByStationIdAndStatus(UUID stationId, String status);
    long countByStationIdAndStatus(UUID stationId, String status);
}
