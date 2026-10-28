import { useCallback, useEffect, useRef, useState } from "react";
import { fetchStatus, getChaos, probeBurst, probeRecommendations } from "./api";
import ServiceCard from "./components/ServiceCard";
import CircuitCard from "./components/CircuitCard";
import ChaosPanel from "./components/ChaosPanel";
import LatencyChart from "./components/LatencyChart";
import EventTimeline from "./components/EventTimeline";
import Storefront from "./components/Storefront";
import Topology from "./components/Topology";
import RequestLog from "./components/RequestLog";

const MAX_SAMPLES = 40;

/** True when something answers at url. no-cors lets us probe a cross-origin server without reading it. */
function useReachable(url) {
  const [up, setUp] = useState(false);
  useEffect(() => {
    let active = true;
    const check = () =>
      fetch(url, { mode: "no-cors" }).then(() => active && setUp(true)).catch(() => active && setUp(false));
    check();
    const id = setInterval(check, 10000);
    return () => { active = false; clearInterval(id); };
  }, [url]);
  return up;
}

export default function Dashboard() {
  const [status, setStatus] = useState(null);
  const [offline, setOffline] = useState(false);
  const [chaos, setChaosState] = useState(null);
  const [samples, setSamples] = useState([]);
  const [traffic, setTraffic] = useState(false);
  const [bursting, setBursting] = useState(false);
  const [reco, setReco] = useState(null);
  const inFlight = useRef(false);
  const zipkinUp = useReachable("http://localhost:9411/health");

  const record = useCallback((newSamples) => {
    setSamples((prev) => [...prev, ...newSamples].slice(-MAX_SAMPLES));
    const withItems = [...newSamples].reverse().find((x) => x.items);
    if (withItems) setReco(withItems);
  }, []);

  const refreshReco = useCallback(async () => {
    record([await probeRecommendations()]);
  }, [record]);

  const refresh = useCallback(async () => {
    try {
      setStatus(await fetchStatus());
      setOffline(false);
      setChaosState(await getChaos().catch(() => null));
    } catch {
      setOffline(true);
    }
  }, []);

  useEffect(() => {
    refresh();
    const id = setInterval(refresh, 1500);
    return () => clearInterval(id);
  }, [refresh]);

  useEffect(() => {
    if (!traffic) return undefined;
    const id = setInterval(async () => {
      if (inFlight.current) return; // never stack probes behind a slow response
      inFlight.current = true;
      const sample = await probeRecommendations();
      inFlight.current = false;
      record([sample]);
    }, 1000);
    return () => clearInterval(id);
  }, [traffic, record]);

  useEffect(() => {
    refreshReco(); // one initial fetch so the storefront is not empty
  }, [refreshReco]);

  const sendBurst = async () => {
    setBursting(true);
    const results = await probeBurst(40);
    record(results);
    setBursting(false);
  };

  const count = (o) => samples.filter((s) => s.outcome === o).length;

  return (
    <div className="dashboard">
      <header className="header">
        <div>
          <h1>CircuitBreaker Monitor</h1>
          <p className="muted">Cloud-native e-commerce platform &middot; Spring Cloud Gateway + Resilience4j</p>
        </div>
        <div className="header-actions">
          <label className="switch">
            <input type="checkbox" checked={traffic} onChange={(e) => setTraffic(e.target.checked)} />
            Auto traffic (1 req/s)
          </label>
          <span className={`pill ${offline ? "down" : "up"}`}>
            <span className="dot" />
            Gateway {offline ? "UNREACHABLE" : "UP"}
          </span>
        </div>
      </header>

      {offline && (
        <div className="banner">
          Cannot reach the API gateway. Start the platform (see README) and this page will reconnect automatically.
        </div>
      )}

      <section>
        <h2>How it works</h2>
        <p className="muted">
          Left: what a <strong>customer</strong> sees. Right: what is happening <strong>inside</strong> the system.
          Break the Recommendation service below and watch the storefront degrade gracefully while everything else keeps working.
        </p>
        <div className="grid two">
          <Storefront reco={reco} onRefreshReco={refreshReco} />
          <Topology status={status} traffic={traffic} />
        </div>
      </section>

      <section>
        <h2>Microservices</h2>
        <div className="grid three">
          {(status?.services ?? []).map((s) => <ServiceCard key={s.service} service={s} />)}
        </div>
      </section>

      <section>
        <h2>Circuit breakers</h2>
        <div className="grid three">
          {(status?.circuitBreakers ?? []).map((b) => <CircuitCard key={b.name} breaker={b} />)}
        </div>
      </section>

      <section className="grid two">
        <ChaosPanel chaos={chaos} onChange={setChaosState} onBurst={sendBurst} bursting={bursting} />
        <div className="card">
          <h3>Probe results (last {samples.length})</h3>
          <div className="stats big">
            <div className="stat"><span className="stat-value ok-text">{count("live")}</span><span className="stat-label">live</span></div>
            <div className="stat"><span className="stat-value warn-text">{count("fallback")}</span><span className="stat-label">fallback</span></div>
            <div className="stat"><span className="stat-value">{count("rate-limited")}</span><span className="stat-label">429</span></div>
            <div className="stat"><span className="stat-value">{count("rejected")}</span><span className="stat-label">bulkhead</span></div>
          </div>
          <h4>Bulkheads</h4>
          {(status?.bulkheads ?? []).map((b) => (
            <div key={b.name} className="bulk">
              <span>{b.name}</span>
              <span className="muted">{b.maxConcurrentCalls - b.availableConcurrentCalls}/{b.maxConcurrentCalls} in use</span>
            </div>
          ))}
        </div>
      </section>

      <section className="grid two">
        <LatencyChart samples={samples} />
        <EventTimeline events={status?.events ?? []} />
      </section>

      <section className="grid one">
        <RequestLog samples={samples} />
      </section>

      <footer className="muted">
        <a href="http://localhost:8761" target="_blank" rel="noreferrer">Eureka</a> &middot;{" "}
        {zipkinUp ? (
          <a href="http://localhost:9411" target="_blank" rel="noreferrer">Zipkin traces</a>
        ) : (
          <span title="Run the platform with: docker compose up --build">Zipkin tracing: not running (start with Docker)</span>
        )}
      </footer>
    </div>
  );
}
