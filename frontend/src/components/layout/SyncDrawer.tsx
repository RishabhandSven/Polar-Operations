import React, { useEffect, useState } from 'react';
import { localDb, PendingOperation } from '../../db/localDb';
import { syncManager } from '../../services/syncManager';
import { X, RefreshCw, CheckCircle2, AlertCircle, Clock, Send } from 'lucide-react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
}

export const SyncDrawer: React.FC<Props> = ({ isOpen, onClose }) => {
  const [operations, setOperations] = useState<PendingOperation[]>([]);
  const [loading, setLoading] = useState(false);
  const [syncing, setSyncing] = useState(false);

  const loadOps = async () => {
    setLoading(true);
    try {
      const all = await localDb.pendingOperations.orderBy('createdAt').reverse().toArray();
      setOperations(all);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      loadOps();
    }
  }, [isOpen]);

  const handleSyncNow = async () => {
    setSyncing(true);
    try {
      await syncManager.sync();
      await loadOps();
    } finally {
      setSyncing(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/60 backdrop-blur-xs">
      <div className="w-full max-w-md bg-slate-900 border-l border-slate-800 h-full p-6 flex flex-col shadow-2xl">
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <RefreshCw className="h-5 w-5 text-blue-400" />
            <div>
              <h3 className="text-sm font-bold uppercase tracking-wider text-slate-100">
                Offline Mutation Queue
              </h3>
              <p className="text-xs text-slate-400 mt-0.5">Dexie / IndexedDB Store & Forward</p>
            </div>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-200 p-1">
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Action Bar */}
        <div className="py-4 flex items-center justify-between border-b border-slate-800 text-xs">
          <span className="text-slate-400 font-medium">
            Pending Queue:{' '}
            <span className="font-bold text-amber-400">
              {operations.filter((o) => o.status === 'PENDING').length}
            </span>
          </span>
          <button
            onClick={handleSyncNow}
            disabled={syncing}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded bg-blue-600 hover:bg-blue-500 text-white font-semibold transition-colors disabled:opacity-50"
          >
            <Send className="h-3.5 w-3.5" />
            <span>{syncing ? 'Syncing...' : 'Sync Now'}</span>
          </button>
        </div>

        {/* Queue Items */}
        <div className="flex-1 overflow-y-auto py-4 space-y-3">
          {operations.length === 0 ? (
            <div className="py-16 text-center text-xs text-slate-500 italic">
              No offline operations recorded in local storage.
            </div>
          ) : (
            operations.map((op) => (
              <div
                key={op.id}
                className="p-3 rounded-lg border border-slate-800 bg-slate-950/70 text-xs space-y-2"
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-slate-200 font-mono">{op.operation}</span>
                  <span
                    className={`px-2 py-0.5 rounded text-[10px] font-semibold border ${
                      op.status === 'SYNCED'
                        ? 'bg-emerald-950/60 text-emerald-400 border-emerald-800/60'
                        : op.status === 'FAILED'
                        ? 'bg-red-950/60 text-red-400 border-red-800/60'
                        : op.status === 'SYNCING'
                        ? 'bg-blue-950/60 text-blue-400 border-blue-800/60 animate-pulse'
                        : 'bg-amber-950/60 text-amber-400 border-amber-800/60'
                    }`}
                  >
                    {op.status}
                  </span>
                </div>

                <div className="text-[11px] text-slate-400 space-y-0.5">
                  <div>
                    Type: <span className="font-medium text-slate-200">{op.payload?.type || 'N/A'}</span> (Qty: {op.payload?.quantity || '0'})
                  </div>
                  <div>
                    Tx ID: <span className="font-mono text-[10px] text-slate-500">{op.clientTransactionId}</span>
                  </div>
                  <div className="text-[10px] text-slate-500">
                    Created: {new Date(op.createdAt).toLocaleTimeString()}
                  </div>
                </div>

                {op.errorMessage && (
                  <div className="p-2 rounded bg-red-950/40 border border-red-900/40 text-[11px] text-red-400">
                    {op.errorMessage}
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
