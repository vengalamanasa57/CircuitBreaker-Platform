const SERVED_BY = {
  live: "recommendation-service",
  fallback: "gateway fallback",
  "rate-limited": "gateway rate limiter (429)",
  rejected: "gateway bulkhead (503)",
  error: "error",
};
const PILL = { live: "up", fallback: "degraded", "rate-limited": "half_open", rejected: "half_open", error: "down" };

export default function RequestLog({ samples }) {
  const rows = [...samples].reverse().slice(0, 8);
  return (
    <div className="card">
      <h3>Request log: GET /recommendations</h3>
      {rows.length === 0 ? (
        <p className="muted">Turn on "Auto traffic" to see each request and who served it.</p>
      ) : (
        <table className="log">
          <thead><tr><th>Time</th><th>Result</th><th>Latency</th><th>Served by</th></tr></thead>
          <tbody>
            {rows.map((r, i) => (
              <tr key={i}>
                <td>{new Date(r.at).toLocaleTimeString()}</td>
                <td><span className={`pill ${PILL[r.outcome]}`}>{r.outcome}</span></td>
                <td>{r.ms} ms</td>
                <td>{SERVED_BY[r.outcome]}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
