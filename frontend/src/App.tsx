import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { StationProvider } from './context/StationContext';
import { Layout } from './components/layout/Layout';
import { DashboardPage } from './pages/DashboardPage';
import { InventoryPage } from './pages/InventoryPage';
import { IntelligencePage } from './pages/IntelligencePage';
import { CargoPage } from './pages/CargoPage';
import { PersonnelPage } from './pages/PersonnelPage';
import { EmergencyPage } from './pages/EmergencyPage';

function App() {
  return (
    <StationProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Layout />}>
            <Route index element={<DashboardPage />} />
            <Route path="inventory" element={<InventoryPage />} />
            <Route path="intelligence" element={<IntelligencePage />} />
            <Route path="cargo" element={<CargoPage />} />
            <Route path="personnel" element={<PersonnelPage />} />
            <Route path="emergency" element={<EmergencyPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </StationProvider>
  );
}

export default App;
