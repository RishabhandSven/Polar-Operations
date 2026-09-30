import React, { createContext, useContext, useState, useEffect } from 'react';
import { Station, STATIONS } from '../types';
import { syncManager, SyncProgress } from '../services/syncManager';

interface StationContextType {
  selectedStation: Station;
  setSelectedStation: (station: Station) => void;
  isOnline: boolean;
  setIsOnlineManual: (online: boolean) => void;
  syncProgress: SyncProgress;
  triggerSync: () => Promise<any>;
}

const StationContext = createContext<StationContextType | undefined>(undefined);

export const StationProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [selectedStation, setSelectedStation] = useState<Station>(STATIONS.BHARATI);
  const [isOnline, setIsOnline] = useState<boolean>(navigator.onLine);
  const [syncProgress, setSyncProgress] = useState<SyncProgress>({
    isSyncing: false,
    lastSyncedAt: null,
    pendingCount: 0,
    syncError: null,
  });

  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      // Auto sync when connection restored
      syncManager.sync();
    };
    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    const unsubscribe = syncManager.subscribe((progress) => {
      setSyncProgress(progress);
    });

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      unsubscribe();
    };
  }, []);

  const setIsOnlineManual = (online: boolean) => {
    setIsOnline(online);
    if (online) {
      syncManager.sync();
    }
  };

  const triggerSync = async () => {
    return await syncManager.sync();
  };

  return (
    <StationContext.Provider
      value={{
        selectedStation,
        setSelectedStation,
        isOnline,
        setIsOnlineManual,
        syncProgress,
        triggerSync,
      }}
    >
      {children}
    </StationContext.Provider>
  );
};

export const useStationContext = () => {
  const context = useContext(StationContext);
  if (!context) {
    throw new Error('useStationContext must be used within a StationProvider');
  }
  return context;
};
