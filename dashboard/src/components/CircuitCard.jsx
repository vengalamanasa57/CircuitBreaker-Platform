const HINT = {
  CLOSED: "Healthy - requests flow to the service.",
  OPEN: "Tripped - requests fail fast and the fallback is served.",
  HALF_OPEN: "Probing - a few trial requests decide whether to close.",
};

const pretty = (name) =>
  name.replace("ServiceCircuitBreaker", "").replace(/^./, (c) => c.toUpperCase());

export default function CircuitCard({ breaker }) {
  const { state, failureRate, failureRateThreshold, bufferedCalls, failedCalls, notPermittedCalls } = breaker;
  const rate = failureRate ?? 0;
  return (
    <div className={`card circuit ${state.toLowerCase()}`}>
      <div className="circuit-head">
        <h3>{pretty(breaker.name)}</h3>
        <span className={`pill ${state.toLowerCase()}`}>
          <span className="dot" />
          {state.replace("_", "-")}
        </span>
      </div>
      <p className="muted">{HINT[state]}</p>

      <div className="bar" title={`Failure threshold ${failureRateThreshold}%`}>
        <div className="bar-fill" style={{ width: `${Math.min(rate, 100)}%` }} />
        <div className="bar-threshold" style={{ left: `${failureRateThreshold}%` }} />
      </div>
      <div className="stats">
        <Stat label="Failure rate" value={failureRate == null ? "n/a" : `${failureRate}%`} />
        <Stat label="Window calls" value={bufferedCalls} />
        <Stat label="Failed" value={failedCalls} />
        <Stat label="Short-circuited" value={notPermittedCalls} />
      </div>
    </div>
  );
}

function Stat({ label, value }) {
  return (
    <div className="stat">
      <span className="stat-value">{value}</span>
      <span className="stat-label">{label}</span>
    </div>
  );
}
