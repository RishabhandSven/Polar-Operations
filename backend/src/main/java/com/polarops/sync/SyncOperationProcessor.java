package com.polarops.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarops.inventory.InventoryDtos.CreateTransactionRequest;
import com.polarops.inventory.InventoryService;
import com.polarops.sync.SyncDtos.SyncOperationRequest;
import com.polarops.sync.SyncDtos.SyncOperationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

@Service
public class SyncOperationProcessor {

    private static final Logger log = LoggerFactory.getLogger(SyncOperationProcessor.class);

    private final ChangeEventRepository changeEventRepository;
    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public SyncOperationProcessor(ChangeEventRepository changeEventRepository,
                                  InventoryService inventoryService,
                                  ObjectMapper objectMapper,
                                  PlatformTransactionManager transactionManager) {
        this.changeEventRepository = changeEventRepository;
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Executes an individual sync operation in its own isolated transaction.
     * Uses TransactionTemplate with PROPAGATION_REQUIRES_NEW.
     * If an operation fails, its transaction cleanly rolls back and returns a REJECTED result.
     */
    public SyncOperationResult processSingleOperation(SyncOperationRequest op) {
        UUID clientTxId = op.clientTransactionId();
        if (clientTxId == null) {
            return SyncOperationResult.rejected(null, "Missing clientTransactionId");
        }

        // 1. Idempotency Pre-Check
        if (changeEventRepository.findByClientTransactionId(clientTxId).isPresent()) {
            log.info("Duplicate sync operation detected for clientTransactionId: {}", clientTxId);
            return SyncOperationResult.duplicate(clientTxId);
        }

        // 2. Validate Operation Whitelist
        String operationType = op.operation() != null ? op.operation().trim().toUpperCase() : "";
        if (!"INVENTORY_TRANSACTION".equals(operationType)) {
            return SyncOperationResult.rejected(clientTxId, "Unsupported operation: " + op.operation() + ". Allowed: INVENTORY_TRANSACTION");
        }

        if (op.payload() == null || op.payload().isNull()) {
            return SyncOperationResult.rejected(clientTxId, "Operation payload cannot be null");
        }

        try {
            return transactionTemplate.execute(status -> {
                // Check again inside transaction (useful for repeatable read or concurrency)
                if (changeEventRepository.findByClientTransactionId(clientTxId).isPresent()) {
                    return SyncOperationResult.duplicate(clientTxId);
                }

                try {
                    // 3. Deserialize Payload
                    CreateTransactionRequest txRequest = objectMapper.treeToValue(op.payload(), CreateTransactionRequest.class);

                    // 4. Delegate to Domain Service
                    inventoryService.recordTransaction(txRequest);

                    // 5. Record ChangeEvent Audit Entry
                    String payloadJson = objectMapper.writeValueAsString(op.payload());
                    ChangeEventEntity changeEvent = new ChangeEventEntity(
                            UUID.randomUUID(),
                            clientTxId,
                            op.deviceId(),
                            operationType,
                            payloadJson,
                            Instant.now(),
                            "APPLIED",
                            null
                    );

                    changeEventRepository.save(changeEvent);

                    return SyncOperationResult.applied(clientTxId);

                } catch (DataIntegrityViolationException ex) {
                    status.setRollbackOnly();
                    log.warn("Data integrity violation for clientTransactionId {}: {}", clientTxId, ex.getMessage());
                    return SyncOperationResult.duplicate(clientTxId);
                } catch (Exception ex) {
                    status.setRollbackOnly();
                    log.warn("Sync operation rejected for clientTransactionId {}: {}", clientTxId, ex.getMessage());
                    return SyncOperationResult.rejected(clientTxId, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
                }
            });

        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent duplicate key detected for clientTransactionId {}: {}", clientTxId, ex.getMessage());
            return SyncOperationResult.duplicate(clientTxId);
        } catch (Exception ex) {
            log.error("Unexpected error processing clientTransactionId {}: {}", clientTxId, ex.getMessage());
            return SyncOperationResult.rejected(clientTxId, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
    }
}
