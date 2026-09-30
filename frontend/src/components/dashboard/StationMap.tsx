import React from 'react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { STATIONS, Station } from '../../types';
import { useStationContext } from '../../context/StationContext';

// Fix leaflet default icon in vite/webpack builds
const defaultIcon = L.icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
});

L.Marker.prototype.options.icon = defaultIcon;

export const StationMap: React.FC = () => {
  const { selectedStation, setSelectedStation } = useStationContext();

  return (
    <div className="rounded-lg border border-slate-800/80 bg-slate-900/50 p-4 backdrop-blur-sm overflow-hidden flex flex-col h-[380px]">
      <div className="flex items-center justify-between pb-3">
        <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-300">
          Global Polar Stations Map
        </h3>
        <span className="text-[11px] text-slate-500">Antarctica & Arctic Nodes</span>
      </div>

      <div className="flex-1 w-full rounded overflow-hidden border border-slate-800 relative z-10">
        <MapContainer
          center={[-30, 45]}
          zoom={1}
          scrollWheelZoom={false}
          className="h-full w-full bg-slate-950"
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          {Object.values(STATIONS).map((station: Station) => {
            const isSelected = selectedStation.id === station.id;
            return (
              <Marker
                key={station.id}
                position={[station.latitude, station.longitude]}
                eventHandlers={{
                  click: () => setSelectedStation(station),
                }}
              >
                <Popup className="text-slate-900">
                  <div className="text-xs font-semibold">{station.name}</div>
                  <div className="text-[11px] text-slate-600">Code: {station.code}</div>
                  <div className="text-[11px] text-slate-600">Status: {station.status}</div>
                  <button
                    onClick={() => setSelectedStation(station)}
                    className="mt-2 w-full text-[10px] bg-blue-600 text-white py-1 px-2 rounded font-medium"
                  >
                    {isSelected ? 'Currently Selected' : 'Switch Station'}
                  </button>
                </Popup>
              </Marker>
            );
          })}
        </MapContainer>
      </div>
    </div>
  );
};
