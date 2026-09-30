import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useStationContext } from '../../context/StationContext';
import { STATIONS } from '../../types';
import {
  Compass,
  X,
  ArrowRight,
  CheckCircle2,
  Clock,
  Ship,
  Boxes,
  WifiOff,
  Send,
  Zap,
} from 'lucide-react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
}

export const DemoTourModal: React.FC<Props> = ({ isOpen, onClose }) => {
  const navigate = useNavigate();
  const { setSelectedStation, setIsOnlineManual, isOnline, triggerSync } = useStationContext();

  if (!isOpen) return null;

  const demoSteps = [
    {
      num: 1,
      title: 'Select Operational Base: Bharati Station',
      desc: 'Set mission context to Bharati Station (Antarctica, 69°S, 76°E) to inspect station telemetry.',
      icon: Compass,
      actionLabel: 'Select Bharati',
      onAction: () => {
        setSelectedStation(STATIONS.BHARATI);
        navigate('/');
        onClose();
      },
    },
    {
      num: 2,
      title: 'Simulate +10 Days Resupply Sea-Ice Delay',
      desc: 'Severe polar ocean pack ice delays incoming tanker. Recalculate days to safety and observe risk elevate to CRITICAL.',
      icon: Clock,
      actionLabel: 'Open Intelligence Engine',
      onAction: () => {
        setSelectedStation(STATIONS.BHARATI);
        navigate('/intelligence');
        onClose();
      },
    },
    {
      num: 3,
      title: 'Inspect Inter-Station Transfer Recommendation',
      desc: 'Deterministic optimizer analyzes Maitri station fuel surplus and suggests optimal emergency inter-station transfer.',
      icon: Zap,
      actionLabel: 'View Transfer Suggestion',
      onAction: () => {
        navigate('/');
        onClose();
      },
    },
    {
      num: 4,
      title: 'Process Arrived Cargo Manifest',
      desc: 'Vessel VOY-2026-BHA-004 arrived at ice pier. Review manifest items and execute atomic station stock receipt.',
      icon: Ship,
      actionLabel: 'Open Cargo Management',
      onAction: () => {
        navigate('/cargo');
        onClose();
      },
    },
    {
      num: 5,
      title: 'Inspect Inventory Stock Restoration',
      desc: 'Verify 3,000L Fuel and 500 units Food are atomically booked into stock levels and transaction ledger.',
      icon: Boxes,
      actionLabel: 'View Station Catalogue',
      onAction: () => {
        navigate('/inventory');
        onClose();
      },
    },
    {
      num: 6,
      title: 'Store & Forward Offline Synchronization',
      desc: 'Toggle offline simulator, record local inventory consumption into Dexie queue, restore connection, and verify idempotent batch push.',
      icon: WifiOff,
      actionLabel: isOnline ? 'Simulate Offline' : 'Restore & Push Sync',
      onAction: async () => {
        if (isOnline) {
          setIsOnlineManual(false);
          navigate('/inventory');
        } else {
          setIsOnlineManual(true);
          await triggerSync();
        }
        onClose();
      },
    },
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-xs p-4">
      <div className="w-full max-w-2xl rounded-xl border border-slate-700 bg-slate-900 shadow-2xl p-6 max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-blue-600/20 border border-blue-500/40 text-blue-400">
              <Compass className="h-6 w-6" />
            </div>
            <div>
              <h2 className="text-base font-bold uppercase tracking-wider text-slate-100">
                SIH 2026 — PolarOps Operational Flow
              </h2>
              <p className="text-xs text-slate-400 mt-0.5">
                Problem Statement SIH26062: End-to-End Evaluation Workflow
              </p>
            </div>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-200 p-1">
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Steps List */}
        <div className="flex-1 overflow-y-auto py-4 space-y-3">
          {demoSteps.map((step) => {
            const Icon = step.icon;
            return (
              <div
                key={step.num}
                className="p-3.5 rounded-lg border border-slate-800/80 bg-slate-950/70 hover:border-slate-700 transition-colors flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs"
              >
                <div className="flex items-start gap-3">
                  <div className="h-7 w-7 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center font-bold text-blue-400 shrink-0">
                    {step.num}
                  </div>
                  <div>
                    <h3 className="font-bold text-slate-200 text-xs uppercase tracking-wide flex items-center gap-1.5">
                      <Icon className="h-3.5 w-3.5 text-blue-400" />
                      <span>{step.title}</span>
                    </h3>
                    <p className="text-slate-400 mt-1 leading-relaxed text-[11px]">{step.desc}</p>
                  </div>
                </div>

                <button
                  onClick={step.onAction}
                  className="px-3 py-1.5 rounded-md bg-blue-600/20 hover:bg-blue-600/30 text-blue-300 border border-blue-500/40 font-semibold text-[11px] transition-colors shrink-0 flex items-center gap-1 self-end sm:self-center"
                >
                  <span>{step.actionLabel}</span>
                  <ArrowRight className="h-3 w-3" />
                </button>
              </div>
            );
          })}
        </div>

        {/* Footer */}
        <div className="pt-3 border-t border-slate-800 flex items-center justify-between text-xs text-slate-500">
          <span>All data driven from Spring Boot 3.x & PostgreSQL 16</span>
          <button
            onClick={onClose}
            className="px-4 py-1.5 rounded bg-slate-800 hover:bg-slate-700 text-slate-200 font-medium"
          >
            Close Guide
          </button>
        </div>
      </div>
    </div>
  );
};
