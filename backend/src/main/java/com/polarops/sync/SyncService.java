package com.polarops.sync;

import com.polarops.sync.SyncDtos.SyncBatchRequest;
import com.polarops.sync.SyncDtos.SyncBatchResponse;
import com.polarops.sync.SyncDtos.SyncOperationRequest;
import com.polarops.sync.SyncDtos.SyncOperationResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SyncService {

    private final SyncOperationProcessor syncOperationProcessor;

    public SyncService(SyncOperationProcessor syncOperationProcessor) {
        this.syncOperationProcessor = syncOperationProcessor;
    }

    /**
     * Processes a batch of offline synchronization operations in strict FIFO sequence.
     * Each operation is isolated into its own transactional boundary.
     */
    public SyncBatchResponse processBatch(SyncBatchRequest request) {
        List<SyncOperationResult> results = new ArrayList<>();

        if (request == null || request.operations() == null) {
            return new SyncBatchResponse(results);
        }

        for (SyncOperationRequest op : request.operations()) {
            SyncOperationResult result = syncOperationProcessor.processSingleOperation(op);
            results.add(result);
        }

        return new SyncBatchResponse(results);
    }
}
