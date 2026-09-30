package com.polarops.sync;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChangeEventRepository extends JpaRepository<ChangeEventEntity, UUID> {
    Optional<ChangeEventEntity> findByClientTransactionId(UUID clientTransactionId);
}
