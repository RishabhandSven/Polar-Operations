export interface Station {
  id: string;
  code: string;
  name: string;
  latitude: number;
  longitude: number;
  status: string;
}

export const STATIONS: Record<string, Station> = {
  BHARATI: {
    id: '22222222-2222-2222-2222-222222222222',
    code: 'BHA',
    name: 'Bharati Station',
    latitude: -69.4072,
    longitude: 76.1914,
    status: 'OPERATIONAL'
  },
  MAITRI: {
    id: '11111111-1111-1111-1111-111111111111',
    code: 'MAI',
    name: 'Maitri Station',
    latitude: -70.7667,
    longitude: 11.7333,
    status: 'OPERATIONAL'
  },
  HIMADRI: {
    id: '33333333-3333-3333-3333-333333333333',
    code: 'HIM',
    name: 'Himadri (Arctic)',
    latitude: 78.9244,
    longitude: 11.9286,
    status: 'OPERATIONAL'
  }
};

export type RiskLevel = 'CRITICAL' | 'WARNING' | 'HEALTHY';

export interface InventoryItem {
  id: string;
  itemCode: string;
  name: string;
  category: string;
  unit: string;
  description?: string;
}

export interface StockLevel {
  id: string;
  stationId: string;
  stationName: string;
  inventoryItemId: string;
  itemCode: string;
  itemName: string;
  category: string;
  unit: string;
  currentStock: number;
  dailyConsumption: number;
  safetyStock: number;
  reorderPoint: number;
  leadTimeDays: number;
  nextResupplyDate: string;
  version: number;
}

export interface InventoryTransaction {
  id: string;
  stationId: string;
  inventoryItemId: string;
  type: string;
  quantity: number;
  timestamp: string;
  source: string;
  referenceId?: string;
  operatorId?: string;
}

export interface CreateTransactionRequest {
  stationId: string;
  inventoryItemId: string;
  type: string;
  quantity: number;
  source?: string;
  referenceId?: string;
  operatorId?: string;
}

export interface StockRiskAssessment {
  stockLevelId: string;
  inventoryItemId: string;
  itemCode: string;
  itemName: string;
  category: string;
  unit: string;
  currentStock: number;
  safetyStock: number;
  dailyConsumption: number;
  effectiveResupplyDate: string;
  daysToSafetyThreshold: number | null;
  projectedStockAtResupply: number;
  riskLevel: RiskLevel;
  explanation: string;
}

export interface TransferRecommendation {
  sourceStationId: string;
  sourceStationName: string;
  targetStationId: string;
  targetStationName: string;
  inventoryItemId: string;
  itemCode: string;
  itemName: string;
  recommendedQuantity: number;
  unit: string;
  rationale: string;
}

export interface StationIntelligenceReport {
  stationId: string;
  stationName: string;
  simulatedDelayDays: number;
  assessments: StockRiskAssessment[];
  recommendations: TransferRecommendation[];
  evaluatedAt: string;
}

export interface AddCargoItemRequest {
  inventoryItemId: string;
  quantity: number;
}

export interface CreateCargoConsignmentRequest {
  trackingNumber: string;
  destinationStationId: string;
  status?: string;
  departureDate?: string;
  estimatedArrival: string;
  initialItems?: AddCargoItemRequest[];
}

export interface CargoItem {
  id: string;
  consignmentId: string;
  inventoryItemId: string;
  itemCode: string;
  itemName: string;
  category?: string;
  unit: string;
  quantity: number;
}

export interface CargoConsignment {
  id: string;
  trackingNumber: string;
  destinationStationId: string;
  destinationStationName: string;
  status: 'PLANNED' | 'IN_TRANSIT' | 'ARRIVED' | 'RECEIVED' | 'CANCELLED';
  departureDate?: string;
  estimatedArrival: string;
  actualArrival?: string;
  items: CargoItem[];
}

export interface ReceiveCargoResponse {
  consignmentId: string;
  trackingNumber: string;
  previousStatus: string;
  newStatus: string;
  stationId: string;
  stationName: string;
  receivedItemCount: number;
  totalReceivedQuantity: number;
  receivedAt: string;
}

export interface Personnel {
  id: string;
  stationId: string;
  name: string;
  role: string;
  status: string;
}

export interface Alert {
  id: string;
  stationId: string;
  type: string;
  severity: string;
  message: string;
  createdAt: string;
  resolved: boolean;
}
