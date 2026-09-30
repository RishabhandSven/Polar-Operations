import { request } from './client';
import {
  InventoryItem,
  StockLevel,
  CreateTransactionRequest,
  StationIntelligenceReport,
  CargoConsignment,
  CreateCargoConsignmentRequest,
  ReceiveCargoResponse,
} from '../types';

export const inventoryApi = {
  getAllItems: () => request<InventoryItem[]>('/api/inventory/items'),

  getStockLevels: (stationId: string) =>
    request<StockLevel[]>(`/api/inventory?stationId=${encodeURIComponent(stationId)}`),

  recordTransaction: (data: CreateTransactionRequest) =>
    request<StockLevel>('/api/inventory/transactions', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
};

export const intelligenceApi = {
  getReport: (stationId: string, delayDays: number = 0) =>
    request<StationIntelligenceReport>(
      `/api/inventory/intelligence?stationId=${encodeURIComponent(stationId)}&delayDays=${delayDays}`
    ),
};

export const cargoApi = {
  getConsignments: (stationId?: string) => {
    const query = stationId ? `?stationId=${encodeURIComponent(stationId)}` : '';
    return request<CargoConsignment[]>(`/api/cargo${query}`);
  },

  getConsignmentById: (id: string) => request<CargoConsignment>(`/api/cargo/${encodeURIComponent(id)}`),

  createConsignment: (data: CreateCargoConsignmentRequest) =>
    request<CargoConsignment>('/api/cargo', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  receiveConsignment: (id: string, operatorId: string = 'CARGO-OPERATOR') =>
    request<ReceiveCargoResponse>(`/api/cargo/${encodeURIComponent(id)}/receive`, {
      method: 'POST',
      body: JSON.stringify({ operatorId }),
    }),
};

export const syncApi = {
  push: (operations: any[]) =>
    request<any>('/api/sync/push', {
      method: 'POST',
      body: JSON.stringify({ operations }),
    }),
};

export const personnelApi = {
  list: (stationId?: string) => {
    const query = stationId ? `?stationId=${encodeURIComponent(stationId)}` : '';
    return request<any[]>(`/api/personnel${query}`);
  },
  create: (data: { stationId: string; name: string; role: string; status?: string }) =>
    request<any>('/api/personnel', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  listMovements: (personnelId: string) =>
    request<any[]>(`/api/personnel/${encodeURIComponent(personnelId)}/movements`),
  recordMovement: (personnelId: string, data: { type: string; destination?: string; notes?: string }) =>
    request<any>(`/api/personnel/${encodeURIComponent(personnelId)}/movements`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),
};

export const emergencyApi = {
  listAlerts: (stationId?: string, resolved?: boolean) => {
    const params = new URLSearchParams();
    if (stationId) params.append('stationId', stationId);
    if (resolved !== undefined) params.append('resolved', String(resolved));
    const query = params.toString() ? `?${params.toString()}` : '';
    return request<any[]>(`/api/emergency/alerts${query}`);
  },
  createAlert: (data: { stationId: string; type: string; severity: string; message: string }) =>
    request<any>('/api/emergency/alerts', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  resolveAlert: (id: string) =>
    request<any>(`/api/emergency/alerts/${encodeURIComponent(id)}/resolve`, {
      method: 'POST',
    }),
};
