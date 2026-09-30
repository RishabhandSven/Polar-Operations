import React, { useState } from 'react';
import { InventoryItem, CreateCargoConsignmentRequest } from '../../types';
import { cargoApi } from '../../api/api';
import { X, Plus, Trash2, Ship, AlertCircle } from 'lucide-react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  stationId: string;
  stationName: string;
  availableItems: InventoryItem[];
  onSuccess: () => void;
}

export const CreateCargoModal: React.FC<Props> = ({
  isOpen,
  onClose,
  stationId,
  stationName,
  availableItems,
  onSuccess,
}) => {
  const [trackingNumber, setTrackingNumber] = useState('');
  const [status, setStatus] = useState('PLANNED');
  const [departureDate, setDepartureDate] = useState('');
  const [estimatedArrival, setEstimatedArrival] = useState('');
  const [items, setItems] = useState<{ inventoryItemId: string; quantity: number }[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const addItemRow = () => {
    if (availableItems.length === 0) return;
    setItems([...items, { inventoryItemId: availableItems[0].id, quantity: 100 }]);
  };

  const removeItemRow = (index: number) => {
    setItems(items.filter((_, i) => i !== index));
  };

  const updateItemRow = (index: number, field: 'inventoryItemId' | 'quantity', value: any) => {
    const updated = [...items];
    updated[index] = { ...updated[index], [field]: value };
    setItems(updated);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!trackingNumber.trim()) {
      setError('Tracking number is required.');
      return;
    }
    if (!estimatedArrival) {
      setError('Estimated arrival date is required.');
      return;
    }

    setLoading(true);
    try {
      const payload: CreateCargoConsignmentRequest = {
        trackingNumber: trackingNumber.trim(),
        destinationStationId: stationId,
        status,
        departureDate: departureDate || undefined,
        estimatedArrival,
        initialItems: items.length > 0 ? items : undefined,
      };

      await cargoApi.createConsignment(payload);
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to create cargo consignment');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-xs p-4">
      <div className="w-full max-w-lg rounded-lg border border-slate-700 bg-slate-900 shadow-2xl p-6 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <Ship className="h-5 w-5 text-blue-400" />
            <div>
              <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-100">
                Create Cargo Consignment
              </h3>
              <p className="text-xs text-slate-400 mt-0.5">Destination: {stationName}</p>
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

        <form onSubmit={handleSubmit} className="space-y-4 mt-4 text-xs">
          <div>
            <label className="block text-slate-400 font-medium uppercase tracking-wider mb-1">
              Tracking Number
            </label>
            <input
              type="text"
              required
              placeholder="e.g. VOY-2026-EXP-001"
              value={trackingNumber}
              onChange={(e) => setTrackingNumber(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-md px-3 py-2 text-slate-200 focus:outline-none focus:border-blue-500 font-mono"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-400 font-medium uppercase tracking-wider mb-1">
                Initial Status
              </label>
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700 rounded-md px-3 py-2 text-slate-200 focus:outline-none focus:border-blue-500"
              >
                <option value="PLANNED">PLANNED</option>
                <option value="IN_TRANSIT">IN_TRANSIT</option>
                <option value="ARRIVED">ARRIVED</option>
              </select>
            </div>
            <div>
              <label className="block text-slate-400 font-medium uppercase tracking-wider mb-1">
                Departure Date
              </label>
              <input
                type="date"
                value={departureDate}
                onChange={(e) => setDepartureDate(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700 rounded-md px-3 py-2 text-slate-200 focus:outline-none focus:border-blue-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-slate-400 font-medium uppercase tracking-wider mb-1">
              Estimated Arrival Date
            </label>
            <input
              type="date"
              required
              value={estimatedArrival}
              onChange={(e) => setEstimatedArrival(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-md px-3 py-2 text-slate-200 focus:outline-none focus:border-blue-500"
            />
          </div>

          {/* Manifest Items Section */}
          <div className="pt-2 border-t border-slate-800">
            <div className="flex items-center justify-between mb-2">
              <span className="font-semibold text-slate-300 uppercase tracking-wider">
                Manifest Manifest Items ({items.length})
              </span>
              <button
                type="button"
                onClick={addItemRow}
                className="inline-flex items-center gap-1 text-[11px] text-blue-400 hover:text-blue-300"
              >
                <Plus className="h-3.5 w-3.5" />
                <span>Add Item</span>
              </button>
            </div>

            {items.map((item, idx) => (
              <div key={idx} className="flex items-center gap-2 mb-2">
                <select
                  value={item.inventoryItemId}
                  onChange={(e) => updateItemRow(idx, 'inventoryItemId', e.target.value)}
                  className="flex-1 bg-slate-950 border border-slate-700 rounded px-2 py-1.5 text-slate-200"
                >
                  {availableItems.map((ai) => (
                    <option key={ai.id} value={ai.id}>
                      {ai.name} ({ai.itemCode})
                    </option>
                  ))}
                </select>
                <input
                  type="number"
                  min="0.01"
                  step="any"
                  value={item.quantity}
                  onChange={(e) => updateItemRow(idx, 'quantity', parseFloat(e.target.value) || 0)}
                  placeholder="Qty"
                  className="w-24 bg-slate-950 border border-slate-700 rounded px-2 py-1.5 text-slate-200 font-mono text-right"
                />
                <button
                  type="button"
                  onClick={() => removeItemRow(idx)}
                  className="p-1 text-slate-500 hover:text-red-400"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </button>
              </div>
            ))}
          </div>

          <div className="pt-4 border-t border-slate-800 flex justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-md text-slate-400 hover:bg-slate-800 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 rounded-md bg-blue-600 hover:bg-blue-500 text-white font-medium transition-colors disabled:opacity-50"
            >
              {loading ? 'Creating...' : 'Create Consignment'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
