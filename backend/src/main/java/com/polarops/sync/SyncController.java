package com.polarops.sync;

import com.polarops.sync.SyncDtos.SyncBatchRequest;
import com.polarops.sync.SyncDtos.SyncBatchResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/push")
    public ResponseEntity<SyncBatchResponse> pushSyncOperations(@Valid @RequestBody SyncBatchRequest request) {
        SyncBatchResponse response = syncService.processBatch(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
