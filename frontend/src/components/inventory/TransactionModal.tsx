import React, { useState } from 'react';
import { StockLevel, CreateTransactionRequest } from '../../types';
import { inventoryApi } from '../../api/api';
import { X, CheckCircle2, AlertCircle, ArrowRightLeft } from 'lucide-react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  stockLevel: StockLevel;
  onSuccess: () => void;
}

export const TransactionModal: React.FC<Props> = ({ isOpen, onClose, stockLevel, onSuccess }) => {
  const [type, setType] = useState<string>('CONSUMPTION');
  const [quantity, setQuantity] = useState<string>('');
  const [referenceId, setReferenceId] = useState<string>('');
  const [operatorId, setOperatorId] = useState<string>('STATION-OP');
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const parsedQty = parseFloat(quantity);
    if (isNaN(parsedQty) || parsedQty <= 0) {
      setError('Please enter a valid positive quantity.');
      return;
    }

    setLoading(true);
    try {
      const payload: CreateTransactionRequest = {
        stationId: stockLevel.stationId,
        inventoryItemId: stockLevel.inventoryItemId,
        type,
        quantity: parsedQty,
        source: navigator.onLine ? 'MANUAL_UI' : 'OFFLINE_DEVICE',
        referenceId: referenceId.trim() || undefined,
        operatorId: operatorId.trim() || 'OPERATOR',
      };

      if (!navigator.onLine) {
        // Offline: Queue to Dexie
        const { syncManager } = await import('../../services/syncManager');
        await syncManager.queueOperation('INVENTORY_TRANSACTION', payload);
        alert('Device offline: Transaction saved locally in offline queue — will sync automatically when connection returns.');
        onSuccess();
        onClose();
        return;
      }

      await inventoryApi.recordTransaction(payload);
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to record inventory transaction');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-xs p-4">
      <div className="w-full max-w-md rounded-lg border border-slate-700 bg-slate-900 shadow-2xl p-6">
        {/* Modal Header */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <ArrowRightLeft className="h-5 w-5 text-blue-400" />
            <div>
              <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-100">
                Log Stock Transaction
              </h3>
              <p className="text-xs text-slate-400 mt-0.5">
                {stockLevel.itemName} ({stockLevel.itemCode})
              </p>
            </div>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-200 p-1">
            <X className="h-4 w-4" />
          </button>
        </div>

        {error && (
          <div className="my-4 rounded-md bg-red-950/60 border border-red-800/80 p-3 text-xs text-red-300 flex items-start gap-2">
            <AlertCircle className="h-4 w-4 shrink-0 mt-0.5 text-red-400" />
            <span>{error}</span>
          </div>
        )}

        {/* Current Stock Context */}
        <div className="my-4 p-3 rounded bg-slate-950/60 border border-slate-800 flex justify-between text-xs">
          <div>
            <span className="text-slate-500">Current Stock:</span>
            <div className="font-bold text-slate-200 text-sm">
              {stockLevel.currentStock.toLocaleString()} {stockLevel.unit}
            </div>
          </div>
          <div>
            <span className="text-slate-500">Safety Threshold:</span>
            <div className="font-bold text-amber-400 text-sm">
              {stockLevel.safetyStock.toLocaleString()} {stockLevel.unit}
            </div>
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-slate-400 uppercase tracking-wider mb-1">
              Transaction Type
            </label>
            <select
              value={type}
              onChange={(e) => setType(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-md px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500 font-medium"
            >
              <option value="CONSUMPTION">CONSUMPTION (Expend stock)</option>
              <option value="RECEIPT">RECEIPT (Add stock)</option>
              <option value="TRANSFER_OUT">TRANSFER OUT (Dispatch)</option>
              <option value="TRANSFER_IN">TRANSFER IN (Receive)</option>
              <option value="ADJUSTMENT">ADJUSTMENT (Audit correction)</option>
              <option value="WASTE">WASTE (Damaged/Spoiled)</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-400 uppercase tracking-wider mb-1">
              Quantity ({stockLevel.unit})
            </label>
            <input
              type="number"
              step="any"
              min="0.01"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              placeholder="e.g. 50.00"
              required
              className="w-full bg-slate-950 border border-slate-700 rounded-md px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500 font-mono"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-slate-400 uppercase tracking-wider mb-1">
                Reference ID
              </label>
              <input
                type="text"
                value={referenceId}
                onChange={(e) => setReferenceId(e.target.value)}
                placeholder="e.g. LOG-2026-001"
                className="w-full bg-slate-950 border border-slate-700 rounded-md px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-400 uppercase tracking-wider mb-1">
                Operator ID
              </label>
              <input
                type="text"
                value={operatorId}
                onChange={(e) => setOperatorId(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700 rounded-md px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
              />
            </div>
          </div>

          <div className="pt-2 flex justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-md text-xs font-medium text-slate-400 hover:bg-slate-800 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 rounded-md text-xs font-medium bg-blue-600 hover:bg-blue-500 text-white transition-colors flex items-center gap-1.5 disabled:opacity-50"
            >
              {loading ? 'Submitting...' : 'Commit Mutation'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
