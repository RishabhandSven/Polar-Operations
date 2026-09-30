package com.polarops.personnel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PersonnelMovementRepository extends JpaRepository<PersonnelMovementEntity, UUID> {
    List<PersonnelMovementEntity> findByPersonnelIdOrderByTimestampDesc(UUID personnelId);
}
