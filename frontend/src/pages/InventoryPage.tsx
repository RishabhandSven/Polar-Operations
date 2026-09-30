import React, { useEffect, useState, useMemo } from 'react';
import { useStationContext } from '../context/StationContext';
import { inventoryApi, intelligenceApi } from '../api/api';
import { StockLevel, StockRiskAssessment, RiskLevel } from '../types';
import { RiskBadge } from '../components/common/RiskBadge';
import { TransactionModal } from '../components/inventory/TransactionModal';
import { LoadingState, ErrorState, EmptyState } from '../components/common/FeedbackStates';
import {
  Search,
  Filter,
  PlusCircle,
  RefreshCw,
  ArrowUpDown,
  FileText,
  Boxes,
  Info,
} from 'lucide-react';

export const InventoryPage: React.FC = () => {
  const { selectedStation } = useStationContext();
  const [stockLevels, setStockLevels] = useState<StockLevel[]>([]);
  const [assessments, setAssessments] = useState<StockRiskAssessment[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Search, Filter, Sort
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [selectedRisk, setSelectedRisk] = useState<string>('ALL');
  const [sortField, setSortField] = useState<'currentStock' | 'dailyConsumption' | 'itemName'>('itemName');
  const [sortAsc, setSortAsc] = useState<boolean>(true);

  // Detail panel & Modal
  const [selectedStockLevel, setSelectedStockLevel] = useState<StockLevel | null>(null);
  const [transactionModalOpen, setTransactionModalOpen] = useState<boolean>(false);
  const [modalTargetStock, setModalTargetStock] = useState<StockLevel | null>(null);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [stocks, intel] = await Promise.all([
        inventoryApi.getStockLevels(selectedStation.id),
        intelligenceApi.getReport(selectedStation.id, 0),
      ]);
      setStockLevels(stocks);
      setAssessments(intel.assessments);
    } catch (err: any) {
      setError(err.message || 'Failed to fetch inventory data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [selectedStation.id]);

  // Map risk assessments by inventoryItemId
  const riskMap = useMemo(() => {
    const map = new Map<string, StockRiskAssessment>();
    assessments.forEach((a) => map.set(a.inventoryItemId, a));
    return map;
  }, [assessments]);

  // Unique categories
  const categories = useMemo(() => {
    const set = new Set<string>();
    stockLevels.forEach((sl) => set.add(sl.category));
    return Array.from(set);
  }, [stockLevels]);

  // Filtered & Sorted items
  const filteredItems = useMemo(() => {
    return stockLevels
      .filter((item) => {
        const matchesSearch =
          item.itemName.toLowerCase().includes(searchQuery.toLowerCase()) ||
          item.itemCode.toLowerCase().includes(searchQuery.toLowerCase());
        const matchesCategory = selectedCategory === 'ALL' || item.category === selectedCategory;
        const itemRisk = riskMap.get(item.inventoryItemId)?.riskLevel || 'HEALTHY';
        const matchesRisk = selectedRisk === 'ALL' || itemRisk === selectedRisk;

        return matchesSearch && matchesCategory && matchesRisk;
      })
      .sort((a, b) => {
        let valA = a[sortField];
        let valB = b[sortField];
        if (typeof valA === 'string') {
          return sortAsc ? valA.localeCompare(valB as string) : (valB as string).localeCompare(valA);
        }
        return sortAsc ? (valA as number) - (valB as number) : (valB as number) - (valA as number);
      });
  }, [stockLevels, searchQuery, selectedCategory, selectedRisk, sortField, sortAsc, riskMap]);

  return (
    <div className="space-y-6">
      {/* Header & Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-900/60 p-4 rounded-lg border border-slate-800 backdrop-blur-sm">
        <div>
          <h1 className="text-lg font-bold text-slate-100 uppercase tracking-wide">
            {selectedStation.name} — Station Stock Catalogue
          </h1>
          <p className="text-xs text-slate-400 mt-0.5">
            Real-time Station Inventory Master & Mutation Ledger
          </p>
        </div>

        <button
          onClick={fetchData}
          className="flex items-center gap-2 px-3 py-1.5 rounded-md bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium transition-colors border border-slate-700/60 self-start sm:self-auto"
        >
          <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin text-blue-400' : ''}`} />
          <span>Refresh Stock</span>
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="grid grid-cols-1 sm:grid-cols-3 lg:grid-cols-4 gap-3">
        {/* Search */}
        <div className="relative sm:col-span-2">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search by item name or code..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-slate-900 border border-slate-800 rounded-md pl-9 pr-4 py-2 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-blue-500"
          />
        </div>

        {/* Category Filter */}
        <div>
          <select
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
            className="w-full bg-slate-900 border border-slate-800 rounded-md px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500 font-medium"
          >
            <option value="ALL">All Categories</option>
            {categories.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </div>

        {/* Risk Filter */}
        <div>
          <select
            value={selectedRisk}
            onChange={(e) => setSelectedRisk(e.target.value)}
            className="w-full bg-slate-900 border border-slate-800 rounded-md px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500 font-medium"
          >
            <option value="ALL">All Risk Statuses</option>
            <option value="CRITICAL">Critical Only</option>
            <option value="WARNING">Warning Only</option>
            <option value="HEALTHY">Healthy Only</option>
          </select>
        </div>
      </div>

      {/* Main Content: Table and Detail Drawer */}
      {loading && stockLevels.length === 0 ? (
        <LoadingState message="Loading station inventory records..." />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchData} />
      ) : filteredItems.length === 0 ? (
        <EmptyState
          title="No stock items found"
          message="No inventory records matched your selected criteria or search term."
        />
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Table Area (2 cols on large screens) */}
          <div className="lg:col-span-2 rounded-lg border border-slate-800/80 bg-slate-900/50 backdrop-blur-sm overflow-hidden flex flex-col">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-300">
                <thead className="bg-slate-950/80 text-[11px] uppercase tracking-wider text-slate-400 border-b border-slate-800">
                  <tr>
                    <th className="px-4 py-3 cursor-pointer" onClick={() => { setSortField('itemName'); setSortAsc(!sortAsc); }}>
                      <div className="flex items-center gap-1">
                        <span>Item</span>
                        <ArrowUpDown className="h-3 w-3" />
                      </div>
                    </th>
                    <th className="px-3 py-3">Category</th>
                    <th className="px-3 py-3 cursor-pointer text-right" onClick={() => { setSortField('currentStock'); setSortAsc(!sortAsc); }}>
                      <div className="flex items-center justify-end gap-1">
                        <span>Current Stock</span>
                        <ArrowUpDown className="h-3 w-3" />
                      </div>
                    </th>
                    <th className="px-3 py-3 text-right">Daily Burn</th>
                    <th className="px-3 py-3 text-center">Status</th>
                    <th className="px-4 py-3 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {filteredItems.map((item) => {
                    const risk = riskMap.get(item.inventoryItemId)?.riskLevel || 'HEALTHY';
                    const isSelected = selectedStockLevel?.id === item.id;
                    return (
                      <tr
                        key={item.id}
                        onClick={() => setSelectedStockLevel(item)}
                        className={`hover:bg-slate-800/50 cursor-pointer transition-colors ${
                          isSelected ? 'bg-blue-950/30 border-l-2 border-blue-500' : ''
                        }`}
                      >
                        <td className="px-4 py-3 font-medium text-slate-100">
                          <div>{item.itemName}</div>
                          <div className="text-[10px] text-slate-500 font-mono">{item.itemCode}</div>
                        </td>
                        <td className="px-3 py-3 text-slate-400">{item.category}</td>
                        <td className="px-3 py-3 text-right font-mono font-semibold text-slate-200">
                          {item.currentStock.toLocaleString()} <span className="text-[10px] text-slate-500">{item.unit}</span>
                        </td>
                        <td className="px-3 py-3 text-right font-mono text-slate-400">
                          {item.dailyConsumption > 0 ? `${item.dailyConsumption.toLocaleString()} ${item.unit}` : '—'}
                        </td>
                        <td className="px-3 py-3 text-center">
                          <RiskBadge risk={risk} />
                        </td>
                        <td className="px-4 py-3 text-right">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setModalTargetStock(item);
                              setTransactionModalOpen(true);
                            }}
                            className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-blue-600/20 hover:bg-blue-600/30 text-blue-400 border border-blue-500/40 font-medium text-[11px] transition-colors"
                          >
                            <PlusCircle className="h-3 w-3" />
                            <span>Log</span>
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* Item Detail Panel (1 col) */}
          <div className="rounded-lg border border-slate-800/80 bg-slate-900/50 p-4 backdrop-blur-sm h-fit space-y-4">
            {selectedStockLevel ? (
              <>
                <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                  <div>
                    <h3 className="text-sm font-bold text-slate-100">{selectedStockLevel.itemName}</h3>
                    <div className="text-xs font-mono text-slate-400">{selectedStockLevel.itemCode}</div>
                  </div>
                  {riskMap.get(selectedStockLevel.inventoryItemId) && (
                    <RiskBadge risk={riskMap.get(selectedStockLevel.inventoryItemId)!.riskLevel} />
                  )}
                </div>

                <div className="space-y-3 text-xs">
                  <div className="flex justify-between py-1 border-b border-slate-800/50">
                    <span className="text-slate-400">Category:</span>
                    <span className="text-slate-200 font-medium">{selectedStockLevel.category}</span>
                  </div>
                  <div className="flex justify-between py-1 border-b border-slate-800/50">
                    <span className="text-slate-400">Current Stock:</span>
                    <span className="font-mono font-bold text-slate-100">
                      {selectedStockLevel.currentStock.toLocaleString()} {selectedStockLevel.unit}
                    </span>
                  </div>
                  <div className="flex justify-between py-1 border-b border-slate-800/50">
                    <span className="text-slate-400">Daily Consumption:</span>
                    <span className="font-mono text-slate-200">
                      {selectedStockLevel.dailyConsumption.toLocaleString()} {selectedStockLevel.unit}/day
                    </span>
                  </div>
                  <div className="flex justify-between py-1 border-b border-slate-800/50">
                    <span className="text-slate-400">Safety Threshold:</span>
                    <span className="font-mono text-amber-400">
                      {selectedStockLevel.safetyStock.toLocaleString()} {selectedStockLevel.unit}
                    </span>
                  </div>
                  <div className="flex justify-between py-1 border-b border-slate-800/50">
                    <span className="text-slate-400">Reorder Point:</span>
                    <span className="font-mono text-slate-200">
                      {selectedStockLevel.reorderPoint.toLocaleString()} {selectedStockLevel.unit}
                    </span>
                  </div>
                  <div className="flex justify-between py-1 border-b border-slate-800/50">
                    <span className="text-slate-400">Lead Time:</span>
                    <span className="text-slate-200">{selectedStockLevel.leadTimeDays} days</span>
                  </div>
                  <div className="flex justify-between py-1 border-b border-slate-800/50">
                    <span className="text-slate-400">Next Resupply Date:</span>
                    <span className="text-slate-200 font-mono">{selectedStockLevel.nextResupplyDate}</span>
                  </div>
                </div>

                {/* Intelligence Explanation */}
                {riskMap.get(selectedStockLevel.inventoryItemId) && (
                  <div className="p-3 rounded bg-slate-950/80 border border-slate-800 text-xs space-y-1.5">
                    <div className="flex items-center gap-1.5 text-blue-400 font-semibold text-[11px] uppercase tracking-wider">
                      <Info className="h-3.5 w-3.5" />
                      <span>Intelligence Diagnosis</span>
                    </div>
                    <p className="text-slate-300 leading-relaxed">
                      {riskMap.get(selectedStockLevel.inventoryItemId)!.explanation}
                    </p>
                  </div>
                )}

                <button
                  onClick={() => {
                    setModalTargetStock(selectedStockLevel);
                    setTransactionModalOpen(true);
                  }}
                  className="w-full py-2 rounded-md bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold tracking-wide transition-colors flex items-center justify-center gap-2"
                >
                  <PlusCircle className="h-4 w-4" />
                  <span>Log Stock Transaction</span>
                </button>
              </>
            ) : (
              <div className="py-12 text-center text-xs text-slate-500">
                <Boxes className="mx-auto h-8 w-8 text-slate-600 mb-2" />
                Select an item in the catalogue table to inspect stock parameters and intelligence diagnosis.
              </div>
            )}
          </div>
        </div>
      )}

      {/* Transaction Modal */}
      {modalTargetStock && (
        <TransactionModal
          isOpen={transactionModalOpen}
          onClose={() => setTransactionModalOpen(false)}
          stockLevel={modalTargetStock}
          onSuccess={fetchData}
        />
      )}
    </div>
  );
};
