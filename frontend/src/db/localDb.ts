import Dexie, { type Table } from 'dexie';
import { StockLevel, StationIntelligenceReport, CargoConsignment, Station } from '../types';

export interface LocalMetadata {
  key: string;
  value: string;
}

export interface PendingOperation {
  id?: number;
  clientTransactionId: string;
  deviceId: string;
  operation: string; // e.g. "INVENTORY_TRANSACTION"
  payload: any;
  createdAt: number;
  status: 'PENDING' | 'SYNCING' | 'FAILED' | 'SYNCED';
  errorMessage?: string;
}

export interface CachedInventory {
  stationId: string;
  items: StockLevel[];
  updatedAt: number;
}

export interface CachedIntelligence {
  stationId: string;
  report: StationIntelligenceReport;
  updatedAt: number;
}

export interface CachedCargo {
  stationId: string;
  consignments: CargoConsignment[];
  updatedAt: number;
}

export interface CachedStations {
  key: string;
  stations: Station[];
  updatedAt: number;
}

export class PolarOpsLocalDB extends Dexie {
  metadata!: Table<LocalMetadata, string>;
  pendingOperations!: Table<PendingOperation, number>;
  cachedInventory!: Table<CachedInventory, string>;
  cachedIntelligence!: Table<CachedIntelligence, string>;
  cachedCargo!: Table<CachedCargo, string>;
  cachedStations!: Table<CachedStations, string>;

  constructor() {
    super('PolarOpsLocalDB');
    this.version(1).stores({
      metadata: 'key',
      pendingOperations: '++id, clientTransactionId, deviceId, operation, status, createdAt',
      cachedInventory: 'stationId',
      cachedIntelligence: 'stationId',
      cachedCargo: 'stationId',
      cachedStations: 'key',
    });
  }
}

export const localDb = new PolarOpsLocalDB();

const DEVICE_ID_KEY = 'polarops_device_uuid';

export async function getOrCreateDeviceId(): Promise<string> {
  const existing = await localDb.metadata.get(DEVICE_ID_KEY);
  if (existing && existing.value) {
    return existing.value;
  }
  const newDeviceId = crypto.randomUUID();
  await localDb.metadata.put({ key: DEVICE_ID_KEY, value: newDeviceId });
  return newDeviceId;
}
