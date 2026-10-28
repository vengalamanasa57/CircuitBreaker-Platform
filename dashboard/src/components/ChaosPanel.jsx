import { useState } from "react";
import { setChaos } from "../api";

const MODES = [
  { mode: "LATENCY", label: "Inject latency (4s)", latencyMs: 4000, cls: "warn" },
  { mode: "ERROR", label: "Inject errors (HTTP 500)", cls: "danger" },
  { mode: "NONE", label: "Heal service", cls: "ok" },
];

export default function ChaosPanel({ chaos, onChange, onBurst, bursting }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function apply(m) {
    setBusy(true);
    setError("");
    try {
      onChange(await setChaos(m.mode, m.latencyMs));
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="card">
      <h3>Chaos control</h3>
      <p className="muted">
        Breaks the Recommendation Service on demand. Current mode: <strong>{chaos?.mode ?? "unknown"}</strong>
      </p>
      <div className="button-row">
        {MODES.map((m) => (
          <button key={m.mode} className={`btn ${m.cls}`} disabled={busy} onClick={() => apply(m)}>
            {m.label}
          </button>
        ))}
      </div>
      <h4>Load test</h4>
      <p className="muted">
        Fires 40 simultaneous requests: watch the <strong>429</strong> (rate limiter) and <strong>bulkhead</strong> counters rise.
        For the bulkhead, inject latency first.
      </p>
      <div className="button-row">
        <button className="btn info" disabled={bursting} onClick={onBurst}>
          {bursting ? "Sending..." : "Send burst (40 requests)"}
        </button>
      </div>
      {error && <p className="error">{error}</p>}
    </div>
  );
}
