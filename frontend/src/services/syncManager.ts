import { syncApi } from '../api/api';
import { localDb, PendingOperation } from '../db/localDb';

export interface SyncProgress {
  isSyncing: boolean;
  lastSyncedAt: number | null;
  pendingCount: number;
  syncError: string | null;
}

export class SyncManager {
  private isSyncing = false;
  private listeners: ((progress: SyncProgress) => void)[] = [];
  private lastSyncedAt: number | null = null;
  private syncError: string | null = null;

  public subscribe(listener: (progress: SyncProgress) => void) {
    this.listeners.push(listener);
    this.notify();
    return () => {
      this.listeners = this.listeners.filter((l) => l !== listener);
    };
  }

  public async getPendingCount(): Promise<number> {
    return await localDb.pendingOperations.where('status').equals('PENDING').count();
  }

  public async notify() {
    const pendingCount = await this.getPendingCount();
    const progress: SyncProgress = {
      isSyncing: this.isSyncing,
      lastSyncedAt: this.lastSyncedAt,
      pendingCount,
      syncError: this.syncError,
    };
    this.listeners.forEach((l) => l(progress));
  }

  public async queueOperation(operation: string, payload: any, clientTransactionId?: string): Promise<PendingOperation> {
    const deviceId = await (await import('../db/localDb')).getOrCreateDeviceId();
    const txId = clientTransactionId || crypto.randomUUID();

    const pending: PendingOperation = {
      clientTransactionId: txId,
      deviceId,
      operation,
      payload,
      createdAt: Date.now(),
      status: 'PENDING',
    };

    const id = await localDb.pendingOperations.add(pending);
    pending.id = id;
    await this.notify();
    return pending;
  }

  public async sync(): Promise<{ success: boolean; applied: number; duplicate: number; failed: number }> {
    if (this.isSyncing) return { success: false, applied: 0, duplicate: 0, failed: 0 };
    if (!navigator.onLine) {
      this.syncError = 'Device is currently offline.';
      await this.notify();
      return { success: false, applied: 0, duplicate: 0, failed: 0 };
    }

    this.isSyncing = true;
    this.syncError = null;
    await this.notify();

    let appliedCount = 0;
    let duplicateCount = 0;
    let failedCount = 0;

    try {
      const pendingList = await localDb.pendingOperations
        .where('status')
        .equals('PENDING')
        .sortBy('createdAt');

      if (pendingList.length === 0) {
        this.isSyncing = false;
        await this.notify();
        return { success: true, applied: 0, duplicate: 0, failed: 0 };
      }

      // Mark as SYNCING
      for (const op of pendingList) {
        if (op.id) await localDb.pendingOperations.update(op.id, { status: 'SYNCING' });
      }

      const operationsPayload = pendingList.map((op) => ({
        clientTransactionId: op.clientTransactionId,
        deviceId: op.deviceId,
        operation: op.operation,
        payload: op.payload,
      }));

      const response = await syncApi.push(operationsPayload);
      const results: { clientTransactionId: string; status: string; message?: string }[] = response.results || [];

      for (const result of results) {
        const localOp = pendingList.find((p) => p.clientTransactionId === result.clientTransactionId);
        if (localOp && localOp.id) {
          if (result.status === 'APPLIED') {
            await localDb.pendingOperations.update(localOp.id, { status: 'SYNCED' });
            appliedCount++;
          } else if (result.status === 'DUPLICATE') {
            await localDb.pendingOperations.update(localOp.id, { status: 'SYNCED' });
            duplicateCount++;
          } else {
            await localDb.pendingOperations.update(localOp.id, {
              status: 'FAILED',
              errorMessage: result.message || 'Operation rejected by server',
            });
            failedCount++;
          }
        }
      }

      this.lastSyncedAt = Date.now();
      return { success: true, applied: appliedCount, duplicate: duplicateCount, failed: failedCount };
    } catch (err: any) {
      this.syncError = err.message || 'Network error during synchronization';
      // Reset SYNCING to PENDING so they can be retried
      const syncing = await localDb.pendingOperations.where('status').equals('SYNCING').toArray();
      for (const op of syncing) {
        if (op.id) await localDb.pendingOperations.update(op.id, { status: 'PENDING' });
      }
      return { success: false, applied: 0, duplicate: 0, failed: 0 };
    } finally {
      this.isSyncing = false;
      await this.notify();
    }
  }
}

export const syncManager = new SyncManager();
