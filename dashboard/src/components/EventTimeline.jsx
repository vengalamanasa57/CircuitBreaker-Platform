export default function EventTimeline({ events }) {
  return (
    <div className="card">
      <h3>Circuit breaker timeline</h3>
      {events.length === 0 ? (
        <p className="muted">No state transitions yet.</p>
      ) : (
        <ul className="timeline">
          {events.slice(0, 8).map((e, i) => (
            <li key={i}>
              <time>{new Date(e.time).toLocaleTimeString()}</time>
              <span>{e.breaker.replace("ServiceCircuitBreaker", "")}</span>
              <span className={`pill ${e.from.toLowerCase()}`}>{e.from}</span>
              <span>&rarr;</span>
              <span className={`pill ${e.to.toLowerCase()}`}>{e.to}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
