import React from 'react';
import { useTrafficStream } from './useTrafficStream';
import { TrafficMap } from './TrafficMap';
import { Activity, Car, AlertTriangle, ShieldCheck } from 'lucide-react';
import './App.css';

function App() {
  const { state, map } = useTrafficStream();

  return (
    <div className="app-container">
      {/* Sidebar: Control Tower Metrics */}
      <div className="sidebar">
        <div className="sidebar-header">
          <ShieldCheck className="text-blue-400 w-8 h-8" />
          <h1 className="logo-text">TRAFFIC CTRL</h1>
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

        {/* Mini Performance Graph (SVG) */}
        <div className="performance-box">
          <div className="logs-title">Performance Trend</div>
          <svg viewBox="0 0 200 60" className="w-full h-16 mt-2 opacity-50">
            <path
              d={`M 0 50 Q 50 ${50 - state.vehicles.length * 5} 100 40 T 200 30`}
              fill="none"
              stroke="#3b82f6"
              strokeWidth="2"
            />
            <rect x="0" y="58" width="200" height="2" fill="#1e293b" />
          </svg>
        </div>

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
          <div className="overlay-subtitle">REAL-TIME VISUALIZATION</div>
          <div className="overlay-title">Smart-Circulation Engine v4.0</div>
        </div>

        <TrafficMap state={state} map={map} />
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
