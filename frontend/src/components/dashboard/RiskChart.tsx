import React from 'react';
import { ResponsiveContainer, PieChart, Pie, Cell, Tooltip } from 'recharts';
import { StockRiskAssessment } from '../../types';

interface RiskChartProps {
  assessments: StockRiskAssessment[];
}

export const RiskChart: React.FC<RiskChartProps> = ({ assessments }) => {
  const criticalCount = assessments.filter((a) => a.riskLevel === 'CRITICAL').length;
  const warningCount = assessments.filter((a) => a.riskLevel === 'WARNING').length;
  const healthyCount = assessments.filter((a) => a.riskLevel === 'HEALTHY').length;

  const data = [
    { name: 'Critical', value: criticalCount, color: '#ef4444' },
    { name: 'Warning', value: warningCount, color: '#f59e0b' },
    { name: 'Healthy', value: healthyCount, color: '#10b981' },
  ].filter((d) => d.value > 0);

  return (
    <div className="rounded-lg border border-slate-800/80 bg-slate-900/50 p-4 backdrop-blur-sm flex flex-col h-[380px]">
      <div className="flex items-center justify-between pb-3">
        <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-300">
          Inventory Risk Breakdown
        </h3>
        <span className="text-[11px] text-slate-500">Real-time telemetry</span>
      </div>

      {data.length === 0 ? (
        <div className="flex-1 flex items-center justify-center text-xs text-slate-500">
          No inventory intelligence items available
        </div>
      ) : (
        <div className="flex-1 flex flex-col items-center justify-center">
          <div className="h-56 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={data}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={85}
                  paddingAngle={5}
                  dataKey="value"
                >
                  {data.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} stroke="#0f172a" strokeWidth={2} />
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#0f172a',
                    borderColor: '#334155',
                    borderRadius: '0.375rem',
                    fontSize: '12px',
                    color: '#f8fafc',
                  }}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>

          <div className="flex items-center gap-6 mt-2">
            <div className="flex items-center gap-2 text-xs">
              <span className="h-2.5 w-2.5 rounded-full bg-red-500" />
              <span className="text-slate-400">Critical ({criticalCount})</span>
            </div>
            <div className="flex items-center gap-2 text-xs">
              <span className="h-2.5 w-2.5 rounded-full bg-amber-500" />
              <span className="text-slate-400">Warning ({warningCount})</span>
            </div>
            <div className="flex items-center gap-2 text-xs">
              <span className="h-2.5 w-2.5 rounded-full bg-emerald-500" />
              <span className="text-slate-400">Healthy ({healthyCount})</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
