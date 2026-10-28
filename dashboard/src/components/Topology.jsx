const STATE_COLOR = { CLOSED: "#22c55e", OPEN: "#ef4444", HALF_OPEN: "#f59e0b" };
const HEALTH_COLOR = { UP: "#22c55e", DOWN: "#ef4444", DEGRADED: "#f59e0b" };

const SERVICES = [
  { id: "product-service", label: "Product", port: 8081, y: 60 },
  { id: "inventory-service", label: "Inventory", port: 8082, y: 170 },
  { id: "recommendation-service", label: "Recommendation", port: 8083, y: 280 },
];

function breakerFor(breakers, serviceId) {
  const key = serviceId.split("-")[0]; // product | inventory | recommendation
  return breakers.find((b) => b.name.startsWith(key));
}

/** Live architecture diagram: edge colour = circuit breaker state, node border = service health. */
export default function Topology({ status, traffic }) {
  const breakers = status?.circuitBreakers ?? [];
  const services = status?.services ?? [];
  const reco = breakerFor(breakers, "recommendation-service");
  const fallbackActive = reco && reco.state !== "CLOSED";

  return (
    <div className="card">
      <div className="store-head">
        <h3>Ops view: live architecture</h3>
        <span className="muted">edge colour = circuit breaker state</span>
      </div>
      <svg viewBox="0 0 760 340" className="topology" role="img" aria-label="Live microservice topology">
        {/* Browser -> Gateway */}
        <line x1="130" y1="170" x2="250" y2="170" className={traffic ? "edge flow" : "edge"} stroke="#64748b" />
        <Node x={20} y={140} w={110} h={60} title="Browser" sub="React dashboard" color="#64748b" />

        {/* Eureka */}
        <line x1="320" y1="130" x2="320" y2="82" stroke="#475569" strokeDasharray="3 4" />
        <Node x={255} y={30} w={130} h={50} title="Eureka :8761" sub="service registry" color="#475569" />

        {/* Gateway */}
        <Node x={250} y={140} w={140} h={60} title="API Gateway :8080" sub="limit · bulkhead · breaker" color="#3b82f6" />

        {/* Edges + services */}
        {SERVICES.map((svc) => {
          const br = breakerFor(breakers, svc.id);
          const state = br?.state ?? "CLOSED";
          const color = STATE_COLOR[state];
          const health = services.find((s) => s.service === svc.id)?.status ?? "DOWN";
          const open = state === "OPEN";
          const cy = svc.y + 25;
          return (
            <g key={svc.id}>
              <line
                x1="390" y1="170" x2="560" y2={cy}
                className={open ? "edge" : traffic ? "edge flow" : "edge"}
                stroke={color}
                strokeDasharray={open ? "2 6" : undefined}
              />
              <text x="475" y={(170 + cy) / 2 - 8} className="edge-label" fill={color}>
                {state.replace("_", "-")}
              </text>
              <Node
                x={560} y={svc.y} w={170} h={50}
                title={`${svc.label} :${svc.port}`}
                sub={health}
                color={HEALTH_COLOR[health] ?? "#64748b"}
              />
            </g>
          );
        })}

        {/* Fallback box */}
        <line x1="320" y1="200" x2="320" y2="262" stroke={fallbackActive ? "#f59e0b" : "#334155"} strokeDasharray="4 4"
              className={fallbackActive ? "flow" : ""} />
        <Node x={235} y={262} w={170} h={50} title="Fallback: Top Sellers" sub={fallbackActive ? "ACTIVE" : "standby"}
              color={fallbackActive ? "#f59e0b" : "#334155"} />
      </svg>
    </div>
  );
}

function Node({ x, y, w, h, title, sub, color }) {
  return (
    <g>
      <rect x={x} y={y} width={w} height={h} rx="10" className="node" stroke={color} />
      <text x={x + w / 2} y={y + h / 2 - 3} textAnchor="middle" className="node-title">{title}</text>
      <text x={x + w / 2} y={y + h / 2 + 14} textAnchor="middle" className="node-sub" fill={color}>{sub}</text>
    </g>
  );
}
