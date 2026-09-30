import React, { useEffect, useState } from 'react';
import { useStationContext } from '../context/StationContext';
import { intelligenceApi, cargoApi } from '../api/api';
import { StationIntelligenceReport, CargoConsignment } from '../types';
import { KpiCard } from '../components/common/KpiCard';
import { RiskChart } from '../components/dashboard/RiskChart';
import { StationMap } from '../components/dashboard/StationMap';
import { TransferRecommendationsCard } from '../components/dashboard/TransferRecommendationsCard';
import { CargoSummaryCard } from '../components/dashboard/CargoSummaryCard';
import { LoadingState, ErrorState } from '../components/common/FeedbackStates';
import {
  AlertTriangle,
  ShieldCheck,
  AlertCircle,
  Ship,
  RefreshCw,
  Clock,
  ArrowRight,
} from 'lucide-react';
import { Link } from 'react-router-dom';

export const DashboardPage: React.FC = () => {
  const { selectedStation } = useStationContext();
  const [report, setReport] = useState<StationIntelligenceReport | null>(null);
  const [consignments, setConsignments] = useState<CargoConsignment[]>([]);
  const [delayDays, setDelayDays] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [intelData, cargoData] = await Promise.all([
        intelligenceApi.getReport(selectedStation.id, delayDays),
        cargoApi.getConsignments(selectedStation.id).catch(() => []),
      ]);
      setReport(intelData);
      setConsignments(cargoData);
    } catch (err: any) {
      setError(err.message || 'Failed to fetch dashboard operational telemetry');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [selectedStation.id, delayDays]);

  const criticalItems = report?.assessments.filter((a) => a.riskLevel === 'CRITICAL') || [];
  const warningItems = report?.assessments.filter((a) => a.riskLevel === 'WARNING') || [];
  const healthyItems = report?.assessments.filter((a) => a.riskLevel === 'HEALTHY') || [];

  return (
    <div className="space-y-6">
      {/* Top Banner & Delay Simulation Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-900/60 p-4 rounded-lg border border-slate-800 backdrop-blur-sm">
        <div>
          <h1 className="text-lg font-bold text-slate-100 uppercase tracking-wide">
            {selectedStation.name} — Operations Command
          </h1>
          <p className="text-xs text-slate-400 mt-0.5">
            Integrated Logistics, Resupply Projections & Telemetry
          </p>
        </div>

        {/* Resupply Delay Simulation Control */}
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 bg-slate-950 px-3 py-1.5 rounded-md border border-slate-700/80 text-xs">
            <Clock className="h-3.5 w-3.5 text-amber-400" />
            <span className="text-slate-400 font-medium">Simulated Resupply Delay:</span>
            <select
              value={delayDays}
              onChange={(e) => setDelayDays(Number(e.target.value))}
              className="bg-transparent font-semibold text-amber-400 focus:outline-none cursor-pointer"
            >
              <option value={0} className="bg-slate-900 text-slate-200">0 Days (Normal)</option>
              <option value={5} className="bg-slate-900 text-slate-200">+5 Days</option>
              <option value={10} className="bg-slate-900 text-slate-200">+10 Days</option>
              <option value={15} className="bg-slate-900 text-slate-200">+15 Days</option>
              <option value={30} className="bg-slate-900 text-slate-200">+30 Days (Severe)</option>
            </select>
          </div>

          <button
            onClick={fetchData}
            title="Refresh Telemetry"
            className="p-2 rounded-md bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors border border-slate-700/60"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin text-blue-400' : ''}`} />
          </button>
        </div>
      </div>

      {loading && !report ? (
        <LoadingState message="Fetching live station telemetry..." />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchData} />
      ) : (
        <>
          {/* KPI Summary Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <KpiCard
              title="Critical Stock Items"
              value={criticalItems.length}
              subtitle="Immediate exhaustion risk"
              icon={AlertCircle}
              variant={criticalItems.length > 0 ? 'critical' : 'default'}
            />
            <KpiCard
              title="Warning Stock Items"
              value={warningItems.length}
              subtitle="Approaching safety point"
              icon={AlertTriangle}
              variant={warningItems.length > 0 ? 'warning' : 'default'}
            />
            <KpiCard
              title="Healthy Stock Items"
              value={healthyItems.length}
              subtitle="Adequate supply"
              icon={ShieldCheck}
              variant="healthy"
            />
            <KpiCard
              title="Active Consignments"
              value={consignments.length}
              subtitle="Supply pipeline"
              icon={Ship}
              variant="info"
            />
          </div>

          {/* Main Visuals: Risk Breakdown & Global Map */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <RiskChart assessments={report?.assessments || []} />
            <StationMap />
          </div>

          {/* Critical Alerts Strip */}
          {criticalItems.length > 0 && (
            <div className="rounded-lg border border-red-900/60 bg-red-950/20 p-4">
              <div className="flex items-center justify-between pb-3 border-b border-red-900/40">
                <div className="flex items-center gap-2 text-red-400 font-semibold text-xs uppercase tracking-wider">
                  <AlertCircle className="h-4 w-4" />
                  <span>Critical Stock Alerts ({criticalItems.length})</span>
                </div>
                <Link
                  to="/intelligence"
                  className="text-xs text-red-400 hover:text-red-300 font-medium flex items-center gap-1"
                >
                  <span>Full Intelligence Report</span>
                  <ArrowRight className="h-3 w-3" />
                </Link>
              </div>
              <div className="mt-3 space-y-2">
                {criticalItems.map((item) => (
                  <div
                    key={item.stockLevelId}
                    className="p-2.5 rounded bg-slate-950/80 border border-red-900/40 flex flex-col sm:flex-row sm:items-center justify-between text-xs gap-2"
                  >
                    <div>
                      <span className="font-semibold text-slate-100">{item.itemName}</span>
                      <span className="text-slate-400 ml-2">({item.itemCode})</span>
                      <p className="text-[11px] text-red-400 mt-0.5">{item.explanation}</p>
                    </div>
                    <div className="text-right shrink-0">
                      <div className="font-mono text-slate-200">
                        {item.currentStock.toLocaleString()} {item.unit} / Safety: {item.safetyStock.toLocaleString()} {item.unit}
                      </div>
                      <div className="text-[11px] text-amber-400 font-medium">
                        {item.daysToSafetyThreshold !== null ? `${item.daysToSafetyThreshold} days to safety` : 'Zero Consumption'}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Secondary Cards: Transfer Suggestions & Cargo Pipelines */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <TransferRecommendationsCard recommendations={report?.recommendations || []} />
            <CargoSummaryCard consignments={consignments} />
          </div>
        </>
      )}
    </div>
  );
};
