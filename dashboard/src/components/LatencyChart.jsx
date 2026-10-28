import {
  Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend,
} from "chart.js";
import { Line } from "react-chartjs-2";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend);

const COLORS = {
  live: "#22c55e", fallback: "#f59e0b", "rate-limited": "#a78bfa", rejected: "#f97316", error: "#ef4444",
};

export default function LatencyChart({ samples }) {
  const data = {
    labels: samples.map((_, i) => i + 1),
    datasets: [{
      label: "Round-trip time (ms)",
      data: samples.map((s) => s.ms),
      borderColor: "#64748b",
      borderWidth: 2,
      tension: 0.25,
      pointRadius: 5,
      pointBackgroundColor: samples.map((s) => COLORS[s.outcome]),
    }],
  };
  const options = {
    responsive: true,
    maintainAspectRatio: false,
    animation: false,
    plugins: {
      legend: { display: false },
      tooltip: { callbacks: { afterLabel: (ctx) => `outcome: ${samples[ctx.dataIndex]?.outcome}` } },
    },
    scales: {
      y: { beginAtZero: true, title: { display: true, text: "ms" } },
      x: { display: false },
    },
  };
  return (
    <div className="card">
      <div className="chart-head">
        <h3>GET /recommendations - live probe</h3>
        <div className="legend">
          {Object.entries(COLORS).map(([k, c]) => (
            <span key={k}><i style={{ background: c }} />{k}</span>
          ))}
        </div>
      </div>
      <div className="chart-box">
        {samples.length ? <Line data={data} options={options} /> : <p className="muted">Turn on "Auto traffic" to start probing.</p>}
      </div>
    </div>
  );
}
