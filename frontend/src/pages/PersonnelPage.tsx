import React, { useEffect, useState } from 'react';
import { useStationContext } from '../context/StationContext';
import { personnelApi } from '../api/api';
import { Personnel } from '../types';
import { KpiCard } from '../components/common/KpiCard';
import { LoadingState, ErrorState, EmptyState } from '../components/common/FeedbackStates';
import {
  Users,
  UserCheck,
  Compass,
  PlusCircle,
  RefreshCw,
  Search,
  ArrowRight,
  Shield,
  X,
  AlertCircle,
} from 'lucide-react';

export const PersonnelPage: React.FC = () => {
  const { selectedStation } = useStationContext();
  const [personnel, setPersonnel] = useState<Personnel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedPerson, setSelectedPerson] = useState<Personnel | null>(null);
  const [movements, setMovements] = useState<any[]>([]);
  const [movementsLoading, setMovementsLoading] = useState(false);

  // New Personnel Modal
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [newName, setNewName] = useState('');
  const [newRole, setNewRole] = useState('SCIENTIST');

  // Movement Modal
  const [isMoveModalOpen, setIsMoveModalOpen] = useState(false);
  const [moveType, setMoveType] = useState('SORTIE_DEPARTURE');
  const [moveDestination, setMoveDestination] = useState('');
  const [moveNotes, setMoveNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await personnelApi.list(selectedStation.id);
      setPersonnel(data);
      if (selectedPerson) {
        const updated = data.find((p) => p.id === selectedPerson.id);
        if (updated) setSelectedPerson(updated);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to fetch personnel roster');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [selectedStation.id]);

  const loadMovements = async (person: Personnel) => {
    setSelectedPerson(person);
    setMovementsLoading(true);
    try {
      const m = await personnelApi.listMovements(person.id);
      setMovements(m);
    } catch (err) {
      setMovements([]);
    } finally {
      setMovementsLoading(false);
    }
  };

  const handleAddPersonnel = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName.trim()) return;
    setSubmitting(true);
    try {
      await personnelApi.create({
        stationId: selectedStation.id,
        name: newName.trim(),
        role: newRole,
        status: 'ON_STATION',
      });
      setIsAddModalOpen(false);
      setNewName('');
      await fetchData();
    } catch (err: any) {
      setError(err.message || 'Failed to enroll personnel');
    } finally {
      setSubmitting(false);
    }
  };

  const handleRecordMovement = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedPerson) return;
    setSubmitting(true);
    try {
      await personnelApi.recordMovement(selectedPerson.id, {
        type: moveType,
        destination: moveDestination.trim() || undefined,
        notes: moveNotes.trim() || undefined,
      });
      setIsMoveModalOpen(false);
      setMoveDestination('');
      setMoveNotes('');
      await fetchData();
      await loadMovements(selectedPerson);
    } catch (err: any) {
      setError(err.message || 'Failed to log movement');
    } finally {
      setSubmitting(false);
    }
  };

  const onStationCount = personnel.filter((p) => p.status === 'ON_STATION').length;
  const sortieCount = personnel.filter((p) => p.status === 'FIELD_SORTIE').length;

  const filtered = personnel.filter(
    (p) =>
      p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.role.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-900/60 p-4 rounded-lg border border-slate-800 backdrop-blur-sm">
        <div>
          <h1 className="text-lg font-bold text-slate-100 uppercase tracking-wide">
            {selectedStation.name} — Expedition Personnel & Sorties
          </h1>
          <p className="text-xs text-slate-400 mt-0.5">
            Station Crew Complement, Field Movements & Medical Readiness
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => setIsAddModalOpen(true)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold tracking-wide transition-colors"
          >
            <PlusCircle className="h-3.5 w-3.5" />
            <span>Enroll Crew</span>
          </button>
          <button
            onClick={fetchData}
            className="p-2 rounded-md bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors border border-slate-700/60"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin text-blue-400' : ''}`} />
          </button>
        </div>
      </div>

      {/* KPI Row */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <KpiCard
          title="Station Complement"
          value={personnel.length}
          subtitle="Total enrolled crew"
          icon={Users}
          variant="info"
        />
        <KpiCard
          title="On Station"
          value={onStationCount}
          subtitle="Currently inside habitat"
          icon={UserCheck}
          variant="healthy"
        />
        <KpiCard
          title="Active Field Sorties"
          value={sortieCount}
          subtitle="Traverse & scientific field teams"
          icon={Compass}
          variant={sortieCount > 0 ? 'warning' : 'default'}
        />
      </div>

      {/* Search */}
      <div className="relative">
        <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
        <input
          type="text"
          placeholder="Search personnel by name or role..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full bg-slate-900 border border-slate-800 rounded-md pl-9 pr-4 py-2 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-blue-500"
        />
      </div>

      {loading && personnel.length === 0 ? (
        <LoadingState message="Loading station personnel manifest..." />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchData} />
      ) : filtered.length === 0 ? (
        <EmptyState
          title="No personnel found"
          message="No active crew records enrolled for this station. Click 'Enroll Crew' to register members."
        />
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Personnel Table */}
          <div className="lg:col-span-2 rounded-lg border border-slate-800/80 bg-slate-900/50 backdrop-blur-sm overflow-hidden flex flex-col">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-300">
                <thead className="bg-slate-950/80 text-[11px] uppercase tracking-wider text-slate-400 border-b border-slate-800">
                  <tr>
                    <th className="px-4 py-3">Crew Member</th>
                    <th className="px-3 py-3">Role</th>
                    <th className="px-3 py-3 text-center">Status</th>
                    <th className="px-4 py-3 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {filtered.map((person) => {
                    const isSelected = selectedPerson?.id === person.id;
                    return (
                      <tr
                        key={person.id}
                        onClick={() => loadMovements(person)}
                        className={`hover:bg-slate-800/50 cursor-pointer transition-colors ${
                          isSelected ? 'bg-blue-950/30 border-l-2 border-blue-500' : ''
                        }`}
                      >
                        <td className="px-4 py-3 font-semibold text-slate-100 flex items-center gap-2">
                          <Shield className="h-3.5 w-3.5 text-blue-400" />
                          <span>{person.name}</span>
                        </td>
                        <td className="px-3 py-3 text-slate-300 font-mono text-[11px]">
                          {person.role}
                        </td>
                        <td className="px-3 py-3 text-center">
                          <span
                            className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] font-semibold border ${
                              person.status === 'ON_STATION'
                                ? 'bg-emerald-950/60 text-emerald-400 border-emerald-800/60'
                                : 'bg-amber-950/60 text-amber-400 border-amber-800/60'
                            }`}
                          >
                            {person.status}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-right">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setSelectedPerson(person);
                              setIsMoveModalOpen(true);
                            }}
                            className="px-2.5 py-1 rounded bg-blue-600/20 hover:bg-blue-600/30 text-blue-400 border border-blue-500/40 text-[11px] font-medium transition-colors"
                          >
                            Log Movement
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* Movement Logs Drawer */}
          <div className="rounded-lg border border-slate-800/80 bg-slate-900/50 p-4 backdrop-blur-sm h-fit space-y-4">
            {selectedPerson ? (
              <>
                <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                  <div>
                    <h3 className="text-sm font-bold text-slate-100">{selectedPerson.name}</h3>
                    <div className="text-xs text-slate-400 font-mono mt-0.5">{selectedPerson.role}</div>
                  </div>
                  <span
                    className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] font-semibold border ${
                      selectedPerson.status === 'ON_STATION'
                        ? 'bg-emerald-950/60 text-emerald-400 border-emerald-800/60'
                        : 'bg-amber-950/60 text-amber-400 border-amber-800/60'
                    }`}
                  >
                    {selectedPerson.status}
                  </span>
                </div>

                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-300">
                      Sortie & Movement History
                    </span>
                    <button
                      onClick={() => setIsMoveModalOpen(true)}
                      className="text-[11px] text-blue-400 hover:text-blue-300 font-medium"
                    >
                      + Log
                    </button>
                  </div>

                  {movementsLoading ? (
                    <div className="py-6 text-center text-xs text-slate-500">Loading history...</div>
                  ) : movements.length === 0 ? (
                    <div className="p-3 rounded bg-slate-950/40 border border-slate-800 text-xs text-slate-500 italic">
                      No logged sorties or movements for this crew member.
                    </div>
                  ) : (
                    <div className="divide-y divide-slate-800/80 rounded border border-slate-800 overflow-hidden">
                      {movements.map((m) => (
                        <div key={m.id} className="p-2.5 bg-slate-950/60 text-xs space-y-1">
                          <div className="flex items-center justify-between font-semibold">
                            <span className="text-slate-200">{m.type}</span>
                            <span className="text-[10px] text-slate-500 font-mono">
                              {new Date(m.timestamp).toLocaleDateString()}
                            </span>
                          </div>
                          {m.destination && (
                            <div className="text-[11px] text-blue-400">Target: {m.destination}</div>
                          )}
                          {m.notes && <div className="text-[11px] text-slate-400">{m.notes}</div>}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </>
            ) : (
              <div className="py-12 text-center text-xs text-slate-500">
                <Users className="mx-auto h-8 w-8 text-slate-600 mb-2" />
                Select a crew member to inspect movement history and log sorties.
              </div>
            )}
          </div>
        </div>
      )}

      {/* Add Crew Modal */}
      {isAddModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-xs p-4">
          <div className="w-full max-w-md rounded-lg border border-slate-700 bg-slate-900 shadow-2xl p-6">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h3 className="text-sm font-bold uppercase tracking-wider text-slate-100">
                Enroll Crew Member
              </h3>
              <button onClick={() => setIsAddModalOpen(false)} className="text-slate-400 hover:text-slate-200">
                <X className="h-4 w-4" />
              </button>
            </div>
            <form onSubmit={handleAddPersonnel} className="space-y-4 mt-4 text-xs">
              <div>
                <label className="block text-slate-400 mb-1 font-medium uppercase tracking-wider">
                  Full Name
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Dr. Rajesh Sharma"
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded px-3 py-2 text-slate-200 focus:outline-none focus:border-blue-500"
                />
              </div>
              <div>
                <label className="block text-slate-400 mb-1 font-medium uppercase tracking-wider">
                  Role
                </label>
                <select
                  value={newRole}
                  onChange={(e) => setNewRole(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded px-3 py-2 text-slate-200 focus:outline-none focus:border-blue-500"
                >
                  <option value="COMMANDER">COMMANDER</option>
                  <option value="SCIENTIST">SCIENTIST</option>
                  <option value="ENGINEER">ENGINEER</option>
                  <option value="MEDICAL_OFFICER">MEDICAL OFFICER</option>
                  <option value="LOGISTICS_SPECIALIST">LOGISTICS SPECIALIST</option>
                </select>
              </div>
              <div className="pt-2 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setIsAddModalOpen(false)}
                  className="px-3 py-1.5 rounded text-slate-400 hover:bg-slate-800"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-3 py-1.5 rounded bg-blue-600 hover:bg-blue-500 text-white font-medium disabled:opacity-50"
                >
                  {submitting ? 'Enrolling...' : 'Enroll Member'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Movement Modal */}
      {isMoveModalOpen && selectedPerson && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-xs p-4">
          <div className="w-full max-w-md rounded-lg border border-slate-700 bg-slate-900 shadow-2xl p-6">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h3 className="text-sm font-bold uppercase tracking-wider text-slate-100">
                Log Movement / Sortie for {selectedPerson.name}
              </h3>
              <button onClick={() => setIsMoveModalOpen(false)} className="text-slate-400 hover:text-slate-200">
                <X className="h-4 w-4" />
              </button>
            </div>
            <form onSubmit={handleRecordMovement} className="space-y-4 mt-4 text-xs">
              <div>
                <label className="block text-slate-400 mb-1 font-medium uppercase tracking-wider">
                  Movement Type
                </label>
                <select
                  value={moveType}
                  onChange={(e) => setMoveType(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded px-3 py-2 text-slate-200 focus:outline-none focus:border-blue-500"
                >
                  <option value="SORTIE_DEPARTURE">SORTIE DEPARTURE (Departs Station)</option>
                  <option value="SORTIE_RETURN">SORTIE RETURN (Returns to Station)</option>
                  <option value="CHECK_IN">CHECK IN</option>
                  <option value="CHECK_OUT">CHECK OUT</option>
                </select>
              </div>
              <div>
                <label className="block text-slate-400 mb-1 font-medium uppercase tracking-wider">
                  Destination / Waypoint
                </label>
                <input
                  type="text"
                  placeholder="e.g. Glacier Core Sample Point Bravo"
                  value={moveDestination}
                  onChange={(e) => setMoveDestination(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded px-3 py-2 text-slate-200 focus:outline-none focus:border-blue-500"
                />
              </div>
              <div>
                <label className="block text-slate-400 mb-1 font-medium uppercase tracking-wider">
                  Operational Notes
                </label>
                <textarea
                  rows={2}
                  placeholder="Weather conditions, radio check frequency..."
                  value={moveNotes}
                  onChange={(e) => setMoveNotes(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded px-3 py-2 text-slate-200 focus:outline-none focus:border-blue-500"
                />
              </div>
              <div className="pt-2 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setIsMoveModalOpen(false)}
                  className="px-3 py-1.5 rounded text-slate-400 hover:bg-slate-800"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-3 py-1.5 rounded bg-blue-600 hover:bg-blue-500 text-white font-medium disabled:opacity-50"
                >
                  {submitting ? 'Recording...' : 'Record Movement'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
