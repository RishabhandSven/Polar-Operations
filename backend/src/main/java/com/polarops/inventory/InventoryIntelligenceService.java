package com.polarops.inventory;

import com.polarops.common.ResourceNotFoundException;
import com.polarops.expedition.StationEntity;
import com.polarops.expedition.StationRepository;
import com.polarops.inventory.InventoryDtos.StationIntelligenceReport;
import com.polarops.inventory.InventoryDtos.StockRiskAssessment;
import com.polarops.inventory.InventoryDtos.TransferRecommendation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InventoryIntelligenceService {

    private final StockLevelRepository stockLevelRepository;
    private final InventoryItemRepository itemRepository;
    private final StationRepository stationRepository;

    public InventoryIntelligenceService(StockLevelRepository stockLevelRepository,
                                      InventoryItemRepository itemRepository,
                                      StationRepository stationRepository) {
        this.stockLevelRepository = stockLevelRepository;
        this.itemRepository = itemRepository;
        this.stationRepository = stationRepository;
    }

    @Transactional(readOnly = true)
    public StationIntelligenceReport evaluateStationIntelligence(UUID stationId, int delayDays) {
        return evaluateStationIntelligence(stationId, delayDays, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public StationIntelligenceReport evaluateStationIntelligence(UUID stationId, int delayDays, LocalDate evaluationDate) {
        if (delayDays < 0) {
            throw new IllegalArgumentException("delayDays must be greater than or equal to 0, received: " + delayDays);
        }

        StationEntity targetStation = stationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + stationId));

        List<StockLevelEntity> targetStockLevels = stockLevelRepository.findByStationId(stationId);
        if (targetStockLevels.isEmpty()) {
            return new StationIntelligenceReport(
                    stationId,
                    targetStation.getName(),
                    delayDays,
                    List.of(),
                    List.of(),
                    Instant.now()
            );
        }

        Map<UUID, InventoryItemEntity> itemsMap = itemRepository.findAllById(
                targetStockLevels.stream().map(StockLevelEntity::getInventoryItemId).toList()
        ).stream().collect(Collectors.toMap(InventoryItemEntity::getId, Function.identity()));

        List<StockRiskAssessment> assessments = new ArrayList<>();
        List<TransferRecommendation> recommendations = new ArrayList<>();

        Map<UUID, StationEntity> allStationsMap = stationRepository.findAll().stream()
                .collect(Collectors.toMap(StationEntity::getId, Function.identity()));

        for (StockLevelEntity stockLevel : targetStockLevels) {
            InventoryItemEntity item = itemsMap.get(stockLevel.getInventoryItemId());
            String itemCode = item != null ? item.getItemCode() : "UNKNOWN";
            String itemName = item != null ? item.getName() : "UNKNOWN";
            String category = item != null ? item.getCategory() : "UNKNOWN";
            String unit = item != null ? item.getUnit() : "UNITS";

            StockRiskAssessment assessment = calculateAssessment(stockLevel, itemCode, itemName, category, unit, delayDays, evaluationDate);
            assessments.add(assessment);

            if (assessment.riskLevel() == RiskLevel.CRITICAL) {
                List<TransferRecommendation> transferProposals = evaluateTransferOptions(
                        targetStation,
                        stockLevel,
                        assessment,
                        delayDays,
                        evaluationDate,
                        allStationsMap
                );
                recommendations.addAll(transferProposals);
            }
        }

        // Deterministic ordering: highest recommended quantity first, then source station name ascending
        recommendations.sort(
                Comparator.comparing(TransferRecommendation::recommendedQuantity, Comparator.reverseOrder())
                        .thenComparing(TransferRecommendation::sourceStationName)
        );

        return new StationIntelligenceReport(
                stationId,
                targetStation.getName(),
                delayDays,
                assessments,
                recommendations,
                Instant.now()
        );
    }

    private StockRiskAssessment calculateAssessment(StockLevelEntity sl,
                                                   String itemCode,
                                                   String itemName,
                                                   String category,
                                                   String unit,
                                                   int delayDays,
                                                   LocalDate evaluationDate) {
        LocalDate effectiveResupplyDate = sl.getNextResupplyDate().plusDays(delayDays);
        long daysUntilResupply = Math.max(0, ChronoUnit.DAYS.between(evaluationDate, effectiveResupplyDate));

        BigDecimal currentStock = sl.getCurrentStock();
        BigDecimal dailyConsumption = sl.getDailyConsumption();
        BigDecimal safetyStock = sl.getSafetyStock();
        BigDecimal reorderPoint = sl.getReorderPoint();

        // Step 2: Zero Consumption Rule
        if (dailyConsumption.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal projectedStockAtResupply = currentStock;
            Double daysToSafetyThreshold = null; // Do NOT serialize Infinity

            if (currentStock.compareTo(safetyStock) <= 0) {
                return new StockRiskAssessment(
                        sl.getId(), sl.getInventoryItemId(), itemCode, itemName, category, unit,
                        currentStock, safetyStock, dailyConsumption, effectiveResupplyDate,
                        daysToSafetyThreshold, projectedStockAtResupply, RiskLevel.CRITICAL,
                        "Current stock is already at or below safety stock. Zero consumption rate recorded."
                );
            } else {
                return new StockRiskAssessment(
                        sl.getId(), sl.getInventoryItemId(), itemCode, itemName, category, unit,
                        currentStock, safetyStock, dailyConsumption, effectiveResupplyDate,
                        daysToSafetyThreshold, projectedStockAtResupply, RiskLevel.HEALTHY,
                        "Zero consumption rate; stock remains constant at current levels."
                );
            }
        }

        // Step 3: Active Consumption Rules
        BigDecimal projectedConsumption = dailyConsumption.multiply(BigDecimal.valueOf(daysUntilResupply));
        BigDecimal projectedStockAtResupply = currentStock.subtract(projectedConsumption);

        // Case A: Already below safety threshold
        if (currentStock.compareTo(safetyStock) <= 0) {
            Double daysToSafetyThreshold = 0.0;
            return new StockRiskAssessment(
                    sl.getId(), sl.getInventoryItemId(), itemCode, itemName, category, unit,
                    currentStock, safetyStock, dailyConsumption, effectiveResupplyDate,
                    daysToSafetyThreshold, projectedStockAtResupply, RiskLevel.CRITICAL,
                    String.format(Locale.US, "Stock is already at or below safety threshold (%s <= %s). Immediate resupply or emergency conservation required.",
                            currentStock, safetyStock)
            );
        }

        // Calculate days to safety threshold (currentStock - safetyStock) / dailyConsumption
        BigDecimal stockAboveSafety = currentStock.subtract(safetyStock);
        double daysToSafety = stockAboveSafety.divide(dailyConsumption, 4, RoundingMode.HALF_UP).doubleValue();
        Double daysToSafetyThreshold = Math.max(0.0, daysToSafety);

        // Case B: Projected safety breach
        if (projectedStockAtResupply.compareTo(safetyStock) < 0) {
            return new StockRiskAssessment(
                    sl.getId(), sl.getInventoryItemId(), itemCode, itemName, category, unit,
                    currentStock, safetyStock, dailyConsumption, effectiveResupplyDate,
                    daysToSafetyThreshold, projectedStockAtResupply, RiskLevel.CRITICAL,
                    String.format(Locale.US, "Safety threshold breached in %.1f days, before scheduled resupply in %d days.",
                            daysToSafetyThreshold, daysUntilResupply)
            );
        }

        // Case C: Reorder warning
        if (projectedStockAtResupply.compareTo(reorderPoint) < 0) {
            return new StockRiskAssessment(
                    sl.getId(), sl.getInventoryItemId(), itemCode, itemName, category, unit,
                    currentStock, safetyStock, dailyConsumption, effectiveResupplyDate,
                    daysToSafetyThreshold, projectedStockAtResupply, RiskLevel.WARNING,
                    String.format(Locale.US, "Projected stock (%s) will drop below reorder point (%s) prior to resupply.",
                            projectedStockAtResupply, reorderPoint)
            );
        }

        // Case D: Healthy
        return new StockRiskAssessment(
                sl.getId(), sl.getInventoryItemId(), itemCode, itemName, category, unit,
                currentStock, safetyStock, dailyConsumption, effectiveResupplyDate,
                daysToSafetyThreshold, projectedStockAtResupply, RiskLevel.HEALTHY,
                String.format(Locale.US, "Projected stock (%s) remains above safety buffer (%s).",
                        projectedStockAtResupply, safetyStock)
        );
    }

    private List<TransferRecommendation> evaluateTransferOptions(StationEntity targetStation,
                                                                 StockLevelEntity targetStockLevel,
                                                                 StockRiskAssessment assessment,
                                                                 int delayDays,
                                                                 LocalDate evaluationDate,
                                                                 Map<UUID, StationEntity> allStationsMap) {
        BigDecimal shortfall = assessment.safetyStock().subtract(assessment.projectedStockAtResupply());
        if (shortfall.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        List<StockLevelEntity> peerStockLevels = stockLevelRepository.findByInventoryItemId(assessment.inventoryItemId());
        List<TransferRecommendation> proposals = new ArrayList<>();

        for (StockLevelEntity peer : peerStockLevels) {
            if (peer.getStationId().equals(targetStation.getId())) {
                continue; // Do not recommend transfer from self
            }

            StationEntity sourceStation = allStationsMap.get(peer.getStationId());
            String sourceStationName = sourceStation != null ? sourceStation.getName() : "Peer Station";

            LocalDate peerResupplyDate = peer.getNextResupplyDate().plusDays(delayDays);
            long daysUntilPeerResupply = Math.max(0, ChronoUnit.DAYS.between(evaluationDate, peerResupplyDate));

            BigDecimal peerProjectedConsumption = peer.getDailyConsumption().multiply(BigDecimal.valueOf(daysUntilPeerResupply));
            BigDecimal peerCommittedStock = peer.getSafetyStock().add(peerProjectedConsumption);
            BigDecimal peerSurplus = peer.getCurrentStock().subtract(peerCommittedStock);

            if (peerSurplus.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal recommendedQty = shortfall.min(peerSurplus);
                if (recommendedQty.compareTo(BigDecimal.ZERO) > 0) {
                    String rationale = String.format(
                            Locale.US,
                            "%s requires approximately %s %s to restore projected stock to its safety threshold. %s has approximately %s %s surplus after accounting for its own safety buffer and projected consumption.",
                            targetStation.getName(),
                            recommendedQty.stripTrailingZeros().toPlainString(),
                            assessment.unit(),
                            sourceStationName,
                            peerSurplus.stripTrailingZeros().toPlainString(),
                            assessment.unit()
                    );

                    proposals.add(new TransferRecommendation(
                            peer.getStationId(),
                            sourceStationName,
                            targetStation.getId(),
                            targetStation.getName(),
                            assessment.inventoryItemId(),
                            assessment.itemCode(),
                            assessment.itemName(),
                            recommendedQty,
                            assessment.unit(),
                            rationale
                    ));
                }
            }
        }

        return proposals;
    }
}
