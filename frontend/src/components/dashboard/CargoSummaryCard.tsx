import React from 'react';
import { CargoConsignment } from '../../types';
import { Ship, Clock, CheckCircle2, ArrowUpRight } from 'lucide-react';
import { Link } from 'react-router-dom';

interface Props {
  consignments: CargoConsignment[];
}

export const CargoSummaryCard: React.FC<Props> = ({ consignments }) => {
  const arrived = consignments.filter((c) => c.status === 'ARRIVED').length;
  const inTransit = consignments.filter((c) => c.status === 'IN_TRANSIT').length;
  const planned = consignments.filter((c) => c.status === 'PLANNED').length;
  const received = consignments.filter((c) => c.status === 'RECEIVED').length;

  return (
    <div className="rounded-lg border border-slate-800/80 bg-slate-900/50 p-4 backdrop-blur-sm">
      <div className="flex items-center justify-between pb-3 border-b border-slate-800/60">
        <div className="flex items-center gap-2">
          <Ship className="h-4 w-4 text-cyan-400" />
          <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-300">
            Cargo & Resupply Pipelines
          </h3>
        </div>
        <Link to="/cargo" className="text-[11px] text-blue-400 hover:underline flex items-center gap-0.5">
          <span>View Manifests</span>
          <ArrowUpRight className="h-3 w-3" />
        </Link>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 my-3">
        <div className="p-2.5 rounded bg-slate-950/80 border border-slate-800">
          <div className="text-[10px] uppercase font-semibold text-slate-400">Arrived</div>
          <div className="text-xl font-bold text-amber-400 mt-0.5">{arrived}</div>
        </div>
        <div className="p-2.5 rounded bg-slate-950/80 border border-slate-800">
          <div className="text-[10px] uppercase font-semibold text-slate-400">In Transit</div>
          <div className="text-xl font-bold text-blue-400 mt-0.5">{inTransit}</div>
        </div>
        <div className="p-2.5 rounded bg-slate-950/80 border border-slate-800">
          <div className="text-[10px] uppercase font-semibold text-slate-400">Planned</div>
          <div className="text-xl font-bold text-slate-300 mt-0.5">{planned}</div>
        </div>
        <div className="p-2.5 rounded bg-slate-950/80 border border-slate-800">
          <div className="text-[10px] uppercase font-semibold text-slate-400">Received</div>
          <div className="text-xl font-bold text-emerald-400 mt-0.5">{received}</div>
        </div>
      </div>

      <div className="space-y-2 mt-3 pt-3 border-t border-slate-800/60">
        {consignments.slice(0, 3).map((c) => (
          <div key={c.id} className="flex items-center justify-between text-xs py-1">
            <div className="flex items-center gap-2">
              <span className="font-mono font-medium text-slate-200">{c.trackingNumber}</span>
              <span className="text-[10px] text-slate-400">ETA: {c.estimatedArrival}</span>
            </div>
            <span
              className={`text-[10px] font-semibold px-2 py-0.5 rounded uppercase border ${
                c.status === 'ARRIVED'
                  ? 'bg-amber-950/60 text-amber-400 border-amber-800/60'
                  : c.status === 'IN_TRANSIT'
                  ? 'bg-blue-950/60 text-blue-400 border-blue-800/60'
                  : 'bg-emerald-950/60 text-emerald-400 border-emerald-800/60'
              }`}
            >
              {c.status}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};
