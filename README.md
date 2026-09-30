# PolarOps — Integrated Polar Expedition Logistics & Asset Management System

> Smart India Hackathon 2026 — Problem Statement 26062  
> Ministry of Earth Sciences / NCPOR  
> Category: Smart Automation | Software

PolarOps is an **offline-first expedition logistics and asset management platform** designed for polar research operations.

The system integrates **inventory intelligence, cargo tracking, expedition personnel movement, emergency response, and offline synchronization** into a single operational platform.

It is designed specifically for environments where connectivity may be intermittent, infrastructure is constrained, and operational decisions need to remain reliable even when the internet is unavailable.

---

## 🚀 Key Features

### 📦 Inventory Management

- Centralized inventory catalogue
- Station-wise stock levels
- Inventory transaction ledger
- Receipt, consumption, transfer and adjustment transactions
- Safety stock and reorder point tracking
- Daily consumption tracking
- Lead-time aware inventory intelligence
- Optimistic locking for concurrent stock updates

### 🧠 Inventory Intelligence

PolarOps continuously evaluates inventory risk using deterministic rules.

Risk levels:

- `CRITICAL`
- `WARNING`
- `HEALTHY`

The system considers:

- Current stock
- Daily consumption
- Safety stock
- Reorder point
- Next resupply date
- Resupply delays

It can also generate **station-to-station transfer recommendations** based on surplus and shortfall.

---

### 🚢 Cargo Management

- Create cargo consignments
- Track cargo by station
- Track cargo items
- Cargo status management
- ARRIVED → RECEIVED workflow
- Atomic inventory updates when cargo is received
- Duplicate receipt protection
- Cargo tracking numbers

Example workflow:

```text
Cargo Arrives
      ↓
ARRIVED
      ↓
Receive Cargo
      ↓
Inventory Transaction Created
      ↓
Stock Updated
      ↓
Inventory Intelligence Recalculated
