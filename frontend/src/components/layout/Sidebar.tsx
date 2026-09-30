import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Boxes,
  BrainCircuit,
  Ship,
  Users,
  AlertOctagon,
  Radio,
  Compass,
} from 'lucide-react';

export const Sidebar: React.FC = () => {
  const navItems = [
    { to: '/', label: 'Overview', icon: LayoutDashboard, ready: true },
    { to: '/inventory', label: 'Inventory', icon: Boxes, ready: true },
    { to: '/intelligence', label: 'Intelligence', icon: BrainCircuit, ready: true },
    { to: '/cargo', label: 'Cargo', icon: Ship, ready: true },
    { to: '/personnel', label: 'Personnel', icon: Users, ready: true },
    { to: '/emergency', label: 'Emergency', icon: AlertOctagon, ready: true },
  ];

  return (
    <aside className="w-64 bg-slate-950 border-r border-slate-800/80 flex flex-col shrink-0">
      {/* Branding */}
      <div className="h-16 flex items-center px-6 gap-3 border-b border-slate-800/80">
        <div className="h-9 w-9 rounded-lg bg-blue-600/20 border border-blue-500/40 flex items-center justify-center text-blue-400">
          <Compass className="h-5 w-5" />
        </div>
        <div>
          <span className="font-bold tracking-wider text-sm text-slate-100 uppercase">POLAROPS</span>
          <span className="block text-[10px] text-slate-400 uppercase tracking-widest">Mission Control</span>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-3 py-4 space-y-1">
        <div className="px-3 pb-2 text-[10px] font-semibold text-slate-500 uppercase tracking-wider">
          Operations
        </div>
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                `flex items-center justify-between px-3 py-2 rounded-md text-xs font-medium transition-all ${
                  isActive
                    ? 'bg-blue-600/20 text-blue-400 border border-blue-500/30'
                    : 'text-slate-400 hover:bg-slate-900 hover:text-slate-200'
                }`
              }
            >
              <div className="flex items-center gap-3">
                <Icon className="h-4 w-4" />
                <span>{item.label}</span>
              </div>
              {!item.ready && (
                <span className="text-[9px] uppercase px-1.5 py-0.5 rounded bg-slate-800/80 text-slate-400 font-normal">
                  Phase 5
                </span>
              )}
            </NavLink>
          );
        })}
      </nav>

      {/* Footer Info */}
      <div className="p-4 border-t border-slate-800/80 bg-slate-950/50 text-[11px] text-slate-500 flex items-center gap-2">
        <Radio className="h-3.5 w-3.5 text-emerald-400 animate-pulse" />
        <span>SIH 2026 • PS 26062</span>
      </div>
    </aside>
  );
};
