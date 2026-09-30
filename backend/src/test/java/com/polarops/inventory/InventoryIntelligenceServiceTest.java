package com.polarops.inventory;

import com.polarops.config.DataSeeder;
import com.polarops.inventory.InventoryDtos.StationIntelligenceReport;
import com.polarops.inventory.InventoryDtos.StockRiskAssessment;
import com.polarops.inventory.InventoryDtos.TransferRecommendation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class InventoryIntelligenceServiceTest {

    @Autowired
    private InventoryIntelligenceService intelligenceService;

    @Autowired
    private StockLevelRepository stockLevelRepository;

    @Autowired
    private InventoryItemRepository itemRepository;

    private final LocalDate evaluationDate = LocalDate.now();

    @BeforeEach
    void resetSeededStockLevels() {
        // Reset Bharati fuel to standard demo values: 8420L current, 420L daily, 2000L safety, 4000L reorder, 23 days
        StockLevelEntity bharatiFuel = stockLevelRepository.findByStationIdAndInventoryItemId(DataSeeder.BHARATI_ID, DataSeeder.ITEM_FUEL_ID).orElseThrow();
        bharatiFuel.setCurrentStock(new BigDecimal("8420.00"));
        bharatiFuel.setDailyConsumption(new BigDecimal("420.00"));
        bharatiFuel.setSafetyStock(new BigDecimal("2000.00"));
        bharatiFuel.setReorderPoint(new BigDecimal("4000.00"));
        bharatiFuel.setNextResupplyDate(evaluationDate.plusDays(23));
        stockLevelRepository.save(bharatiFuel);

        // Reset Maitri fuel: 18500L current, 380L daily, 2500L safety, 5000L reorder, 15 days
        StockLevelEntity maitriFuel = stockLevelRepository.findByStationIdAndInventoryItemId(DataSeeder.MAITRI_ID, DataSeeder.ITEM_FUEL_ID).orElseThrow();
        maitriFuel.setCurrentStock(new BigDecimal("18500.00"));
        maitriFuel.setDailyConsumption(new BigDecimal("380.00"));
        maitriFuel.setSafetyStock(new BigDecimal("2500.00"));
        maitriFuel.setReorderPoint(new BigDecimal("5000.00"));
        maitriFuel.setNextResupplyDate(evaluationDate.plusDays(15));
        stockLevelRepository.save(maitriFuel);
    }

    @Test
    void test1_HealthyStock() {
        UUID testStationId = DataSeeder.HIMADRI_ID;
        UUID testItemId = DataSeeder.ITEM_BATT_ID;

        StockLevelEntity sl = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testItemId).orElseThrow();
        sl.setCurrentStock(new BigDecimal("100.00"));
        sl.setDailyConsumption(new BigDecimal("1.00"));
        sl.setSafetyStock(new BigDecimal("10.00"));
        sl.setReorderPoint(new BigDecimal("20.00"));
        sl.setNextResupplyDate(evaluationDate.plusDays(10));
        stockLevelRepository.save(sl);

        StationIntelligenceReport report = intelligenceService.evaluateStationIntelligence(testStationId, 0, evaluationDate);
        StockRiskAssessment batt = report.assessments().stream()
                .filter(a -> a.inventoryItemId().equals(testItemId))
                .findFirst().orElseThrow();

        assertThat(batt.riskLevel()).isEqualTo(RiskLevel.HEALTHY);
        assertThat(batt.projectedStockAtResupply()).isEqualByComparingTo("90.00");
        assertThat(batt.explanation()).contains("remains above safety buffer");
    }

    @Test
    void test2_WarningStock() {
        UUID testStationId = DataSeeder.HIMADRI_ID;
        UUID testItemId = DataSeeder.ITEM_BATT_ID;

        StockLevelEntity sl = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testItemId).orElseThrow();
        sl.setCurrentStock(new BigDecimal("25.00"));
        sl.setDailyConsumption(new BigDecimal("1.00"));
        sl.setSafetyStock(new BigDecimal("10.00"));
        sl.setReorderPoint(new BigDecimal("20.00"));
        sl.setNextResupplyDate(evaluationDate.plusDays(10));
        stockLevelRepository.save(sl);

        StationIntelligenceReport report = intelligenceService.evaluateStationIntelligence(testStationId, 0, evaluationDate);
        StockRiskAssessment batt = report.assessments().stream()
                .filter(a -> a.inventoryItemId().equals(testItemId))
                .findFirst().orElseThrow();

        // Projected: 25 - 10 = 15. Since 10 <= 15 < 20 (reorder point), risk is WARNING
        assertThat(batt.riskLevel()).isEqualTo(RiskLevel.WARNING);
        assertThat(batt.projectedStockAtResupply()).isEqualByComparingTo("15.00");
        assertThat(batt.explanation()).contains("will drop below reorder point");
    }

    @Test
    void test3_CriticalProjectedBreach() {
        UUID testStationId = DataSeeder.HIMADRI_ID;
        UUID testItemId = DataSeeder.ITEM_BATT_ID;

        StockLevelEntity sl = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testItemId).orElseThrow();
        sl.setCurrentStock(new BigDecimal("30.00"));
        sl.setDailyConsumption(new BigDecimal("2.00"));
        sl.setSafetyStock(new BigDecimal("10.00"));
        sl.setReorderPoint(new BigDecimal("20.00"));
        sl.setNextResupplyDate(evaluationDate.plusDays(15));
        stockLevelRepository.save(sl);

        // Projected: 30 - (2 * 15) = 0 < 10 (safety stock) -> CRITICAL
        StationIntelligenceReport report = intelligenceService.evaluateStationIntelligence(testStationId, 0, evaluationDate);
        StockRiskAssessment batt = report.assessments().stream()
                .filter(a -> a.inventoryItemId().equals(testItemId))
                .findFirst().orElseThrow();

        assertThat(batt.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(batt.daysToSafetyThreshold()).isEqualTo(10.0); // (30 - 10) / 2 = 10 days
        assertThat(batt.explanation()).contains("Safety threshold breached in 10.0 days");
    }

    @Test
    void test4_CriticalAlreadyBelowSafetyStock() {
        UUID testStationId = DataSeeder.HIMADRI_ID;
        UUID testItemId = DataSeeder.ITEM_BATT_ID;

        StockLevelEntity sl = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testItemId).orElseThrow();
        sl.setCurrentStock(new BigDecimal("8.00"));
        sl.setDailyConsumption(new BigDecimal("1.00"));
        sl.setSafetyStock(new BigDecimal("10.00"));
        sl.setReorderPoint(new BigDecimal("20.00"));
        sl.setNextResupplyDate(evaluationDate.plusDays(10));
        stockLevelRepository.save(sl);

        StationIntelligenceReport report = intelligenceService.evaluateStationIntelligence(testStationId, 0, evaluationDate);
        StockRiskAssessment batt = report.assessments().stream()
                .filter(a -> a.inventoryItemId().equals(testItemId))
                .findFirst().orElseThrow();

        assertThat(batt.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(batt.daysToSafetyThreshold()).isEqualTo(0.0);
        assertThat(batt.explanation()).contains("Stock is already at or below safety threshold");
    }

    @Test
    void test5_ZeroConsumptionAboveSafety() {
        UUID testStationId = DataSeeder.HIMADRI_ID;
        UUID testItemId = DataSeeder.ITEM_BATT_ID;

        StockLevelEntity sl = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testItemId).orElseThrow();
        sl.setCurrentStock(new BigDecimal("50.00"));
        sl.setDailyConsumption(BigDecimal.ZERO);
        sl.setSafetyStock(new BigDecimal("10.00"));
        sl.setNextResupplyDate(evaluationDate.plusDays(10));
        stockLevelRepository.save(sl);

        StationIntelligenceReport report = intelligenceService.evaluateStationIntelligence(testStationId, 0, evaluationDate);
        StockRiskAssessment batt = report.assessments().stream()
                .filter(a -> a.inventoryItemId().equals(testItemId))
                .findFirst().orElseThrow();

        assertThat(batt.riskLevel()).isEqualTo(RiskLevel.HEALTHY);
        assertThat(batt.daysToSafetyThreshold()).isNull();
        assertThat(batt.projectedStockAtResupply()).isEqualByComparingTo("50.00");
        assertThat(batt.explanation()).contains("Zero consumption rate; stock remains constant");
    }

    @Test
    void test6_ZeroConsumptionBelowSafety() {
        UUID testStationId = DataSeeder.HIMADRI_ID;
        UUID testItemId = DataSeeder.ITEM_BATT_ID;

        StockLevelEntity sl = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testItemId).orElseThrow();
        sl.setCurrentStock(new BigDecimal("5.00"));
        sl.setDailyConsumption(BigDecimal.ZERO);
        sl.setSafetyStock(new BigDecimal("10.00"));
        sl.setNextResupplyDate(evaluationDate.plusDays(10));
        stockLevelRepository.save(sl);

        StationIntelligenceReport report = intelligenceService.evaluateStationIntelligence(testStationId, 0, evaluationDate);
        StockRiskAssessment batt = report.assessments().stream()
                .filter(a -> a.inventoryItemId().equals(testItemId))
                .findFirst().orElseThrow();

        assertThat(batt.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(batt.daysToSafetyThreshold()).isNull();
        assertThat(batt.projectedStockAtResupply()).isEqualByComparingTo("5.00");
        assertThat(batt.explanation()).contains("Current stock is already at or below safety stock. Zero consumption rate recorded.");
    }

    @Test
    void test7_DelayDaysChangesProjectedStock() {
        StationIntelligenceReport report0 = intelligenceService.evaluateStationIntelligence(DataSeeder.BHARATI_ID, 0, evaluationDate);
        StationIntelligenceReport report10 = intelligenceService.evaluateStationIntelligence(DataSeeder.BHARATI_ID, 10, evaluationDate);

        StockRiskAssessment fuel0 = report0.assessments().stream()
                .filter(a -> a.itemCode().equals("FUEL-DIESEL-A")).findFirst().orElseThrow();
        StockRiskAssessment fuel10 = report10.assessments().stream()
                .filter(a -> a.itemCode().equals("FUEL-DIESEL-A")).findFirst().orElseThrow();

        // 10 extra days * 420L/day = 4200L lower projected stock
        assertThat(fuel0.projectedStockAtResupply().subtract(fuel10.projectedStockAtResupply()))
                .isEqualByComparingTo("4200.00");
    }

    @Test
    void test8_DelayDaysChangesEffectiveResupplyDate() {
        StationIntelligenceReport report0 = intelligenceService.evaluateStationIntelligence(DataSeeder.BHARATI_ID, 0, evaluationDate);
        StationIntelligenceReport report10 = intelligenceService.evaluateStationIntelligence(DataSeeder.BHARATI_ID, 10, evaluationDate);

        StockRiskAssessment fuel0 = report0.assessments().stream()
                .filter(a -> a.itemCode().equals("FUEL-DIESEL-A")).findFirst().orElseThrow();
        StockRiskAssessment fuel10 = report10.assessments().stream()
                .filter(a -> a.itemCode().equals("FUEL-DIESEL-A")).findFirst().orElseThrow();

        assertThat(fuel10.effectiveResupplyDate()).isEqualTo(fuel0.effectiveResupplyDate().plusDays(10));
    }

    @Test
    void test9_DaysToSafetyCalculation() {
        // Bharati Fuel: 8420L current, 2000L safety, 420L/day consumption -> (8420 - 2000) / 420 = 15.2857 days
        StationIntelligenceReport report = intelligenceService.evaluateStationIntelligence(DataSeeder.BHARATI_ID, 0, evaluationDate);
        StockRiskAssessment fuel = report.assessments().stream()
                .filter(a -> a.itemCode().equals("FUEL-DIESEL-A")).findFirst().orElseThrow();

        assertThat(fuel.daysToSafetyThreshold()).isNotNull();
        assertThat(fuel.daysToSafetyThreshold()).isCloseTo(15.2857, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void test10_ProjectedStockCalculation() {
        // Bharati Fuel: 8420 - (420 * 23) = 8420 - 9660 = -1240.00
        StationIntelligenceReport report = intelligenceService.evaluateStationIntelligence(DataSeeder.BHARATI_ID, 0, evaluationDate);
        StockRiskAssessment fuel = report.assessments().stream()
                .filter(a -> a.itemCode().equals("FUEL-DIESEL-A")).findFirst().orElseThrow();

        assertThat(fuel.projectedStockAtResupply()).isEqualByComparingTo("-1240.00");
    }

    @Test
    void test11_StationWithMultipleInventoryItems() {
        StationIntelligenceReport report = intelligenceService.evaluateStationIntelligence(DataSeeder.BHARATI_ID, 0, evaluationDate);
        assertThat(report.assessments().size()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void test12_NoNegativeDaysToSafety() {
        UUID testStationId = DataSeeder.HIMADRI_ID;
        UUID testItemId = DataSeeder.ITEM_BATT_ID;

        StockLevelEntity sl = stockLevelRepository.findByStationIdAndInventoryItemId(testStationId, testItemId).orElseThrow();
        sl.setCurrentStock(new BigDecimal("5.00"));
        sl.setSafetyStock(new BigDecimal("20.00"));
        stockLevelRepository.save(sl);

        StationIntelligenceReport report = intelligenceService.evaluateStationIntelligence(testStationId, 0, evaluationDate);
        StockRiskAssessment batt = report.assessments().stream()
                .filter(a -> a.inventoryItemId().equals(testItemId))
                .findFirst().orElseThrow();

        assertThat(batt.daysToSafetyThreshold()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void test13_InterStationSurplusDetection() {
        // Maitri has 18,500L fuel, safety 2500L, daily 380L, 15 days resupply.
        // Maitri committed = 2500 + (380 * 15) = 8200L. Surplus = 18500 - 8200 = 10,300L!
        StationIntelligenceReport report = intelligenceService.evaluateStationIntelligence(DataSeeder.BHARATI_ID, 0, evaluationDate);
        assertThat(report.recommendations()).isNotEmpty();

        TransferRecommendation fuelRec = report.recommendations().stream()
                .filter(r -> r.itemCode().equals("FUEL-DIESEL-A") && r.sourceStationId().equals(DataSeeder.MAITRI_ID))
                .findFirst().orElseThrow();

        assertThat(fuelRec.sourceStationName()).isEqualTo("Maitri Station");
        assertThat(fuelRec.targetStationName()).isEqualTo("Bharati Station");
        assertThat(fuelRec.recommendedQuantity()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void test14_TransferRecommendationGeneration() {
        StationIntelligenceReport report = intelligenceService.evaluateStationIntelligence(DataSeeder.BHARATI_ID, 10, evaluationDate);
        assertThat(report.recommendations()).isNotEmpty();

        TransferRecommendation rec = report.recommendations().get(0);
        assertThat(rec.sourceStationId()).isNotNull();
        assertThat(rec.targetStationId()).isEqualTo(DataSeeder.BHARATI_ID);
        assertThat(rec.rationale()).contains("requires approximately");
    }

    @Test
    void test15_NoRecommendationWhenSourceHasNoSurplus() {
        // Reduce Maitri's fuel to bare safety level
        StockLevelEntity maitriFuel = stockLevelRepository.findByStationIdAndInventoryItemId(DataSeeder.MAITRI_ID, DataSeeder.ITEM_FUEL_ID).orElseThrow();
        maitriFuel.setCurrentStock(new BigDecimal("3000.00")); // committed is 8200L, so surplus is negative
        stockLevelRepository.save(maitriFuel);

        // Also ensure Himadri has no fuel surplus
        StockLevelEntity himadriFuel = stockLevelRepository.findByStationIdAndInventoryItemId(DataSeeder.HIMADRI_ID, DataSeeder.ITEM_FUEL_ID).orElseThrow();
        himadriFuel.setCurrentStock(new BigDecimal("1000.00"));
        stockLevelRepository.save(himadriFuel);

        StationIntelligenceReport report = intelligenceService.evaluateStationIntelligence(DataSeeder.BHARATI_ID, 0, evaluationDate);
        List<TransferRecommendation> fuelRecs = report.recommendations().stream()
                .filter(r -> r.itemCode().equals("FUEL-DIESEL-A"))
                .toList();

        assertThat(fuelRecs).isEmpty();
    }

    @Test
    void test16_NoRecommendationWhenTargetHasNoShortfall() {
        // Make Bharati Fuel healthy
        StockLevelEntity bharatiFuel = stockLevelRepository.findByStationIdAndInventoryItemId(DataSeeder.BHARATI_ID, DataSeeder.ITEM_FUEL_ID).orElseThrow();
        bharatiFuel.setCurrentStock(new BigDecimal("25000.00"));
        stockLevelRepository.save(bharatiFuel);

        StationIntelligenceReport report = intelligenceService.evaluateStationIntelligence(DataSeeder.BHARATI_ID, 0, evaluationDate);
        List<TransferRecommendation> fuelRecs = report.recommendations().stream()
                .filter(r -> r.itemCode().equals("FUEL-DIESEL-A"))
                .toList();

        assertThat(fuelRecs).isEmpty();
    }

    @Test
    void test17_SourceStationIsNeverTargetItself() {
        StationIntelligenceReport report = intelligenceService.evaluateStationIntelligence(DataSeeder.BHARATI_ID, 10, evaluationDate);
        for (TransferRecommendation rec : report.recommendations()) {
            assertThat(rec.sourceStationId()).isNotEqualTo(rec.targetStationId());
        }
    }

    @Test
    void test18_RecommendationQuantityNeverExceedsSourceSurplus() {
        StationIntelligenceReport report = intelligenceService.evaluateStationIntelligence(DataSeeder.BHARATI_ID, 0, evaluationDate);
        for (TransferRecommendation rec : report.recommendations()) {
            StockLevelEntity sourceSl = stockLevelRepository.findByStationIdAndInventoryItemId(rec.sourceStationId(), rec.inventoryItemId()).orElseThrow();
            long days = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(evaluationDate, sourceSl.getNextResupplyDate()));
            BigDecimal committed = sourceSl.getSafetyStock().add(sourceSl.getDailyConsumption().multiply(BigDecimal.valueOf(days)));
            BigDecimal surplus = sourceSl.getCurrentStock().subtract(committed);

            assertThat(rec.recommendedQuantity()).isLessThanOrEqualTo(surplus);
        }
    }

    @Test
    void test19_RecommendationQuantityNeverExceedsTargetShortfall() {
        StationIntelligenceReport report = intelligenceService.evaluateStationIntelligence(DataSeeder.BHARATI_ID, 0, evaluationDate);
        for (TransferRecommendation rec : report.recommendations()) {
            StockRiskAssessment targetAssessment = report.assessments().stream()
                    .filter(a -> a.inventoryItemId().equals(rec.inventoryItemId()))
                    .findFirst().orElseThrow();
            BigDecimal shortfall = targetAssessment.safetyStock().subtract(targetAssessment.projectedStockAtResupply());

            assertThat(rec.recommendedQuantity()).isLessThanOrEqualTo(shortfall);
        }
    }

    @Test
    void test20_SihDemoScenario_DelayTriggersCriticalAndSurplusRecommendation() {
        // SIH Demo Scenario:
        // Bharati Fuel: 8420L current, 420L/day, 2000L safety, 23 days resupply.
        // Days to safety = (8420 - 2000) / 420 = 15.2857 days.
        // Resupply at Day 23: 8420 - (420 * 23) = -1240L < 2000L -> CRITICAL breach!
        // Maitri has 18500L fuel, safety 2500L, daily 380L, 15 days resupply.
        // Maitri surplus = 18500 - (2500 + 380 * 15) = 10,300L.
        // Bharati Shortfall = 2000 - (-1240) = 3240L.
        // Recommended Transfer = min(3240, 10300) = 3240L from Maitri to Bharati.

        StationIntelligenceReport report = intelligenceService.evaluateStationIntelligence(DataSeeder.BHARATI_ID, 0, evaluationDate);

        StockRiskAssessment fuelAssessment = report.assessments().stream()
                .filter(a -> a.itemCode().equals("FUEL-DIESEL-A"))
                .findFirst().orElseThrow();

        assertThat(fuelAssessment.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(fuelAssessment.daysToSafetyThreshold()).isCloseTo(15.2857, org.assertj.core.data.Offset.offset(0.001));

        TransferRecommendation maitriTransfer = report.recommendations().stream()
                .filter(r -> r.sourceStationId().equals(DataSeeder.MAITRI_ID) && r.itemCode().equals("FUEL-DIESEL-A"))
                .findFirst().orElseThrow();

        assertThat(maitriTransfer.sourceStationName()).isEqualTo("Maitri Station");
        assertThat(maitriTransfer.targetStationName()).isEqualTo("Bharati Station");
        assertThat(maitriTransfer.recommendedQuantity()).isEqualByComparingTo("3240.00");
    }

    @Test
    void testNegativeDelayDaysThrowsException() {
        assertThatThrownBy(() -> intelligenceService.evaluateStationIntelligence(DataSeeder.BHARATI_ID, -5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delayDays must be greater than or equal to 0");
    }
}
