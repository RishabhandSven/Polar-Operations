import React, { useEffect, useState, useMemo } from 'react';
import { useStationContext } from '../context/StationContext';
import { cargoApi, inventoryApi } from '../api/api';
import { CargoConsignment, InventoryItem, ReceiveCargoResponse } from '../types';
import { KpiCard } from '../components/common/KpiCard';
import { LoadingState, ErrorState, EmptyState } from '../components/common/FeedbackStates';
import { CreateCargoModal } from '../components/cargo/CreateCargoModal';
import {
  Ship,
  Search,
  Filter,
  RefreshCw,
  PlusCircle,
  PackageCheck,
  CheckCircle2,
  Clock,
  ArrowRight,
  Boxes,
  AlertTriangle,
} from 'lucide-react';

export const CargoPage: React.FC = () => {
  const { selectedStation } = useStationContext();
  const [consignments, setConsignments] = useState<CargoConsignment[]>([]);
  const [items, setItems] = useState<InventoryItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Filters & Search
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  // Detail & Modals
  const [selectedConsignment, setSelectedConsignment] = useState<CargoConsignment | null>(null);
  const [createModalOpen, setCreateModalOpen] = useState<boolean>(false);
  const [receiptSuccess, setReceiptSuccess] = useState<ReceiveCargoResponse | null>(null);
  const [receivingId, setReceivingId] = useState<string | null>(null);
  const [confirmReceiveId, setConfirmReceiveId] = useState<string | null>(null);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [cargoData, itemData] = await Promise.all([
        cargoApi.getConsignments(selectedStation.id),
        inventoryApi.getAllItems().catch(() => []),
      ]);
      setConsignments(cargoData);
      setItems(itemData);
      if (selectedConsignment) {
        const updated = cargoData.find((c) => c.id === selectedConsignment.id);
        if (updated) setSelectedConsignment(updated);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to fetch cargo consignments');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [selectedStation.id]);

  const handleReceiveCargo = async (consignment: CargoConsignment) => {
    setReceivingId(consignment.id);
    setError(null);
    try {
      const res = await cargoApi.receiveConsignment(consignment.id, 'EXPEDITION-COMMANDER');
      setReceiptSuccess(res);
      setConfirmReceiveId(null);
      await fetchData();
    } catch (err: any) {
      setError(err.message || 'Failed to receive cargo consignment');
    } finally {
      setReceivingId(null);
    }
  };

  // KPIs
  const arrivedCount = consignments.filter((c) => c.status === 'ARRIVED').length;
  const inTransitCount = consignments.filter((c) => c.status === 'IN_TRANSIT').length;
  const plannedCount = consignments.filter((c) => c.status === 'PLANNED').length;
  const receivedCount = consignments.filter((c) => c.status === 'RECEIVED').length;

  const filteredConsignments = useMemo(() => {
    return consignments.filter((c) => {
      const matchesSearch =
        c.trackingNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (c.destinationStationName && c.destinationStationName.toLowerCase().includes(searchQuery.toLowerCase()));
      const matchesStatus = statusFilter === 'ALL' || c.status === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [consignments, searchQuery, statusFilter]);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'ARRIVED':
        return 'bg-amber-950/80 text-amber-400 border-amber-800/60';
      case 'RECEIVED':
        return 'bg-emerald-950/80 text-emerald-400 border-emerald-800/60';
      case 'IN_TRANSIT':
        return 'bg-blue-950/80 text-blue-400 border-blue-800/60';
      case 'PLANNED':
      default:
        return 'bg-slate-800/80 text-slate-300 border-slate-700/60';
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-900/60 p-4 rounded-lg border border-slate-800 backdrop-blur-sm">
        <div>
          <h1 className="text-lg font-bold text-slate-100 uppercase tracking-wide">
            {selectedStation.name} — Cargo Management & Resupply
          </h1>
          <p className="text-xs text-slate-400 mt-0.5">
            Vessel Tracking, Manifest Verification & Atomic Station Receipt
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => setCreateModalOpen(true)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold tracking-wide transition-colors"
          >
            <PlusCircle className="h-3.5 w-3.5" />
            <span>New Consignment</span>
          </button>
          <button
            onClick={fetchData}
            className="p-2 rounded-md bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors border border-slate-700/60"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin text-blue-400' : ''}`} />
          </button>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <KpiCard
          title="Arrived at Station"
          value={arrivedCount}
          subtitle="Awaiting receipt into stock"
          icon={PackageCheck}
          variant={arrivedCount > 0 ? 'warning' : 'default'}
        />
        <KpiCard
          title="In Transit Vessels"
          value={inTransitCount}
          subtitle="En route via polar ocean"
          icon={Ship}
          variant="info"
        />
        <KpiCard
          title="Planned Consignments"
          value={plannedCount}
          subtitle="Scheduled resupply runs"
          icon={Clock}
          variant="default"
        />
        <KpiCard
          title="Received Converted"
          value={receivedCount}
          subtitle="Atomically merged into stock"
          icon={CheckCircle2}
          variant="healthy"
        />
      </div>

      {/* Receipt Success Banner */}
      {receiptSuccess && (
        <div className="rounded-lg border border-emerald-800/70 bg-emerald-950/30 p-4 text-xs text-emerald-300 flex items-start justify-between">
          <div className="flex items-start gap-3">
            <CheckCircle2 className="h-5 w-5 text-emerald-400 shrink-0 mt-0.5" />
            <div>
              <div className="font-bold text-sm text-emerald-200">
                Consignment {receiptSuccess.trackingNumber} Atomically Received!
              </div>
              <div className="mt-1 text-slate-300">
                Converted {receiptSuccess.receivedItemCount} manifest items (Total:{' '}
                {receiptSuccess.totalReceivedQuantity.toLocaleString()} units) into station inventory
                ledger with source <span className="font-mono text-emerald-400">CARGO_RECEIPT</span>.
              </div>
              <div className="mt-1 text-[11px] text-slate-400">
                Station stock levels and intelligence projections have been automatically updated.
              </div>
            </div>
          </div>
          <button
            onClick={() => setReceiptSuccess(null)}
            className="text-slate-400 hover:text-slate-200 text-xs ml-4"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Search and Filters */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div className="relative sm:col-span-2">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search tracking number or station..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-slate-900 border border-slate-800 rounded-md pl-9 pr-4 py-2 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-blue-500"
          />
        </div>

        <div>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="w-full bg-slate-900 border border-slate-800 rounded-md px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500 font-medium"
          >
            <option value="ALL">All Statuses</option>
            <option value="ARRIVED">Arrived (Ready to Receive)</option>
            <option value="IN_TRANSIT">In Transit</option>
            <option value="PLANNED">Planned</option>
            <option value="RECEIVED">Received</option>
          </select>
        </div>
      </div>

      {/* Main View: Table + Detail Manifest Drawer */}
      {loading && consignments.length === 0 ? (
        <LoadingState message="Fetching cargo manifests and consignments..." />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchData} />
      ) : filteredConsignments.length === 0 ? (
        <EmptyState
          title="No cargo consignments found"
          message="No consignments match the active filters or station destination."
        />
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* 2 Cols: Consignment List */}
          <div className="lg:col-span-2 rounded-lg border border-slate-800/80 bg-slate-900/50 backdrop-blur-sm overflow-hidden flex flex-col">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-300">
                <thead className="bg-slate-950/80 text-[11px] uppercase tracking-wider text-slate-400 border-b border-slate-800">
                  <tr>
                    <th className="px-4 py-3">Tracking #</th>
                    <th className="px-3 py-3">Destination</th>
                    <th className="px-3 py-3 text-center">Status</th>
                    <th className="px-3 py-3">Est. Arrival</th>
                    <th className="px-3 py-3 text-center">Items</th>
                    <th className="px-4 py-3 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {filteredConsignments.map((c) => {
                    const isSelected = selectedConsignment?.id === c.id;
                    const isArrived = c.status === 'ARRIVED';
                    return (
                      <tr
                        key={c.id}
                        onClick={() => setSelectedConsignment(c)}
                        className={`hover:bg-slate-800/50 cursor-pointer transition-colors ${
                          isSelected ? 'bg-blue-950/30 border-l-2 border-blue-500' : ''
                        }`}
                      >
                        <td className="px-4 py-3 font-mono font-medium text-slate-100">
                          {c.trackingNumber}
                        </td>
                        <td className="px-3 py-3 text-slate-300">{c.destinationStationName}</td>
                        <td className="px-3 py-3 text-center">
                          <span
                            className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] border tracking-wider font-semibold uppercase ${getStatusBadge(
                              c.status
                            )}`}
                          >
                            {c.status}
                          </span>
                        </td>
                        <td className="px-3 py-3 text-slate-400 font-mono text-[11px]">
                          {c.estimatedArrival}
                        </td>
                        <td className="px-3 py-3 text-center font-mono">{c.items ? c.items.length : 0}</td>
                        <td className="px-4 py-3 text-right">
                          {isArrived ? (
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                setConfirmReceiveId(c.id);
                              }}
                              disabled={receivingId === c.id}
                              className="px-2.5 py-1 rounded bg-amber-500/20 hover:bg-amber-500/30 text-amber-400 border border-amber-500/40 text-[11px] font-semibold transition-colors disabled:opacity-50"
                            >
                              {receivingId === c.id ? 'Receiving...' : 'Receive Cargo'}
                            </button>
                          ) : (
                            <span className="text-[11px] text-slate-500 italic">
                              {c.status === 'RECEIVED' ? 'Processed' : 'En Route'}
                            </span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* 1 Col: Manifest Details Drawer */}
          <div className="rounded-lg border border-slate-800/80 bg-slate-900/50 p-4 backdrop-blur-sm h-fit space-y-4">
            {selectedConsignment ? (
              <>
                <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                  <div>
                    <h3 className="text-sm font-bold text-slate-100 font-mono">
                      {selectedConsignment.trackingNumber}
                    </h3>
                    <div className="text-xs text-slate-400 mt-0.5">
                      Destination: {selectedConsignment.destinationStationName}
                    </div>
                  </div>
                  <span
                    className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] border tracking-wider font-semibold uppercase ${getStatusBadge(
                      selectedConsignment.status
                    )}`}
                  >
                    {selectedConsignment.status}
                  </span>
                </div>

                {/* Timeline */}
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="p-2.5 rounded bg-slate-950/60 border border-slate-800">
                    <span className="text-[10px] uppercase font-semibold text-slate-500">
                      Departure Date
                    </span>
                    <div className="font-mono text-slate-200 mt-0.5">
                      {selectedConsignment.departureDate || '—'}
                    </div>
                  </div>
                  <div className="p-2.5 rounded bg-slate-950/60 border border-slate-800">
                    <span className="text-[10px] uppercase font-semibold text-slate-500">
                      Estimated Arrival
                    </span>
                    <div className="font-mono text-slate-200 mt-0.5">
                      {selectedConsignment.estimatedArrival}
                    </div>
                  </div>
                </div>

                {/* Manifest Items List */}
                <div className="space-y-2">
                  <span className="text-xs font-semibold uppercase tracking-wider text-slate-300">
                    Manifest Items ({selectedConsignment.items ? selectedConsignment.items.length : 0})
                  </span>

                  {!selectedConsignment.items || selectedConsignment.items.length === 0 ? (
                    <div className="text-xs text-slate-500 italic p-3 bg-slate-950/40 rounded border border-slate-800">
                      No items attached to this manifest.
                    </div>
                  ) : (
                    <div className="divide-y divide-slate-800/80 rounded border border-slate-800 overflow-hidden">
                      {selectedConsignment.items.map((item) => (
                        <div
                          key={item.id}
                          className="p-2.5 bg-slate-950/60 flex items-center justify-between text-xs"
                        >
                          <div>
                            <div className="font-medium text-slate-100">{item.itemName}</div>
                            <div className="text-[10px] text-slate-500 font-mono">{item.itemCode}</div>
                          </div>
                          <div className="font-mono font-bold text-slate-200">
                            {item.quantity.toLocaleString()} {item.unit}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* Receive Action for ARRIVED cargo */}
                {selectedConsignment.status === 'ARRIVED' && (
                  <div className="pt-2">
                    <button
                      onClick={() => setConfirmReceiveId(selectedConsignment.id)}
                      disabled={receivingId === selectedConsignment.id}
                      className="w-full py-2 rounded-md bg-amber-600 hover:bg-amber-500 text-white text-xs font-semibold tracking-wide transition-colors flex items-center justify-center gap-2 disabled:opacity-50"
                    >
                      <PackageCheck className="h-4 w-4" />
                      <span>Receive into Station Inventory</span>
                    </button>
                  </div>
                )}
              </>
            ) : (
              <div className="py-12 text-center text-xs text-slate-500">
                <Boxes className="mx-auto h-8 w-8 text-slate-600 mb-2" />
                Select a cargo consignment to inspect manifest items and execute atomic station receipts.
              </div>
            )}
          </div>
        </div>
      )}

      {/* Receive Confirmation Dialog */}
      {confirmReceiveId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-xs p-4">
          <div className="w-full max-w-md rounded-lg border border-slate-700 bg-slate-900 shadow-2xl p-6 space-y-4">
            <div className="flex items-center gap-3 text-amber-400">
              <AlertTriangle className="h-6 w-6" />
              <h3 className="text-sm font-bold uppercase tracking-wider text-slate-100">
                Confirm Station Cargo Receipt
              </h3>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              Receiving this consignment will atomically transition its status from{' '}
              <span className="font-semibold text-amber-400">ARRIVED</span> to{' '}
              <span className="font-semibold text-emerald-400">RECEIVED</span>, create official immutable{' '}
              <span className="font-mono text-slate-100">RECEIPT</span> transaction ledger records, and
              immediately increase station stock levels.
            </p>
            <div className="flex justify-end gap-3 pt-2">
              <button
                onClick={() => setConfirmReceiveId(null)}
                className="px-4 py-2 rounded text-xs font-medium text-slate-400 hover:bg-slate-800 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  const target = consignments.find((c) => c.id === confirmReceiveId);
                  if (target) handleReceiveCargo(target);
                }}
                disabled={receivingId !== null}
                className="px-4 py-2 rounded text-xs font-semibold bg-emerald-600 hover:bg-emerald-500 text-white transition-colors"
              >
                {receivingId ? 'Processing Receipt...' : 'Confirm Receipt'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Create Consignment Modal */}
      <CreateCargoModal
        isOpen={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        stationId={selectedStation.id}
        stationName={selectedStation.name}
        availableItems={items}
        onSuccess={fetchData}
      />
    </div>
  );
};
