package com.polarops.expedition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StationRepository extends JpaRepository<StationEntity, UUID> {
    Optional<StationEntity> findByCode(String code);
}
