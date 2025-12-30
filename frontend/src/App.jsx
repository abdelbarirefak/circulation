import React from 'react';
import { useTrafficStream } from './useTrafficStream';
import { TrafficMap } from './TrafficMap';
import { Activity, Car, AlertTriangle, ShieldCheck } from 'lucide-react';
import './App.css';

function App() {
  const { state, map } = useTrafficStream();
  const [selectedVehicle, setSelectedVehicle] = React.useState(null);

  const handleControl = (type, value) => {
    fetch('/api/control', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ type, value })
    });
  };

  return (
    <div className="app-container">
      {/* Sidebar: Control Tower Metrics */}
      <div className="sidebar">
        <div className="sidebar-header">
          <ShieldCheck className="text-blue-400 w-8 h-8" />
          <h1 className="logo-text">DIGITAL TWIN</h1>
        </div>

        <div className="metrics-grid">
          <MetricCard
            icon={<Car style={{ color: '#10b981' }} />}
            label="Active Agents"
            value={state.vehicles.length}
            trend="+2.4%"
          />
          <MetricCard
            icon={<Activity style={{ color: '#3b82f6' }} />}
            label="Flow Rate"
            value={`${(state.vehicles.length * 0.8).toFixed(1)}`}
            unit="veh/m"
            trend="+12%"
          />
          <MetricCard
            icon={<AlertTriangle style={{ color: '#f59e0b' }} />}
            label="Gridlock Risk"
            value="LOW"
            status="safe"
          />
        </div>

        {/* Live Telemetry Panel */}
        {selectedVehicle && (
          <div className="telemetry-panel">
            <h2 className="panel-title">LIVE TELEMETRY: {selectedVehicle.id}</h2>
            <div className="telemetry-grid">
              <div className="tel-item">
                <span className="tel-label">SPEED:</span>
                <span className="tel-value">{(selectedVehicle.speed * 3.6).toFixed(1)} km/h</span>
              </div>
              <div className="tel-item">
                <span className="tel-label">ROAD:</span>
                <span className="tel-value">{selectedVehicle.road}</span>
              </div>
              <div className="tel-item">
                <span className="tel-label">DECISION:</span>
                <span className="tel-value text-blue-400">{selectedVehicle.thought}</span>
              </div>
            </div>
            <div className="performance-box mt-4">
              <div className="logs-title">Path Confidence</div>
              <div className="w-full bg-slate-800 h-1 rounded-full overflow-hidden">
                <div className="bg-blue-500 h-full w-[85%]"></div>
              </div>
            </div>
          </div>
        )}

        <div className="logs-container">
          <h2 className="logs-title">System Telemetry</h2>
          <div className="logs-box">
            {state.vehicles.slice(-8).reverse().map(v => (
              <div key={v.id} className="log-entry">
                <span className="log-time">[{new Date().toLocaleTimeString([], { hour12: false })}]</span>
                <span className="log-id"> {v.id}</span>: {v.thought}
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Main Viewport */}
      <div className="viewport-container">
        <div className="overlay-info">
          <div className="overlay-subtitle">DIGITAL TWIN VISUALIZATION</div>
          <div className="overlay-title">Smart-Circulation Cloud v5.0</div>
        </div>

        {/* Simulation Control Bar */}
        <div className="control-bar">
          <button className="ctrl-btn" onClick={() => handleControl('pause')}>PAUSE</button>
          <button className="ctrl-btn" onClick={() => handleControl('resume')}>RESUME</button>
          <button className="ctrl-btn" onClick={() => handleControl('speed', 2.0)}>2X SPEED</button>
          <button className="ctrl-btn inject" onClick={() => handleControl('incident', { x: 500, y: 500 })}>INJECT INCIDENT</button>
        </div>

        <TrafficMap
          state={state}
          map={map}
          onSelectVehicle={setSelectedVehicle}
          selectedId={selectedVehicle?.id}
        />
      </div>
    </div>
  );
}

function MetricCard({ icon, label, value, unit, trend, status }) {
  return (
    <div className="metric-card">
      <div className="metric-icon-wrapper">
        {icon}
      </div>
      <div>
        <div className="metric-label">{label}</div>
        <div className="metric-value-container">
          <span className="metric-value">{value}</span>
          {unit && <span className="metric-unit">{unit}</span>}
          {trend && <span className="metric-trend">{trend}</span>}
          {status && <span className={`metric-status ${status}`}>{status}</span>}
        </div>
      </div>
    </div>
  );
}

export default App;
