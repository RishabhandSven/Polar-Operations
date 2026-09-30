import React, { useState } from 'react';
import { useStationContext } from '../../context/StationContext';
import { STATIONS } from '../../types';
import { ChevronDown, User } from 'lucide-react';
import { SyncDrawer } from './SyncDrawer';
import { DemoTourModal } from '../common/DemoTourModal';

export const Header: React.FC = () => {
  const { selectedStation, setSelectedStation, isOnline, setIsOnlineManual, syncProgress } = useStationContext();
  const [syncDrawerOpen, setSyncDrawerOpen] = useState(false);
  const [demoModalOpen, setDemoModalOpen] = useState(false);

  return (
    <header className="h-16 bg-slate-900/80 border-b border-slate-800/80 px-6 flex items-center justify-between backdrop-blur-md sticky top-0 z-30">
      {/* Station Selector */}
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-2">
          <span className="text-xs uppercase font-medium text-slate-400">Station:</span>
          <div className="relative">
            <select
              value={selectedStation.code}
              onChange={(e) => {
                const code = e.target.value;
                const match = Object.values(STATIONS).find((s) => s.code === code);
                if (match) setSelectedStation(match);
              }}
              className="appearance-none bg-slate-800 border border-slate-700/80 text-slate-100 text-xs font-semibold rounded-md pl-3 pr-8 py-1.5 focus:outline-none focus:border-blue-500 cursor-pointer"
            >
              {Object.values(STATIONS).map((s) => (
                <option key={s.id} value={s.code}>
                  {s.name} ({s.code})
                </option>
              ))}
            </select>
            <ChevronDown className="absolute right-2.5 top-2.5 h-3.5 w-3.5 text-slate-400 pointer-events-none" />
          </div>
        </div>

        <div className="hidden sm:flex items-center text-xs text-slate-500 border-l border-slate-800 pl-4 gap-2">
          <span>LAT: {selectedStation.latitude}°</span>
          <span>LON: {selectedStation.longitude}°</span>
        </div>
      </div>

      {/* Connectivity, Demo & Operator Status */}
      <div className="flex items-center gap-4">
        {/* SIH Demo Tour Button */}
        <button
          onClick={() => setDemoModalOpen(true)}
          className="flex items-center gap-1.5 px-2.5 py-1 rounded bg-blue-600/20 hover:bg-blue-600/30 text-blue-400 border border-blue-500/40 text-xs font-bold uppercase tracking-wider transition-colors"
        >
          <span className="h-1.5 w-1.5 rounded-full bg-blue-400 animate-ping"></span>
          <span>SIH Demo Guide</span>
        </button>

        {/* Connection & Sync status */}
        <div className="flex items-center gap-2">
          {/* Simulated Offline Toggle for Demo */}
          <button
            onClick={() => setIsOnlineManual(!isOnline)}
            title="Toggle Network Simulator"
            className="hidden sm:inline-flex text-[10px] uppercase font-bold px-2 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition-colors"
          >
            {isOnline ? 'Simulate Offline' : 'Restore Online'}
          </button>

          {/* Sync Button & Drawer Trigger */}
          <button
            onClick={() => setSyncDrawerOpen(true)}
            className="flex items-center gap-2 px-2.5 py-1 rounded-full bg-slate-800/80 hover:bg-slate-700 border border-slate-700/60 text-xs transition-colors cursor-pointer"
          >
            <span className="relative flex h-2 w-2">
              {isOnline ? (
                <>
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
                </>
              ) : (
                <span className="relative inline-flex rounded-full h-2 w-2 bg-red-500"></span>
              )}
            </span>
            <span className={`font-semibold ${isOnline ? 'text-emerald-400' : 'text-red-400'}`}>
              {isOnline ? 'ONLINE' : 'OFFLINE'}
            </span>
            {syncProgress.pendingCount > 0 && (
              <span className="ml-1 px-1.5 py-0.2 rounded-full bg-amber-500/20 border border-amber-500/40 text-amber-400 font-mono text-[10px]">
                {syncProgress.pendingCount} pending
              </span>
            )}
          </button>
        </div>

        {/* Operator info */}
        <div className="flex items-center gap-2 border-l border-slate-800 pl-4 text-xs text-slate-300">
          <div className="h-7 w-7 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-slate-300">
            <User className="h-3.5 w-3.5" />
          </div>
          <div className="hidden md:block">
            <div className="font-medium text-slate-200 leading-none">Station Commander</div>
            <div className="text-[10px] text-slate-400 leading-none mt-1">OPERATIONAL DEMO</div>
          </div>
        </div>
      </div>

      <SyncDrawer isOpen={syncDrawerOpen} onClose={() => setSyncDrawerOpen(false)} />
      <DemoTourModal isOpen={demoModalOpen} onClose={() => setDemoModalOpen(false)} />
    </header>
  );
};
