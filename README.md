# PolarOps — Integrated Polar Expedition Logistics & Asset Management System

> **Smart India Hackathon 2026 — Problem Statement 26062**  
> **Ministry of Earth Sciences / NCPOR**  
> **Category:** Smart Automation | **Domain:** Software  

**PolarOps** is an expedition-grade, modular monolithic logistics and asset management platform engineered specifically for the extreme operational constraints of polar research stations in Antarctica and the Arctic (Maitri, Bharati, and Himadri).

The platform unifies **inventory intelligence, atomic cargo receipt, expedition personnel movement, crisis emergency management, and store-and-forward offline synchronization** into a resilient operational console. When satellite connectivity drops during blizzards or geomagnetic storms, station operators can continue logging missions and material transactions locally via IndexedDB. Upon reconnection, transactions automatically reconcile with the central Spring Boot backend with deterministic idempotency.

---

## Features

- **Inventory Domain & Immutable Ledger**: Centralized item master catalogue (`InventoryItem`), station-specific real-time stock balances (`StockLevel`), and an append-only audit ledger (`InventoryTransaction`) supporting transaction classifications: `CONSUMPTION`, `RECEIPT`, `TRANSFER_OUT`, `TRANSFER_IN`, `ADJUSTMENT`, and `WASTE`.
- **Deterministic Inventory Intelligence Engine**: Mathematical shortage and stockout projection engine that calculates days to safety threshold, flags `CRITICAL`, `WARNING`, and `HEALTHY` risk classifications under simulated resupply delays, and autonomously recommends inter-station mutual-aid transfers from surplus stations.
- **Cargo Management & Atomic Receipt**: Lifecycle tracking for expedition cargo consignments (`PLANNED` $\rightarrow$ `IN_TRANSIT` $\rightarrow$ `ARRIVED` $\rightarrow$ `RECEIVED`). Supports atomic cargo receipt where an arrived consignment is atomically converted into station inventory ledger receipts wrapped in a Spring `@Transactional` database boundary.
- **Personnel & Sortie Movement Roster**: Crew tracking per station (`ON_STATION`, `DEPLOYED_SORTIE`, `EVACUATED`, `IN_TRANSIT`) with an append-only movement ledger logging sortie departures, returns, check-ins, and check-outs.
- **Station Emergency & Crisis Alerts**: Real-time crisis logging and resolution for polar contingencies (`BLIZZARD_WARNING`, `SOS`, `GENERATOR_FAILURE`, `SAFETY_STOCK_BREACH`).
- **Offline-First Synchronization Engine**: Client-side storage via Dexie.js (IndexedDB) with persistent device UUID generation and per-mutation `clientTransactionId` UUIDs. An automated store-and-forward background queue reconciles queued actions via `POST /api/sync/push`, returning idempotent `APPLIED`, `DUPLICATE`, or `REJECTED` statuses.
- **Progressive Web App (PWA)**: Built with `vite-plugin-pwa`, complete with offline web manifest, service worker asset precaching, and standalone display support.
- **Docker Multi-Container Orchestration**: Multi-stage production container builds for both backend (Java 21 JRE) and frontend (Nginx Alpine reverse proxy and SPA routing fallback) orchestrated alongside PostgreSQL 16.

---

## Architecture

### System Topology (Online Mode)

```text
Browser / React PWA (Port 5173 or 80)
        │
        │ REST API (/api/*)
        ▼
Spring Boot Backend (Port 8080)
        │
        │ JPA / Hibernate / Flyway (Port 5432)
        ▼
PostgreSQL 16 Database ("polarops")
```

### Offline Store-and-Forward Architecture

```text
                     POLAROPS FRONTEND (React 19 PWA)
                                │
               ┌────────────────┴────────────────┐
               │                                 │
            ONLINE                            OFFLINE
               │                                 │
               ▼                                 ▼
         Direct REST API               Dexie.js (IndexedDB)
               │                                 │
               │                         Local Pending Queue
               │                                 │
               │                         Connection Restored
               │                                 ▼
               └───────────────────────► POST /api/sync/push
                                                 │
                                                 ▼
                                        SyncOperationProcessor
                                  (@Transactional REQUIRES_NEW)
                                                 │
                                  ┌──────────────┴──────────────┐
                           First Occurrence               Duplicate Replay
                                  │                             │
                                  ▼                             ▼
                            Apply Mutation             Return DUPLICATE
                        + Record ChangeEvent        (Domain Stock Unchanged)
```

1. **Client Isolation**: When network disconnects, the UI detects offline state and switches to client-side optimistic logging.
2. **IndexedDB Persistent Queue**: Mutations are saved in Dexie with a unique `clientTransactionId` and device UUID.
3. **Replay & Idempotency**: Upon reconnection, `SyncManager` pushes batches to `/api/sync/push`. The backend checks the `change_events` table and returns `DUPLICATE` without duplicating stock transactions if the event was already applied.

---

## Technology Stack

The versions and technologies below correspond strictly to the codebase:

| Category | Technology | Version | Purpose |
|---|---|---|---|
| **Runtime** | Java LTS | `21` | Backend JVM runtime |
| **Framework** | Spring Boot | `3.3.4` | Modular monolithic REST API backend |
| **Build Tool** | Apache Maven | `3.9+` | Backend dependency management & build |
| **Database** | PostgreSQL | `16` | Relational storage & ACID compliance |
| **Migrations** | Flyway | `10.x` (managed by Spring Boot) | Automated schema migration (`V1__initial_schema.sql`) |
| **Frontend Framework** | React | `19.3.0` | Client user interface library |
| **Language** | TypeScript | `6.0.2` | Static type safety |
| **Frontend Build Tool** | Vite | `8.3.0` | Ultra-fast client bundler & dev server |
| **Styling** | Tailwind CSS | `4.3.3` | Utility-first responsive design |
| **Routing** | React Router | `7.18.4` | Client-side SPA navigation |
| **Offline Storage** | Dexie.js | `4.4.6` | IndexedDB abstraction for offline queue |
| **PWA Engine** | `vite-plugin-pwa` | `1.3.0` | Service worker & Web App Manifest |
| **Geospatial Maps** | Leaflet / React-Leaflet | `1.9.4` / `5.0.0` | Antarctic station interactive mapping |
| **Charts** | Recharts | `3.10.1` | Shortage and risk projection charts |
| **Icons** | Lucide React | `1.49.0` | Expedition operational iconography |
| **Reverse Proxy** | Nginx | Alpine | Production Docker frontend server & API proxy |
| **Containers** | Docker & Docker Compose | Compose file v3 | Multi-container stack orchestration |

---

## Project Structure

```text
Polar-Operations/
├── .env.example                               # Environment variable template
├── .gitignore                                 # Git ignore definitions
├── docker-compose.yml                         # Multi-container orchestration (postgres, backend, frontend)
├── README.md                                  # Developer and operational documentation
├── docs/
│   ├── HLD.md                                 # High-Level Architecture Design
│   └── LLD.md                                 # Low-Level Design & Mathematical Models
├── backend/
│   ├── Dockerfile                             # Multi-stage build (Maven 3.9 + Temurin 21 JRE)
│   ├── pom.xml                                # Spring Boot 3.3.4 & Java 21 dependencies
│   └── src/
│       ├── main/
│       │   ├── java/com/polarops/
│       │   │   ├── PolarOpsApplication.java   # Spring Boot entry point
│       │   │   ├── common/                    # Global exceptions and HTTP error handlers
│       │   │   ├── config/                    # CORS configuration and DataSeeder
│       │   │   ├── expedition/                # Polar stations domain
│       │   │   ├── inventory/                 # Stock levels, items, ledger, intelligence engine
│       │   │   ├── cargo/                     # Consignments, manifests, atomic receipt
│       │   │   ├── personnel/                 # Crew rosters and sortie movements
│       │   │   ├── emergency/                 # Alerts and crisis incident handling
│       │   │   ├── sync/                      # Idempotent offline sync processor
│       │   │   └── health/                    # System health endpoint
│       │   └── resources/
│       │       ├── application.yml            # Database, Hibernate, Flyway, and CORS properties
│       │       └── db/migration/
│       │           └── V1__initial_schema.sql # 10 relational tables and indices
│       └── test/                              # 76 automated unit and slice tests
└── frontend/
    ├── Dockerfile                             # Multi-stage build (Node 22 + Nginx Alpine)
    ├── nginx.conf                             # Nginx SPA fallback and /api/ reverse proxy
    ├── package.json                           # React 19, Vite, Tailwind CSS v4, Dexie
    ├── package-lock.json                      # Pinned frontend dependencies
    ├── index.html                             # Single Page Application root HTML
    ├── tsconfig.json                          # TypeScript configuration
    ├── vite.config.ts                         # Vite config with PWA plugin & dev proxy
    ├── public/                                # Favicons and PWA icons
    └── src/
        ├── main.tsx                           # React DOM mount
        ├── App.tsx                            # React Router routes and context provider
        ├── index.css                          # Global styles with Tailwind imports
        ├── api/                               # HTTP client (client.ts) and API modules (api.ts)
        ├── context/                           # StationContext (station selector, connectivity state)
        ├── db/                                # Dexie IndexedDB database schema (localDb.ts)
        ├── services/                          # Offline sync manager queue (syncManager.ts)
        ├── types/                             # TypeScript shared interfaces and Enums
        ├── components/                        # UI cards, modals, maps, charts, navigation
        └── pages/                             # Dashboard, Inventory, Intelligence, Cargo, Personnel, Emergency
```

---

# 🚀 Getting Started

There are two supported ways to run PolarOps:

1. **Development Mode (Recommended for development)**:
   - PostgreSQL started via Docker container.
   - Spring Boot backend started natively with Maven (`mvn spring-boot:run`).
   - React frontend started natively with Vite (`npm run dev`).
   - Offers instant Hot-Module-Replacement (HMR) for frontend and fast Java restarts.

2. **Docker Compose Mode**:
   - The entire stack (`postgres`, `backend`, `frontend`) builds and runs in isolated containers via `docker compose up --build`.
   - Ideal for demonstration, testing, and production simulation.

---

## Prerequisites

Verify that the following tools are installed on your machine:

| Requirement | Minimum / Recommended Version | Purpose | Verification Command |
|---|---|---|---|
| **Git** | `2.40+` | Source control | `git --version` |
| **Java Development Kit (JDK)** | `Java 21 LTS` | Backend compilation and execution | `java -version` |
| **Apache Maven** | `3.9+` | Java dependency management | `mvn -version` |
| **Node.js** | `v20.x` or `v22.x` (LTS) | Frontend runtime environment | `node -v` |
| **npm** | `10.x` or `11.x` | Frontend package manager | `npm -v` |
| **Docker Desktop** | `24.x+` (Docker Compose `v2.x+`) | PostgreSQL database and container stack | `docker --version`<br>`docker compose version` |

> [!IMPORTANT]
> **Docker Desktop must be running** before executing any `docker` or `docker compose` commands.

---

## Step 1 — Clone the Repository

Open your terminal (PowerShell on Windows, or Bash on macOS/Linux) and clone the repository:

```powershell
git clone https://github.com/RishabhandSven/Polar-Operations.git
cd Polar-Operations
git status
```

*Ensure all subsequent commands are run from the project root (`Polar-Operations`).*

---

## Step 2 — Environment Configuration

PolarOps uses sensible local-development defaults embedded in `backend/src/main/resources/application.yml` and `docker-compose.yml`.

A template file `.env.example` is provided:
```properties
# Database Configuration
POSTGRES_DB=polarops
POSTGRES_USER=polarops
POSTGRES_PASSWORD=polarops_dev
POSTGRES_PORT=5432

# Backend Spring Boot Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/polarops
SPRING_DATASOURCE_USERNAME=polarops
SPRING_DATASOURCE_PASSWORD=polarops_dev

# Frontend Configuration (Leave empty to use Vite / Nginx reverse proxy)
VITE_API_BASE_URL=
```

To create a `.env` file for custom Docker Compose configuration:

**Windows PowerShell**:
```powershell
Copy-Item .env.example .env
```

**macOS / Linux**:
```bash
cp .env.example .env
```

> [!WARNING]
> **Never commit real credentials, production passwords, or API keys to Git.** The `.gitignore` file already excludes `.env` and `.env.*`.

---

## Step 3 — Start PostgreSQL

PostgreSQL must be running before the Spring Boot backend can start. Launch the database container using Docker Compose:

```powershell
docker compose up -d postgres
```

Check the container status:
```powershell
docker compose ps
```

The status may initially show `health: starting`. Wait a few seconds until the status displays `healthy`:

```text
NAME                IMAGE                COMMAND                  SERVICE    STATUS
polarops-postgres   postgres:16-alpine   "docker-entrypoint.s…"   postgres   Up 10 seconds (healthy)
```

- **Host**: `localhost`
- **Port**: `5432`
- **Database Name**: `polarops`
- **Username**: `polarops`
- **Password**: `polarops_dev`

---

## Step 4 — Verify PostgreSQL

To check database container logs and ensure PostgreSQL is ready for incoming connections:

```powershell
docker compose logs postgres
```

Look for:
```text
database system is ready to accept connections
```

> [!NOTE]
> **Do NOT manually create database tables.** Spring Boot executes Flyway migrations on startup (`V1__initial_schema.sql`), automatically creating all 10 tables, indices, and constraints.

---

## Step 5 — Start Spring Boot Backend

Open a terminal in the `backend` folder and run the application via Maven:

```powershell
cd backend
mvn spring-boot:run
```

During startup, Spring Boot will:
1. Compile backend sources.
2. Connect to PostgreSQL on `localhost:5432`.
3. Execute Flyway migration `V1__initial_schema.sql`.
4. Validate JPA entities and Hibernate mappings.
5. Seed initial stations (`Bharati`, `Maitri`, `Himadri`), catalogue items, and stock balances via `DataSeeder.java`.
6. Start embedded Apache Tomcat on port `8080`.

Successful startup will finish with logs similar to:
```text
Tomcat started on port 8080 (http) with context path '/'
Started PolarOpsApplication in 4.5 seconds (process running for 5.1)
```

> [!IMPORTANT]
> **Keep this terminal running.** Do not close this window.

---

## Step 6 — Verify Backend Health

Open a **NEW terminal window** and run:

**Windows PowerShell**:
```powershell
curl.exe -s http://localhost:8080/api/health
```

**macOS / Linux**:
```bash
curl -s http://localhost:8080/api/health
```

Expected JSON response:
```json
{"service":"polarops-backend","status":"UP"}
```

An HTTP 200 status confirms the backend is healthy and responding.

---

## Step 7 — Verify Application API & Database Connectivity

Verify that Spring Boot successfully communicates with PostgreSQL by querying the item master catalogue:

```powershell
curl.exe -s http://localhost:8080/api/inventory/items
```

Expected output: A JSON array of seeded inventory items (`FUEL-DIESEL-A`, `FOOD-RATION-STD`, `MED-TRAUMA-KIT`, `BATTERY-12V`).

---

## Step 8 — Install Frontend Dependencies

Open a **NEW terminal window**, navigate to the `frontend` directory, and install npm packages:

```powershell
cd frontend
npm install
```

This installs React 19, Vite, Tailwind CSS v4, Lucide icons, Dexie, React Router, Recharts, and Leaflet.

---

## Step 9 — Configure Frontend API URL

Inspect `frontend/vite.config.ts`. In development mode, Vite includes a built-in reverse proxy:

```typescript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

In `frontend/src/api/client.ts`, `API_BASE_URL` reads `import.meta.env.VITE_API_BASE_URL || ''`.
- **For standard local development**: Leave `VITE_API_BASE_URL` blank. All API requests use the relative path `/api/*` and are proxied automatically to `http://localhost:8080` by Vite. No extra configuration is needed.

---

## Step 10 — Start Frontend

In the `frontend` terminal, start the Vite development server:

```powershell
npm run dev
```

Expected output:
```text
  VITE v8.3.0  ready in 240 ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
```

Open your browser and navigate to:
**[http://localhost:5173](http://localhost:5173)**

---

## Step 11 — Verify Full Application

You now have 3 active processes:
- **Terminal 1**: PostgreSQL running in Docker (`polarops-postgres`).
- **Terminal 2**: Spring Boot running on `http://localhost:8080`.
- **Terminal 3**: React/Vite running on `http://localhost:5173`.

In your browser, you should see the PolarOps operational dashboard featuring:
- Active station selector in the top navigation bar (`Bharati Station (BHA)` selected by default).
- Station GPS coordinates and interactive Antarctic map.
- Fuel and critical resource KPI summary cards.
- Stock risk assessment charts.
- Top navigation links to: **Dashboard**, **Inventory**, **Intelligence**, **Cargo**, **Personnel**, and **Emergency**.
- **SIH Demo Guide** walkthrough modal button.
- **Simulate Offline** connectivity toggle and sync queue drawer.

---

# Running the Application After the First Setup

Once initial setup and `npm install` are complete, daily startup only takes 3 commands:

```powershell
# Terminal 1: Start PostgreSQL
docker compose up -d postgres

# Terminal 2: Start Backend
cd backend
mvn spring-boot:run

# Terminal 3: Start Frontend
cd frontend
npm run dev
```

Then visit `http://localhost:5173`.

---

# 🐳 Running with Docker Compose (Full Stack Mode)

PolarOps includes a production-grade `docker-compose.yml` that builds and launches the entire stack (PostgreSQL, Spring Boot Backend, and Frontend behind Nginx) in one command:

```powershell
docker compose up --build -d
```

Check the status of all three services:
```powershell
docker compose ps
```

Expected output:
```text
NAME                IMAGE                 COMMAND                  SERVICE    STATUS
polarops-backend    nextgensih-backend    "java -jar app.jar"      backend    Up (healthy)
polarops-frontend   nextgensih-frontend   "/docker-entrypoint.…"   frontend   Up
polarops-postgres   postgres:16-alpine    "docker-entrypoint.s…"   postgres   Up (healthy)
```

Access points:
- **Web UI (Frontend + Nginx Reverse Proxy)**: [http://localhost](http://localhost) (Port 80)
- **Direct Backend API**: [http://localhost:8080/api/health](http://localhost:8080/api/health)
- **PostgreSQL Database**: `localhost:5432`

### Viewing Logs
```powershell
# Tail all container logs
docker compose logs -f

# Tail backend logs only
docker compose logs -f backend

# Tail frontend logs only
docker compose logs -f frontend
```

### Stopping Docker Stack
```powershell
docker compose down
```

> [!CAUTION]
> **Do NOT use `docker compose down -v`** unless you explicitly intend to wipe the PostgreSQL database volume and all stored records.

---

# 🧪 Testing & Verification

### Backend Automated Test Suite
The Spring Boot backend contains 76 automated unit, slice, and integration tests across all domains:
- `CargoControllerTest` & `CargoServiceTest` (Consignment workflows, atomic inventory receipts)
- `InventoryControllerTest` & `InventoryServiceTest` (Ledger operations, stock constraints)
- `InventoryIntelligenceServiceTest` (Deterministic forecasting, edge cases, transfer proposals)
- `SyncControllerTest` & `SyncServiceTest` (Idempotent sync engine, duplicate replay protection)
- `PolarOpsApplicationTests` (Spring application context loading)

Run the backend test suite:
```powershell
cd backend
mvn clean test
```
*Current verified status: `Tests run: 76, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`).*

### Frontend Production Build
Validate TypeScript types and compile production assets:
```powershell
cd frontend
npm run build
```
*Current verified status: 0 TypeScript errors. Output generated in `frontend/dist/` with PWA service worker `sw.js`.*

### Frontend Preview Server
To preview the compiled production frontend build locally:
```powershell
npm run preview
```

---

# 📦 Production Build Artifacts

- **Backend JAR**:
  ```powershell
  cd backend
  mvn clean package -DskipTests
  ```
  Generates executable JAR: `backend/target/polarops-backend-0.0.1-SNAPSHOT.jar`. Run with:
  ```powershell
  java -jar target/polarops-backend-0.0.1-SNAPSHOT.jar
  ```
- **Frontend Static Distribution**:
  ```powershell
  cd frontend
  npm run build
  ```
  Generates static distribution in `frontend/dist/`, ready to be served by Nginx, Cloudflare Pages, or Render.

---

# 🔌 API Documentation

All endpoints are prefixed with `/api`.

### 1. Health Endpoint
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/health` | Service health status (`{"status":"UP","service":"polarops-backend"}`) |

### 2. Inventory Domain
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/inventory/items` | List all item master catalogue records |
| `POST` | `/api/inventory/items` | Register a new master catalogue item |
| `GET` | `/api/inventory?stationId={uuid}` | List current stock levels for a specific station |
| `POST` | `/api/inventory/stock-levels` | Initialize a stock level balance for an item at a station |
| `POST` | `/api/inventory/transactions` | Post an immutable ledger transaction (`CONSUMPTION`, `RECEIPT`, `TRANSFER_OUT`, etc.) |

### 3. Inventory Intelligence Engine
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/inventory/intelligence?stationId={uuid}&delayDays={int}` | Evaluate deterministic stock risk report, days to safety, and transfer recommendations |

### 4. Cargo Domain
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/cargo?stationId={uuid}` | List cargo consignments (optionally filtered by destination station) |
| `GET` | `/api/cargo/{id}` | Retrieve single consignment manifest and item details |
| `POST` | `/api/cargo` | Create a new cargo consignment with manifest items |
| `POST` | `/api/cargo/{id}/items` | Add items to an existing cargo manifest |
| `POST` | `/api/cargo/{id}/receive` | Atomically convert an `ARRIVED` cargo consignment to `RECEIVED` and post inventory receipts |

### 5. Personnel & Sortie Domain
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/personnel?stationId={uuid}` | List all personnel stationed at a given station |
| `POST` | `/api/personnel` | Add a new crew member to a station roster |
| `GET` | `/api/personnel/{id}/movements` | List movement history for a crew member |
| `POST` | `/api/personnel/{id}/movements` | Record a sortie movement (`SORTIE_DEPARTURE`, `SORTIE_RETURN`, etc.) |

### 6. Emergency & Crisis Domain
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/emergency/alerts?stationId={uuid}&resolved={bool}` | List station alerts (filterable by active/resolved) |
| `POST` | `/api/emergency/alerts` | Broadcast an emergency alert (`BLIZZARD_WARNING`, `SOS`, etc.) |
| `POST` | `/api/emergency/alerts/{id}/resolve` | Mark an alert incident as resolved |

### 7. Offline Synchronization Gateway
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/sync/push` | Batch process offline client mutations with duplicate idempotency checks |

---

# 📴 Offline Mode & Store-and-Forward Engine

Polar research stations frequently lose satellite connectivity during blizzards and geomagnetic storms. PolarOps implements an offline architecture to ensure continuous operational capability:

1. **Local Mutation Queuing**:
   - Every browser client initializes a permanent installation UUID stored in IndexedDB (`getOrCreateDeviceId()`).
   - When offline (`isOnline === false`), transactions are recorded in IndexedDB via Dexie (`localDb.pendingOperations`).
   - Each mutation is assigned a random `clientTransactionId` UUID.
2. **Replay & Batch Sync (`POST /api/sync/push`)**:
   - When connectivity is restored, the `SyncManager` service automatically gathers pending items ordered by `createdAt` FIFO.
   - Pushes batch payload: `{ operations: [...] }` to the backend.
3. **Idempotency Guarantees**:
   - The backend checks `change_events` for the unique `client_transaction_id`.
   - If previously processed, the backend returns status `DUPLICATE` and skips mutating stock levels.
   - If new, the transaction executes in a dedicated `@Transactional(propagation = Propagation.REQUIRES_NEW)` boundary, applies the mutation, records the audit row in `change_events`, and returns `APPLIED`.
   - If invalid, returns `REJECTED` without rolling back preceding independent transactions.

### Simulating Offline Mode in the UI
In the header bar, click the **"Simulate Offline"** button. The connectivity pill turns amber (`OFFLINE`). Any inventory transaction logged in this mode is saved to the client queue without failing, and syncs automatically when clicking **"Restore Online"**.

---

# 🚢 Cargo → Inventory Integration

Cargo receipt directly updates station inventory through an atomic transaction:

1. An expedition consignment is created (`PLANNED` $\rightarrow$ `IN_TRANSIT`).
2. When the vessel arrives at the polar ice shelf, status updates to `ARRIVED`.
3. In the Cargo UI, clicking **"Receive into Station Inventory"** calls `POST /api/cargo/{id}/receive`.
4. In `CargoService.receiveCargo()`:
   - Validates consignment is in `ARRIVED` status.
   - Changes status to `RECEIVED` and records `actualArrival` timestamp.
   - For every item on the consignment manifest:
     - Creates an `InventoryTransaction` with type `RECEIPT` and source `CARGO_RECEIPT`.
     - References `consignment.id` in `referenceId`.
     - Atomically increments station `StockLevel.currentStock`.
5. Both status change and inventory additions succeed or fail together within a single transaction boundary.

---

# 🧠 Deterministic Inventory Intelligence Engine

The intelligence service (`InventoryIntelligenceService.java`) operates on pure mathematical models without probabilistic guesswork:

### 1. Resupply Lead Time & Shortage Projection
$$\text{effectiveResupplyDate} = \text{nextResupplyDate} + \text{delayDays}$$
$$\text{daysUntilResupply} = \max(0, \text{days between evaluationDate and effectiveResupplyDate})$$
$$\text{projectedConsumption} = \text{dailyConsumption} \times \text{daysUntilResupply}$$
$$\text{projectedStockAtResupply} = \text{currentStock} - \text{projectedConsumption}$$

### 2. Days to Safety Threshold
$$\text{daysToSafety} = \frac{\text{currentStock} - \text{safetyStock}}{\text{dailyConsumption}}$$

### 3. Risk Classifications
- **`CRITICAL`**: If $\text{currentStock} \le \text{safetyStock}$ OR $\text{projectedStockAtResupply} < \text{safetyStock}$.
- **`WARNING`**: If $\text{projectedStockAtResupply} < \text{reorderPoint}$.
- **`HEALTHY`**: All other conditions.
- **Zero Consumption Handling**: If $\text{dailyConsumption} == 0$, projected stock remains constant, avoiding divide-by-zero errors.

### 4. Inter-Station Transfer Recommendations
When an item is classified as `CRITICAL`, the engine scans all peer stations:
- Calculates surplus: $\text{surplus} = \text{peer.currentStock} - \text{peer.safetyStock}$.
- Recommends transferring $\min(\text{deficit}, \text{surplus})$ from the peer station with the highest surplus.

---

# 🗺️ Frontend Routes

| Route | Page Component | Purpose |
|---|---|---|
| `/` | `DashboardPage.tsx` | Overview map, key metrics, stock risk chart, and mutual aid proposals |
| `/inventory` | `InventoryPage.tsx` | Real-time stock levels, safety indicators, and mutation logging modal |
| `/intelligence` | `IntelligencePage.tsx` | Resupply delay simulation (+0 to +30 days) and transfer recommendations |
| `/cargo` | `CargoPage.tsx` | Consignment manifests, creation modal, and atomic cargo receipt |
| `/personnel` | `PersonnelPage.tsx` | Station crew roster, medical status, and sortie movement logging |
| `/emergency` | `EmergencyPage.tsx` | Station emergency alert ledger, broadcast modal, and resolution |

---

# 🗄️ Database & Seed Data

- **Database Engine**: PostgreSQL 16
- **Database Name**: `polarops`
- **Migration Engine**: Flyway
- **Migration Script**: `backend/src/main/resources/db/migration/V1__initial_schema.sql`

### Seeded Polar Stations
On boot, `DataSeeder.java` seeds 3 Indian polar expedition stations:

| Station Name | Code | Station UUID | Coordinates | Purpose |
|---|---|---|---|---|
| **Bharati Station** | `BHA` | `22222222-2222-2222-2222-222222222222` | 69.4075° S, 76.1872° E | Primary demo station (Larsemann Hills, Antarctica) |
| **Maitri Station** | `MAI` | `11111111-1111-1111-1111-111111111111` | 70.7667° S, 11.7333° E | Inland Antarctic station with fuel surplus |
| **Himadri Station** | `HIM` | `33333333-3333-3333-3333-333333333333` | 78.9236° N, 11.9278° E | Arctic research station (Ny-Ålesund, Svalbard) |

### Seeded Inventory Master Items
- `FUEL-DIESEL-A`: Polar Diesel Arctic Grade (SAB) (LITERS)
- `FOOD-RATION-STD`: Standard Polar Expedition Rations (KG)
- `MED-TRAUMA-KIT`: Emergency Medical Trauma Packs (BOXES)
- `BATTERY-12V`: LiFePO4 Low-Temp Deep Cycle Battery (UNITS)

### Seeded Cargo Consignment
- Consignment `VOY-2026-BHA-004` addressed to **Bharati Station** in `ARRIVED` status carrying 3,000 L of Arctic Diesel and 500 kg of Rations, ready for immediate receipt demonstration.

---

# 🚨 Troubleshooting

### 1. PostgreSQL connection refused (`Connection to localhost:5432 refused`)
- **Cause**: The PostgreSQL Docker container is not running.
- **Solution**:
  ```powershell
  docker compose up -d postgres
  docker compose ps
  ```
  Wait until status displays `(healthy)`, then restart the Spring Boot backend.

### 2. PostgreSQL status shows `health: starting`
- **Cause**: PostgreSQL is running initial health checks.
- **Solution**: This is normal during the first 5–10 seconds. Re-run `docker compose ps` until it shows `healthy`.

### 3. Port 5432 already in use
- **Cause**: Another local PostgreSQL instance is running on your machine.
- **Solution**: Find the occupying process:
  ```powershell
  netstat -ano | findstr :5432
  ```
  Stop the existing local PostgreSQL service via Windows Services (`services.msc`), or change `POSTGRES_PORT` in `.env` to `5433` and update `SPRING_DATASOURCE_URL`.

### 4. Port 8080 already in use
- **Cause**: Another application or previous Spring Boot run is occupying port 8080.
- **Solution**:
  ```powershell
  netstat -ano | findstr :8080
  ```
  Identify the Process ID (PID) in the right-most column, and terminate it:
  ```powershell
  Stop-Process -Id <PID> -Force
  ```

### 5. Frontend cannot connect to backend
- **Checklist**:
  1. Verify backend health endpoint responds:
     ```powershell
     curl.exe http://localhost:8080/api/health
     ```
  2. If running via Vite, make sure `VITE_API_BASE_URL` in `.env` is empty so requests route through Vite's proxy.
  3. Open browser Developer Tools (`F12`), check the **Console** and **Network** tabs for failed requests.

### 6. Docker daemon is not running
- **Error**: `failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine`
- **Solution**: Launch **Docker Desktop** from the Start Menu, wait until the Docker icon shows "Engine running", then re-run `docker compose up -d postgres`.

### 7. Java version mismatch
- **Error**: `Fatal error compiling: error: release version 21 not supported`
- **Solution**: PolarOps requires **Java 21 LTS**. Verify with `java -version`. Ensure `JAVA_HOME` points to your JDK 21 installation.

### 8. Node or npm build failures
- **Solution**: Clear local modules and re-install:
  ```powershell
  cd frontend
  Remove-Item -Recurse -Force node_modules, package-lock.json
  npm install
  npm run build
  ```

### 9. Flyway migration failure
- **Error**: `FlywayException: Validate failed: Migrations have failed validation`
- **Solution**: Check backend logs to see if tables were modified outside Flyway. In local development, you can reset the database volume:
  ```powershell
  docker compose down -v
  docker compose up -d postgres
  ```
  Then restart `mvn spring-boot:run`.

---

# 🛑 Shutdown

### Development Mode Shutdown
1. In the **Frontend terminal**: Press `Ctrl+C` to stop Vite.
2. In the **Backend terminal**: Press `Ctrl+C` to stop Spring Boot.
3. In your main terminal, stop the database:
   ```powershell
   docker compose stop postgres
   ```

### Docker Compose Mode Shutdown
```powershell
docker compose down
```

> [!NOTE]
> Database records are persisted in the Docker volume `postgres_data`. When you start the containers again, all inventory, cargo, and movement data remain intact.

---

# 🔁 Restarting the Project

To resume working after having previously completed installation:

```powershell
# 1. Start PostgreSQL
docker compose up -d postgres

# 2. In Terminal A: Start Backend
cd backend
mvn spring-boot:run

# 3. In Terminal B: Start Frontend
cd frontend
npm run dev
```
Open **[http://localhost:5173](http://localhost:5173)** in your browser.

---

# 🎯 SIH Demonstration Workflow

Follow this sequence to evaluate the system:

1. **Dashboard Overview**:
   - Open `http://localhost:5173`.
   - Bharati Station (`BHA`) is selected. Notice the interactive map, fuel stock KPI (8,420 L), and safe stock indicators.
2. **Deterministic Shortage Prediction**:
   - Navigate to the **Intelligence** page (`/intelligence`).
   - Adjust the **Simulated Resupply Delay** slider from `0 Days` to `+10 Days`.
   - Notice Fuel Risk instantly switches to `CRITICAL` with projected safety stock breach.
   - Observe the **Mutual-Aid Transfer Recommendation**: The engine recommends transferring surplus fuel from **Maitri Station**.
3. **Atomic Cargo Receipt**:
   - Navigate to the **Cargo** page (`/cargo`).
   - Locate consignment `VOY-2026-BHA-004` (status: `ARRIVED`, carrying 3,000 L of Arctic Diesel).
   - Click **"Receive into Station Inventory"**.
   - Confirm receipt: Status changes to `RECEIVED`.
   - Navigate to **Inventory** (`/inventory`): Fuel stock is updated from 8,420 L to 11,420 L.
4. **Store-and-Forward Offline Sync**:
   - Click **"Simulate Offline"** in the top navigation header.
   - Navigate to **Inventory** (`/inventory`), select an item, and log a `CONSUMPTION` transaction.
   - The UI confirms the action is saved locally in IndexedDB.
   - Click the **Offline Queue** pill in the header to view the pending mutation.
   - Click **"Restore Online"** in the header.
   - The sync manager dispatches the queue to `POST /api/sync/push`. The queue clears and the backend ledger records the transaction.
5. **Sortie & Emergency Management**:
   - Explore **Personnel** (`/personnel`) to log sortie movements.
   - Explore **Emergency** (`/emergency`) to broadcast and resolve station crisis alerts.

---

# 🚧 Current Scope vs. Future Roadmap

### Currently Implemented (SIH 2026 Prototype)
- Fully functioning modular monolith backend with Spring Boot 3.3.4, Java 21, and PostgreSQL 16.
- Flyway automated schema migrations and deterministic data seeder.
- Mathematical Inventory Intelligence engine with shortage and transfer forecasting.
- Atomic cargo consignment receipt into immutable inventory transactions.
- Dexie-backed offline queue with device UUID tracking and duplicate idempotency.
- Progressive Web App with service worker precaching and installable manifest.
- Complete responsive frontend UI covering all operational modules.
- Multi-container Docker Compose configuration for production simulation.

### Future Roadmap
- Hardware RFID and BLE gateway integration for automated warehouse cargo scanning.
- Direct Iridium and INMARSAT low-bandwidth satellite packet telemetry compression.
- Dedicated Arctic/Antarctic icebreaker vessel AIS position tracking and route optimization.
- Integration with NCPOR (National Centre for Polar and Ocean Research) enterprise databases.

---

# 🎓 Smart India Hackathon 2026

- **Problem Statement ID**: `SIH26062`
- **Problem Statement Title**: Integrated Polar Expedition Logistics & Asset Management System
- **Organization**: Ministry of Earth Sciences / NCPOR
- **Category**: Smart Automation
- **Domain**: Software
- **GitHub Repository**: [https://github.com/RishabhandSven/Polar-Operations.git](https://github.com/RishabhandSven/Polar-Operations.git)
