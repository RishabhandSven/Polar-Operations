import React from 'react';
import { TransferRecommendation } from '../../types';
import { ArrowRight, Truck } from 'lucide-react';

interface Props {
  recommendations: TransferRecommendation[];
}

export const TransferRecommendationsCard: React.FC<Props> = ({ recommendations }) => {
  return (
    <div className="rounded-lg border border-slate-800/80 bg-slate-900/50 p-4 backdrop-blur-sm">
      <div className="flex items-center justify-between pb-3 border-b border-slate-800/60">
        <div className="flex items-center gap-2">
          <Truck className="h-4 w-4 text-blue-400" />
          <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-300">
            Inter-Station Transfer Suggestions
          </h3>
        </div>
        <span className="text-[11px] text-slate-500">Optimized Logistics</span>
      </div>

      {recommendations.length === 0 ? (
        <div className="py-6 text-center text-xs text-slate-500">
          No active inter-station transfers recommended at this time.
        </div>
      ) : (
        <div className="divide-y divide-slate-800/60">
          {recommendations.map((rec, idx) => (
            <div key={idx} className="py-3 first:pt-3 last:pb-0 space-y-1.5">
              <div className="flex items-center justify-between text-xs">
                <div className="flex items-center gap-2 font-semibold text-slate-200">
                  <span className="text-blue-400">{rec.sourceStationName}</span>
                  <ArrowRight className="h-3.5 w-3.5 text-slate-500" />
                  <span className="text-emerald-400">{rec.targetStationName}</span>
                </div>
                <span className="font-bold text-amber-400 bg-amber-950/40 px-2 py-0.5 rounded border border-amber-800/50">
                  {rec.recommendedQuantity.toLocaleString()} {rec.unit}
                </span>
              </div>
              <div className="text-xs text-slate-300 font-medium">
                {rec.itemName} <span className="text-slate-500">({rec.itemCode})</span>
              </div>
              <div className="text-[11px] text-slate-400 bg-slate-950/60 p-2 rounded border border-slate-800/80">
                {rec.rationale}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
