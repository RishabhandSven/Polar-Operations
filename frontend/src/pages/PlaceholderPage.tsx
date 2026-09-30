import React from 'react';
import { LucideIcon } from 'lucide-react';

interface Props {
  title: string;
  subtitle: string;
  icon: LucideIcon;
  phase: string;
}

export const PlaceholderPage: React.FC<Props> = ({ title, subtitle, icon: Icon, phase }) => {
  return (
    <div className="rounded-lg border border-slate-800 bg-slate-900/40 p-12 text-center text-slate-400 backdrop-blur-sm space-y-4">
      <div className="mx-auto h-12 w-12 rounded-full bg-slate-800/80 border border-slate-700 flex items-center justify-center text-blue-400">
        <Icon className="h-6 w-6" />
      </div>
      <div>
        <h2 className="text-base font-bold text-slate-100 uppercase tracking-wide">{title}</h2>
        <p className="text-xs text-slate-400 mt-1 max-w-md mx-auto">{subtitle}</p>
      </div>
      <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-950/50 border border-blue-800/60 text-blue-400 text-xs font-semibold uppercase tracking-wider">
        <span>Scheduled for {phase}</span>
      </div>
    </div>
  );
};
