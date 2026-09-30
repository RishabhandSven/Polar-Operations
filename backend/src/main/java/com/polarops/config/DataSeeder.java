package com.polarops.config;

import com.polarops.cargo.CargoConsignmentEntity;
import com.polarops.cargo.CargoConsignmentRepository;
import com.polarops.cargo.CargoItemEntity;
import com.polarops.cargo.CargoItemRepository;
import com.polarops.expedition.StationEntity;
import com.polarops.expedition.StationRepository;
import com.polarops.inventory.InventoryItemEntity;
import com.polarops.inventory.InventoryItemRepository;
import com.polarops.inventory.StockLevelEntity;
import com.polarops.inventory.StockLevelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    // Fixed deterministic UUIDs for core stations
    public static final UUID MAITRI_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID BHARATI_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID HIMADRI_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    // Fixed deterministic UUIDs for core inventory items
    public static final UUID ITEM_FUEL_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    public static final UUID ITEM_FOOD_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    public static final UUID ITEM_MED_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    public static final UUID ITEM_BATT_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    // Fixed deterministic UUIDs for sample cargo consignments
    public static final UUID CARGO_BHARATI_ARRIVED_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    public static final UUID CARGO_MAITRI_IN_TRANSIT_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    private final StationRepository stationRepository;
    private final InventoryItemRepository itemRepository;
    private final StockLevelRepository stockLevelRepository;
    private final CargoConsignmentRepository consignmentRepository;
    private final CargoItemRepository cargoItemRepository;

    public DataSeeder(StationRepository stationRepository,
                      InventoryItemRepository itemRepository,
                      StockLevelRepository stockLevelRepository,
                      CargoConsignmentRepository consignmentRepository,
                      CargoItemRepository cargoItemRepository) {
        this.stationRepository = stationRepository;
        this.itemRepository = itemRepository;
        this.stockLevelRepository = stockLevelRepository;
        this.consignmentRepository = consignmentRepository;
        this.cargoItemRepository = cargoItemRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedStations();
        seedInventoryItems();
        seedStockLevels();
        seedCargoConsignments();
    }

    private void seedStations() {
        if (stationRepository.count() == 0) {
            log.info("Seeding initial 3 polar stations (Maitri, Bharati, Himadri)...");

            List<StationEntity> stations = List.of(
                    new StationEntity(
                            MAITRI_ID,
                            "MAI",
                            "Maitri Station",
                            new BigDecimal("-70.766700"),
                            new BigDecimal("11.733300"),
                            "OPERATIONAL"
                    ),
                    new StationEntity(
                            BHARATI_ID,
                            "BHA",
                            "Bharati Station",
                            new BigDecimal("-69.407500"),
                            new BigDecimal("76.187200"),
                            "OPERATIONAL"
                    ),
                    new StationEntity(
                            HIMADRI_ID,
                            "HIM",
                            "Himadri Station",
                            new BigDecimal("78.923600"),
                            new BigDecimal("11.927800"),
                            "OPERATIONAL"
                    )
            );

            stationRepository.saveAll(stations);
            log.info("Successfully seeded {} polar stations.", stations.size());
        }
    }

    private void seedInventoryItems() {
        if (itemRepository.count() == 0) {
            log.info("Seeding core inventory master items...");

            List<InventoryItemEntity> items = List.of(
                    new InventoryItemEntity(
                            ITEM_FUEL_ID,
                            "FUEL-DIESEL-A",
                            "Polar Diesel Arctic Grade (SAB)",
                            "FUEL",
                            "LITERS",
                            "Special Antarctic blend low-pour diesel fuel for generators and heating plants"
                    ),
                    new InventoryItemEntity(
                            ITEM_FOOD_ID,
                            "FOOD-RATION-STD",
                            "Standard Polar Expedition Rations",
                            "FOOD",
                            "KG",
                            "High-caloric nutrient-dense balanced ration packs for winter-over personnel"
                    ),
                    new InventoryItemEntity(
                            ITEM_MED_ID,
                            "MED-TRAUMA-KIT",
                            "Emergency Medical Trauma Packs",
                            "MEDICAL",
                            "BOXES",
                            "Advanced hypothermia and surgical trauma response kits"
                    ),
                    new InventoryItemEntity(
                            ITEM_BATT_ID,
                            "BATTERY-12V",
                            "LiFePO4 Low-Temp Deep Cycle Battery",
                            "BATTERIES",
                            "UNITS",
                            "Extreme cold weather rated 12V 100Ah station auxiliary batteries"
                    )
            );

            itemRepository.saveAll(items);
            log.info("Successfully seeded {} inventory items.", items.size());
        }
    }

    private void seedStockLevels() {
        if (stockLevelRepository.count() == 0) {
            log.info("Seeding station-specific stock levels...");
            LocalDate today = LocalDate.now();

            List<StockLevelEntity> stockLevels = List.of(
                    // Bharati Stock Levels (Configured for Demo Scenario: 8420L Fuel, 420L/day, 2000L safety, 23 days resupply)
                    new StockLevelEntity(
                            UUID.randomUUID(),
                            BHARATI_ID,
                            ITEM_FUEL_ID,
                            new BigDecimal("8420.00"),
                            new BigDecimal("420.00"),
                            new BigDecimal("2000.00"),
                            new BigDecimal("4000.00"),
                            21,
                            today.plusDays(23)
                    ),
                    new StockLevelEntity(
                            UUID.randomUUID(),
                            BHARATI_ID,
                            ITEM_FOOD_ID,
                            new BigDecimal("2400.00"),
                            new BigDecimal("45.00"),
                            new BigDecimal("500.00"),
                            new BigDecimal("1000.00"),
                            21,
                            today.plusDays(30)
                    ),
                    new StockLevelEntity(
                            UUID.randomUUID(),
                            BHARATI_ID,
                            ITEM_MED_ID,
                            new BigDecimal("40.00"),
                            new BigDecimal("0.20"),
                            new BigDecimal("10.00"),
                            new BigDecimal("20.00"),
                            30,
                            today.plusDays(45)
                    ),
                    new StockLevelEntity(
                            UUID.randomUUID(),
                            BHARATI_ID,
                            ITEM_BATT_ID,
                            new BigDecimal("50.00"),
                            new BigDecimal("0.50"),
                            new BigDecimal("15.00"),
                            new BigDecimal("25.00"),
                            30,
                            today.plusDays(45)
                    ),

                    // Maitri Stock Levels (Configured with Fuel Surplus: 18500L Fuel, 380L/day, 2500L safety, 15 days resupply)
                    new StockLevelEntity(
                            UUID.randomUUID(),
                            MAITRI_ID,
                            ITEM_FUEL_ID,
                            new BigDecimal("18500.00"),
                            new BigDecimal("380.00"),
                            new BigDecimal("2500.00"),
                            new BigDecimal("5000.00"),
                            14,
                            today.plusDays(15)
                    ),
                    new StockLevelEntity(
                            UUID.randomUUID(),
                            MAITRI_ID,
                            ITEM_FOOD_ID,
                            new BigDecimal("3200.00"),
                            new BigDecimal("50.00"),
                            new BigDecimal("600.00"),
                            new BigDecimal("1200.00"),
                            14,
                            today.plusDays(20)
                    ),
                    new StockLevelEntity(
                            UUID.randomUUID(),
                            MAITRI_ID,
                            ITEM_MED_ID,
                            new BigDecimal("60.00"),
                            new BigDecimal("0.25"),
                            new BigDecimal("15.00"),
                            new BigDecimal("25.00"),
                            20,
                            today.plusDays(30)
                    ),
                    new StockLevelEntity(
                            UUID.randomUUID(),
                            MAITRI_ID,
                            ITEM_BATT_ID,
                            new BigDecimal("75.00"),
                            new BigDecimal("0.40"),
                            new BigDecimal("20.00"),
                            new BigDecimal("35.00"),
                            20,
                            today.plusDays(30)
                    ),

                    // Himadri Stock Levels
                    new StockLevelEntity(
                            UUID.randomUUID(),
                            HIMADRI_ID,
                            ITEM_FUEL_ID,
                            new BigDecimal("5000.00"),
                            new BigDecimal("120.00"),
                            new BigDecimal("1000.00"),
                            new BigDecimal("2000.00"),
                            10,
                            today.plusDays(25)
                    ),
                    new StockLevelEntity(
                            UUID.randomUUID(),
                            HIMADRI_ID,
                            ITEM_FOOD_ID,
                            new BigDecimal("1100.00"),
                            new BigDecimal("25.00"),
                            new BigDecimal("250.00"),
                            new BigDecimal("500.00"),
                            10,
                            today.plusDays(25)
                    ),
                    new StockLevelEntity(
                            UUID.randomUUID(),
                            HIMADRI_ID,
                            ITEM_MED_ID,
                            new BigDecimal("25.00"),
                            new BigDecimal("0.10"),
                            new BigDecimal("5.00"),
                            new BigDecimal("10.00"),
                            14,
                            today.plusDays(30)
                    ),
                    new StockLevelEntity(
                            UUID.randomUUID(),
                            HIMADRI_ID,
                            ITEM_BATT_ID,
                            new BigDecimal("30.00"),
                            new BigDecimal("0.20"),
                            new BigDecimal("8.00"),
                            new BigDecimal("15.00"),
                            14,
                            today.plusDays(30)
                    )
            );

            stockLevelRepository.saveAll(stockLevels);
            log.info("Successfully seeded {} station stock levels.", stockLevels.size());
        }
    }

    private void seedCargoConsignments() {
        if (consignmentRepository.count() == 0) {
            log.info("Seeding sample cargo consignments...");
            LocalDate today = LocalDate.now();

            CargoConsignmentEntity arrivedConsignment = new CargoConsignmentEntity(
                    CARGO_BHARATI_ARRIVED_ID,
                    "VOY-2026-BHA-004",
                    BHARATI_ID,
                    "ARRIVED",
                    today.minusDays(18),
                    today,
                    null
            );

            CargoConsignmentEntity inTransitConsignment = new CargoConsignmentEntity(
                    CARGO_MAITRI_IN_TRANSIT_ID,
                    "VOY-2026-MAI-002",
                    MAITRI_ID,
                    "IN_TRANSIT",
                    today.minusDays(5),
                    today.plusDays(10),
                    null
            );

            consignmentRepository.saveAll(List.of(arrivedConsignment, inTransitConsignment));

            List<CargoItemEntity> items = List.of(
                    new CargoItemEntity(
                            UUID.randomUUID(),
                            CARGO_BHARATI_ARRIVED_ID,
                            ITEM_FUEL_ID,
                            new BigDecimal("3000.00")
                    ),
                    new CargoItemEntity(
                            UUID.randomUUID(),
                            CARGO_BHARATI_ARRIVED_ID,
                            ITEM_FOOD_ID,
                            new BigDecimal("500.00")
                    ),
                    new CargoItemEntity(
                            UUID.randomUUID(),
                            CARGO_MAITRI_IN_TRANSIT_ID,
                            ITEM_FUEL_ID,
                            new BigDecimal("2000.00")
                    ),
                    new CargoItemEntity(
                            UUID.randomUUID(),
                            CARGO_MAITRI_IN_TRANSIT_ID,
                            ITEM_MED_ID,
                            new BigDecimal("20.00")
                    )
            );

            cargoItemRepository.saveAll(items);
            log.info("Successfully seeded 2 cargo consignments with 4 manifest items.");
        }
    }
}
