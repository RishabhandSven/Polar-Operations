import React from 'react';
import { LucideIcon } from 'lucide-react';

interface KpiCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: LucideIcon;
  variant?: 'default' | 'critical' | 'warning' | 'healthy' | 'info';
  onClick?: () => void;
}

export const KpiCard: React.FC<KpiCardProps> = ({
  title,
  value,
  subtitle,
  icon: Icon,
  variant = 'default',
  onClick,
}) => {
  const variantStyles = {
    default: 'border-slate-800 bg-slate-900/60 text-slate-100',
    critical: 'border-red-900/50 bg-red-950/20 text-red-300',
    warning: 'border-amber-900/50 bg-amber-950/20 text-amber-300',
    healthy: 'border-emerald-900/50 bg-emerald-950/20 text-emerald-300',
    info: 'border-blue-900/50 bg-blue-950/20 text-blue-300',
  };

  const iconColors = {
    default: 'text-slate-400 bg-slate-800/80',
    critical: 'text-red-400 bg-red-900/40',
    warning: 'text-amber-400 bg-amber-900/40',
    healthy: 'text-emerald-400 bg-emerald-900/40',
    info: 'text-blue-400 bg-blue-900/40',
  };

  return (
    <div
      onClick={onClick}
      className={`rounded-lg border p-4 shadow-sm backdrop-blur-sm transition-all ${variantStyles[variant]} ${
        onClick ? 'cursor-pointer hover:border-slate-700 hover:bg-slate-800/50' : ''
      }`}
    >
      <div className="flex items-center justify-between">
        <span className="text-xs font-medium uppercase tracking-wider text-slate-400">{title}</span>
        <div className={`rounded-md p-1.5 ${iconColors[variant]}`}>
          <Icon className="h-4 w-4" />
        </div>
      </div>
      <div className="mt-3 flex items-baseline gap-2">
        <span className="text-2xl font-bold tracking-tight">{value}</span>
        {subtitle && <span className="text-xs text-slate-400">{subtitle}</span>}
      </div>
    </div>
  );
};
