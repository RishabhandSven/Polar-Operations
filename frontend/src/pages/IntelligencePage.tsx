import React, { useEffect, useState } from 'react';
import { useStationContext } from '../context/StationContext';
import { intelligenceApi } from '../api/api';
import { StationIntelligenceReport, StockRiskAssessment } from '../types';
import { RiskBadge } from '../components/common/RiskBadge';
import { TransferRecommendationsCard } from '../components/dashboard/TransferRecommendationsCard';
import { LoadingState, ErrorState, EmptyState } from '../components/common/FeedbackStates';
import {
  BrainCircuit,
  Clock,
  RefreshCw,
  Info,
  Calendar,
  Zap,
  TrendingDown,
  AlertTriangle,
} from 'lucide-react';

export const IntelligencePage: React.FC = () => {
  const { selectedStation } = useStationContext();
  const [report, setReport] = useState<StationIntelligenceReport | null>(null);
  const [delayDays, setDelayDays] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedAssessment, setSelectedAssessment] = useState<StockRiskAssessment | null>(null);

  const fetchReport = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await intelligenceApi.getReport(selectedStation.id, delayDays);
      setReport(data);
      if (data.assessments.length > 0 && !selectedAssessment) {
        setSelectedAssessment(data.assessments[0]);
      } else if (selectedAssessment) {
        // Keep selected assessment up to date
        const updated = data.assessments.find((a) => a.stockLevelId === selectedAssessment.stockLevelId);
        if (updated) setSelectedAssessment(updated);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to generate inventory intelligence report');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReport();
  }, [selectedStation.id, delayDays]);

  return (
    <div className="space-y-6">
      {/* Header with Resupply Delay Simulation */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-900/60 p-4 rounded-lg border border-slate-800 backdrop-blur-sm">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-lg bg-blue-600/20 border border-blue-500/30 text-blue-400">
            <BrainCircuit className="h-6 w-6" />
          </div>
          <div>
            <h1 className="text-lg font-bold text-slate-100 uppercase tracking-wide">
              {selectedStation.name} — Predictive Risk & Intelligence
            </h1>
            <p className="text-xs text-slate-400 mt-0.5">
              Deterministic Forecasting Engine & Inter-Station Optimization
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 bg-slate-950 px-3 py-1.5 rounded-md border border-slate-700/80 text-xs">
            <Clock className="h-3.5 w-3.5 text-amber-400" />
            <span className="text-slate-400 font-medium">Delay Simulation:</span>
            <select
              value={delayDays}
              onChange={(e) => setDelayDays(Number(e.target.value))}
              className="bg-transparent font-semibold text-amber-400 focus:outline-none cursor-pointer"
            >
              <option value={0} className="bg-slate-900 text-slate-200">0 Days (Normal Schedule)</option>
              <option value={5} className="bg-slate-900 text-slate-200">+5 Days Delay</option>
              <option value={10} className="bg-slate-900 text-slate-200">+10 Days Delay</option>
              <option value={15} className="bg-slate-900 text-slate-200">+15 Days Delay</option>
              <option value={30} className="bg-slate-900 text-slate-200">+30 Days (Crisis Mode)</option>
            </select>
          </div>

          <button
            onClick={fetchReport}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium transition-colors border border-slate-700/60"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin text-blue-400' : ''}`} />
            <span>Recalculate</span>
          </button>
        </div>
      </div>

      {loading && !report ? (
        <LoadingState message="Executing deterministic risk evaluation model..." />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchReport} />
      ) : !report || report.assessments.length === 0 ? (
        <EmptyState
          title="No intelligence data available"
          message="No active stock records were returned for evaluation at this station."
        />
      ) : (
        <>
          {/* Main Risk Table & Deep-Dive Inspector */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* 2-Columns: Assessments Table */}
            <div className="lg:col-span-2 rounded-lg border border-slate-800/80 bg-slate-900/50 backdrop-blur-sm overflow-hidden flex flex-col">
              <div className="px-4 py-3 border-b border-slate-800 flex justify-between items-center bg-slate-950/40">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-300">
                  Stock Risk Evaluations ({report.assessments.length})
                </span>
                <span className="text-[11px] text-slate-500 font-mono">
                  Evaluated: {new Date(report.evaluatedAt).toLocaleTimeString()}
                </span>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-slate-300">
                  <thead className="bg-slate-950/80 text-[11px] uppercase tracking-wider text-slate-400 border-b border-slate-800">
                    <tr>
                      <th className="px-4 py-3">Item</th>
                      <th className="px-3 py-3 text-right">Current Stock</th>
                      <th className="px-3 py-3 text-right">Days to Safety</th>
                      <th className="px-3 py-3 text-right">Proj. At Resupply</th>
                      <th className="px-3 py-3 text-center">Risk Level</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {report.assessments.map((a) => {
                      const isSelected = selectedAssessment?.stockLevelId === a.stockLevelId;
                      return (
                        <tr
                          key={a.stockLevelId}
                          onClick={() => setSelectedAssessment(a)}
                          className={`hover:bg-slate-800/50 cursor-pointer transition-colors ${
                            isSelected ? 'bg-blue-950/30 border-l-2 border-blue-500' : ''
                          }`}
                        >
                          <td className="px-4 py-3 font-medium text-slate-100">
                            <div>{a.itemName}</div>
                            <div className="text-[10px] text-slate-500 font-mono">{a.itemCode}</div>
                          </td>
                          <td className="px-3 py-3 text-right font-mono text-slate-200">
                            {a.currentStock.toLocaleString()} {a.unit}
                          </td>
                          <td className="px-3 py-3 text-right font-mono font-semibold">
                            {a.daysToSafetyThreshold !== null ? (
                              <span className={a.daysToSafetyThreshold <= 0 ? 'text-red-400' : 'text-slate-200'}>
                                {a.daysToSafetyThreshold} d
                              </span>
                            ) : (
                              <span className="text-slate-500 font-normal">N/A (0 burn)</span>
                            )}
                          </td>
                          <td className="px-3 py-3 text-right font-mono font-semibold">
                            <span className={a.projectedStockAtResupply < 0 ? 'text-red-400' : 'text-slate-200'}>
                              {a.projectedStockAtResupply.toLocaleString()} {a.unit}
                            </span>
                          </td>
                          <td className="px-3 py-3 text-center">
                            <RiskBadge risk={a.riskLevel} />
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>

            {/* 1-Column: Deep-Dive Risk Inspector */}
            <div className="rounded-lg border border-slate-800/80 bg-slate-900/50 p-4 backdrop-blur-sm h-fit space-y-4">
              {selectedAssessment ? (
                <>
                  <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                    <div>
                      <h3 className="text-sm font-bold text-slate-100">{selectedAssessment.itemName}</h3>
                      <div className="text-xs font-mono text-slate-400">{selectedAssessment.itemCode}</div>
                    </div>
                    <RiskBadge risk={selectedAssessment.riskLevel} />
                  </div>

                  {/* Operational Telemetry Grid */}
                  <div className="grid grid-cols-2 gap-3 text-xs">
                    <div className="p-2.5 rounded bg-slate-950/60 border border-slate-800">
                      <span className="text-[10px] uppercase font-semibold text-slate-500">Current Stock</span>
                      <div className="text-sm font-bold font-mono text-slate-100 mt-0.5">
                        {selectedAssessment.currentStock.toLocaleString()} {selectedAssessment.unit}
                      </div>
                    </div>
                    <div className="p-2.5 rounded bg-slate-950/60 border border-slate-800">
                      <span className="text-[10px] uppercase font-semibold text-slate-500">Safety Threshold</span>
                      <div className="text-sm font-bold font-mono text-amber-400 mt-0.5">
                        {selectedAssessment.safetyStock.toLocaleString()} {selectedAssessment.unit}
                      </div>
                    </div>
                    <div className="p-2.5 rounded bg-slate-950/60 border border-slate-800">
                      <span className="text-[10px] uppercase font-semibold text-slate-500">Daily Burn Rate</span>
                      <div className="text-sm font-bold font-mono text-slate-200 mt-0.5">
                        {selectedAssessment.dailyConsumption.toLocaleString()} {selectedAssessment.unit}/d
                      </div>
                    </div>
                    <div className="p-2.5 rounded bg-slate-950/60 border border-slate-800">
                      <span className="text-[10px] uppercase font-semibold text-slate-500">Effective Resupply</span>
                      <div className="text-xs font-bold font-mono text-blue-400 mt-0.5">
                        {selectedAssessment.effectiveResupplyDate}
                      </div>
                    </div>
                  </div>

                  {/* Shortage Projection Indicator */}
                  <div className="p-3 rounded bg-slate-950/80 border border-slate-800 text-xs space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-slate-400 font-medium">Projected Stock at Resupply:</span>
                      <span
                        className={`font-mono font-bold ${
                          selectedAssessment.projectedStockAtResupply < 0 ? 'text-red-400' : 'text-slate-200'
                        }`}
                      >
                        {selectedAssessment.projectedStockAtResupply.toLocaleString()} {selectedAssessment.unit}
                      </span>
                    </div>

                    <div className="flex items-center justify-between">
                      <span className="text-slate-400 font-medium">Days Until Safety Breach:</span>
                      <span className="font-mono font-bold text-amber-400">
                        {selectedAssessment.daysToSafetyThreshold !== null
                          ? `${selectedAssessment.daysToSafetyThreshold} days`
                          : 'Indefinite (0 burn)'}
                      </span>
                    </div>
                  </div>

                  {/* Natural Language Explanation from Backend */}
                  <div className="p-3.5 rounded bg-blue-950/30 border border-blue-900/50 text-xs space-y-1.5">
                    <div className="flex items-center gap-1.5 text-blue-400 font-semibold text-[11px] uppercase tracking-wider">
                      <Zap className="h-3.5 w-3.5" />
                      <span>Mathematical Rationale</span>
                    </div>
                    <p className="text-slate-300 leading-relaxed">
                      {selectedAssessment.explanation}
                    </p>
                  </div>
                </>
              ) : (
                <div className="py-12 text-center text-xs text-slate-500">
                  Select an item to view mathematical risk model and projections.
                </div>
              )}
            </div>
          </div>

          {/* Transfer Recommendations */}
          <TransferRecommendationsCard recommendations={report.recommendations} />
        </>
      )}
    </div>
  );
};
