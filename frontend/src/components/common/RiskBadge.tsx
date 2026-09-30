import React from 'react';
import { RiskLevel } from '../../types';

interface RiskBadgeProps {
  risk: RiskLevel;
  className?: string;
}

export const RiskBadge: React.FC<RiskBadgeProps> = ({ risk, className = '' }) => {
  const styles: Record<RiskLevel, string> = {
    CRITICAL: 'bg-red-950/80 text-red-400 border-red-800/60 font-semibold',
    WARNING: 'bg-amber-950/80 text-amber-400 border-amber-800/60 font-medium',
    HEALTHY: 'bg-emerald-950/80 text-emerald-400 border-emerald-800/60 font-medium',
  };

  return (
    <span
      className={`inline-flex items-center px-2 py-0.5 rounded text-xs border tracking-wider uppercase ${styles[risk]} ${className}`}
    >
      {risk}
    </span>
  );
};
