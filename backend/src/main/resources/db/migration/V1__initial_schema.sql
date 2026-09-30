-- V1 Initial Schema for PolarOps

-- 1. Stations
CREATE TABLE stations (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    latitude DECIMAL(9, 6) NOT NULL,
    longitude DECIMAL(9, 6) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPERATIONAL'
);

-- 2. Item Master Catalogue
CREATE TABLE inventory_items (
    id UUID PRIMARY KEY,
    item_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    description TEXT
);

-- 3. Station-Specific Stock Levels
CREATE TABLE stock_levels (
    id UUID PRIMARY KEY,
    station_id UUID NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    inventory_item_id UUID NOT NULL REFERENCES inventory_items(id) ON DELETE RESTRICT,
    current_stock DECIMAL(12, 2) NOT NULL CHECK (current_stock >= 0),
    daily_consumption DECIMAL(12, 2) NOT NULL CHECK (daily_consumption >= 0),
    safety_stock DECIMAL(12, 2) NOT NULL CHECK (safety_stock >= 0),
    reorder_point DECIMAL(12, 2) NOT NULL CHECK (reorder_point >= 0),
    lead_time_days INT NOT NULL DEFAULT 14,
    next_resupply_date DATE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_station_item UNIQUE (station_id, inventory_item_id)
);
CREATE INDEX idx_stock_levels_station ON stock_levels(station_id);

-- 4. Immutable Inventory Transactions Ledger
CREATE TABLE inventory_transactions (
    id UUID PRIMARY KEY,
    station_id UUID NOT NULL REFERENCES stations(id),
    inventory_item_id UUID NOT NULL REFERENCES inventory_items(id),
    type VARCHAR(32) NOT NULL,
    quantity DECIMAL(12, 2) NOT NULL CHECK (quantity > 0),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    source VARCHAR(64) NOT NULL,
    reference_id VARCHAR(128),
    operator_id VARCHAR(64)
);
CREATE INDEX idx_inv_tx_station_item ON inventory_transactions(station_id, inventory_item_id);

-- 5. Cargo Consignments
CREATE TABLE cargo_consignments (
    id UUID PRIMARY KEY,
    tracking_number VARCHAR(64) NOT NULL UNIQUE,
    destination_station_id UUID NOT NULL REFERENCES stations(id),
    status VARCHAR(32) NOT NULL,
    departure_date DATE,
    estimated_arrival DATE NOT NULL,
    actual_arrival DATE
);
CREATE INDEX idx_cargo_station ON cargo_consignments(destination_station_id);

-- 6. Cargo Manifest Items
CREATE TABLE cargo_items (
    id UUID PRIMARY KEY,
    consignment_id UUID NOT NULL REFERENCES cargo_consignments(id) ON DELETE CASCADE,
    inventory_item_id UUID NOT NULL REFERENCES inventory_items(id),
    quantity DECIMAL(12, 2) NOT NULL CHECK (quantity > 0)
);

-- 7. Personnel
CREATE TABLE personnel (
    id UUID PRIMARY KEY,
    station_id UUID NOT NULL REFERENCES stations(id),
    name VARCHAR(128) NOT NULL,
    role VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ON_STATION'
);

-- 8. Personnel Movements
CREATE TABLE personnel_movements (
    id UUID PRIMARY KEY,
    personnel_id UUID NOT NULL REFERENCES personnel(id) ON DELETE CASCADE,
    type VARCHAR(32) NOT NULL,
    destination VARCHAR(128),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    notes TEXT
);

-- 9. Alerts
CREATE TABLE alerts (
    id UUID PRIMARY KEY,
    station_id UUID NOT NULL REFERENCES stations(id),
    type VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved BOOLEAN NOT NULL DEFAULT FALSE
);

-- 10. Audit & Offline Sync Ledger
CREATE TABLE change_events (
    id UUID PRIMARY KEY,
    client_transaction_id UUID NOT NULL UNIQUE,
    device_id UUID NOT NULL,
    operation VARCHAR(64) NOT NULL,
    payload_json TEXT NOT NULL,
    server_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_message TEXT
);
CREATE INDEX idx_change_events_client_tx ON change_events(client_transaction_id);
