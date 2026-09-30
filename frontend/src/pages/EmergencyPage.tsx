import React, { useEffect, useState } from 'react';
import { useStationContext } from '../context/StationContext';
import { emergencyApi } from '../api/api';
import { Alert } from '../types';
import { KpiCard } from '../components/common/KpiCard';
import { LoadingState, ErrorState, EmptyState } from '../components/common/FeedbackStates';
import {
  AlertOctagon,
  AlertTriangle,
  Bell,
  CheckCircle2,
  RefreshCw,
  PlusCircle,
  X,
  Radio,
  Flame,
  CloudSnow,
  HeartPulse,
} from 'lucide-react';

export const EmergencyPage: React.FC = () => {
  const { selectedStation } = useStationContext();
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // New Alert Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [alertType, setAlertType] = useState('BLIZZARD_WARNING');
  const [severity, setSeverity] = useState('CRITICAL');
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await emergencyApi.listAlerts(selectedStation.id);
      setAlerts(data);
    } catch (err: any) {
      setError(err.message || 'Failed to fetch emergency alerts');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [selectedStation.id]);

  const handleCreateAlert = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!message.trim()) return;
    setSubmitting(true);
    try {
      await emergencyApi.createAlert({
        stationId: selectedStation.id,
        type: alertType,
        severity,
        message: message.trim(),
      });
      setIsModalOpen(false);
      setMessage('');
      await fetchData();
    } catch (err: any) {
      setError(err.message || 'Failed to dispatch alert');
    } finally {
      setSubmitting(false);
    }
  };

  const handleResolveAlert = async (id: string) => {
    try {
      await emergencyApi.resolveAlert(id);
      await fetchData();
    } catch (err: any) {
      setError(err.message || 'Failed to resolve alert');
    }
  };

  const activeAlerts = alerts.filter((a) => !a.resolved);
  const criticalCount = activeAlerts.filter((a) => a.severity === 'CRITICAL').length;
  const warningCount = activeAlerts.filter((a) => a.severity === 'WARNING').length;
  const resolvedCount = alerts.filter((a) => a.resolved).length;

  const getSeverityStyle = (sev: string) => {
    switch (sev) {
      case 'CRITICAL':
        return 'bg-red-950/80 text-red-400 border-red-800/80';
      case 'WARNING':
        return 'bg-amber-950/80 text-amber-400 border-amber-800/80';
      case 'INFO':
      default:
        return 'bg-blue-950/80 text-blue-400 border-blue-800/80';
    }
  };

  const getAlertIcon = (type: string) => {
    if (type.includes('WEATHER') || type.includes('BLIZZARD')) return CloudSnow;
    if (type.includes('MEDICAL') || type.includes('SOS')) return HeartPulse;
    if (type.includes('FIRE') || type.includes('GENERATOR')) return Flame;
    return AlertTriangle;
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-900/60 p-4 rounded-lg border border-slate-800 backdrop-blur-sm">
        <div>
          <h1 className="text-lg font-bold text-slate-100 uppercase tracking-wide">
            {selectedStation.name} — Emergency Response & Crisis Protocols
          </h1>
          <p className="text-xs text-slate-400 mt-0.5">
            Real-time Threat Monitoring, Blizzard Alerts & Mutual Station Aid
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => setIsModalOpen(true)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-red-600 hover:bg-red-500 text-white text-xs font-semibold tracking-wide transition-colors"
          >
            <PlusCircle className="h-3.5 w-3.5" />
            <span>Raise Alert</span>
          </button>
          <button
            onClick={fetchData}
            className="p-2 rounded-md bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors border border-slate-700/60"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin text-blue-400' : ''}`} />
          </button>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <KpiCard
          title="Critical Alerts"
          value={criticalCount}
          subtitle="Immediate threat to station safety"
          icon={AlertOctagon}
          variant={criticalCount > 0 ? 'critical' : 'default'}
        />
        <KpiCard
          title="Active Warnings"
          value={warningCount}
          subtitle="Advisories & environmental warnings"
          icon={AlertTriangle}
          variant={warningCount > 0 ? 'warning' : 'default'}
        />
        <KpiCard
          title="Resolved Incident Logs"
          value={resolvedCount}
          subtitle="Mitigated and closed incidents"
          icon={CheckCircle2}
          variant="healthy"
        />
      </div>

      {/* Alerts View */}
      {loading && alerts.length === 0 ? (
        <LoadingState message="Connecting to station telemetry alert grid..." />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchData} />
      ) : alerts.length === 0 ? (
        <EmptyState
          title="No active emergency alerts"
          message="Station telemetry reports normal operations. No blizzard warnings, SOS or breaches recorded."
        />
      ) : (
        <div className="space-y-3">
          <h2 className="text-xs font-semibold uppercase tracking-wider text-slate-400">
            Station Alert Incident Ledger ({alerts.length})
          </h2>

          <div className="divide-y divide-slate-800 rounded-lg border border-slate-800 bg-slate-900/50 backdrop-blur-sm overflow-hidden">
            {alerts.map((alert) => {
              const Icon = getAlertIcon(alert.type);
              return (
                <div
                  key={alert.id}
                  className={`p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4 transition-colors ${
                    alert.resolved
                      ? 'bg-slate-950/40 opacity-70'
                      : alert.severity === 'CRITICAL'
                      ? 'bg-red-950/20'
                      : 'bg-amber-950/20'
                  }`}
                >
                  <div className="flex items-start gap-3">
                    <div
                      className={`p-2 rounded-lg border shrink-0 ${
                        alert.severity === 'CRITICAL'
                          ? 'bg-red-900/40 border-red-700/60 text-red-400'
                          : 'bg-amber-900/40 border-amber-700/60 text-amber-400'
                      }`}
                    >
                      <Icon className="h-5 w-5" />
                    </div>

                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-sm text-slate-100 uppercase tracking-wide">
                          {alert.type}
                        </span>
                        <span
                          className={`text-[10px] font-semibold px-2 py-0.5 rounded border uppercase ${getSeverityStyle(
                            alert.severity
                          )}`}
                        >
                          {alert.severity}
                        </span>
                        {alert.resolved && (
                          <span className="text-[10px] bg-slate-800 text-slate-400 px-2 py-0.5 rounded font-mono">
                            RESOLVED
                          </span>
                        )}
                      </div>

                      <p className="text-xs text-slate-300 leading-relaxed max-w-2xl">{alert.message}</p>
                      <div className="text-[11px] text-slate-500 font-mono">
                        Logged: {new Date(alert.createdAt).toLocaleString()}
                      </div>
                    </div>
                  </div>

                  <div className="shrink-0 flex items-center gap-2 self-end sm:self-center">
                    {!alert.resolved && (
                      <button
                        onClick={() => handleResolveAlert(alert.id)}
                        className="px-3 py-1.5 rounded bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-400 border border-emerald-500/40 text-xs font-semibold transition-colors"
                      >
                        Acknowledge & Resolve
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Modal to Raise Alert */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-xs p-4">
          <div className="w-full max-w-md rounded-lg border border-slate-700 bg-slate-900 shadow-2xl p-6">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h3 className="text-sm font-bold uppercase tracking-wider text-slate-100 flex items-center gap-2">
                <AlertOctagon className="h-4 w-4 text-red-400" />
                <span>Dispatch Station Emergency Alert</span>
              </h3>
              <button onClick={() => setIsModalOpen(false)} className="text-slate-400 hover:text-slate-200">
                <X className="h-4 w-4" />
              </button>
            </div>
            <form onSubmit={handleCreateAlert} className="space-y-4 mt-4 text-xs">
              <div>
                <label className="block text-slate-400 mb-1 font-medium uppercase tracking-wider">
                  Incident Type
                </label>
                <select
                  value={alertType}
                  onChange={(e) => setAlertType(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded px-3 py-2 text-slate-200 focus:outline-none focus:border-red-500"
                >
                  <option value="BLIZZARD_WARNING">BLIZZARD WARNING (Severe Whiteout)</option>
                  <option value="SOS">SOS / DISTRESS BEACON</option>
                  <option value="GENERATOR_FAILURE">MAIN GENERATOR TRIP / POWER CRISIS</option>
                  <option value="SAFETY_STOCK_BREACH">SAFETY STOCK EXHAUSTION</option>
                  <option value="MEDICAL_EVACUATION">MEDICAL EVACUATION REQUEST</option>
                </select>
              </div>

              <div>
                <label className="block text-slate-400 mb-1 font-medium uppercase tracking-wider">
                  Severity Level
                </label>
                <select
                  value={severity}
                  onChange={(e) => setSeverity(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded px-3 py-2 text-slate-200 focus:outline-none focus:border-red-500 font-bold"
                >
                  <option value="CRITICAL" className="text-red-400 font-bold">
                    CRITICAL (Immediate Threat)
                  </option>
                  <option value="WARNING" className="text-amber-400 font-bold">
                    WARNING (Elevated Threat)
                  </option>
                  <option value="INFO" className="text-blue-400 font-medium">
                    INFO (Advisory)
                  </option>
                </select>
              </div>

              <div>
                <label className="block text-slate-400 mb-1 font-medium uppercase tracking-wider">
                  Incident Details & Protocol Directives
                </label>
                <textarea
                  required
                  rows={3}
                  placeholder="Detail severity, affected habitat sectors, immediate lockdown or muster orders..."
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded px-3 py-2 text-slate-200 focus:outline-none focus:border-red-500"
                />
              </div>

              <div className="pt-2 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-3 py-1.5 rounded text-slate-400 hover:bg-slate-800"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-3 py-1.5 rounded bg-red-600 hover:bg-red-500 text-white font-medium disabled:opacity-50"
                >
                  {submitting ? 'Broadcasting...' : 'Broadcast Alert'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
