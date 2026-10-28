// All calls go through the same-origin /gateway prefix (Vite proxy in dev, nginx in Docker).
const BASE = "/gateway";

export async function fetchStatus() {
  const res = await fetch(`${BASE}/resilience/status`);
  if (!res.ok) throw new Error(`status HTTP ${res.status}`);
  return res.json();
}

/** Sends one probe request through the gateway and classifies the outcome. */
export async function probeRecommendations() {
  const started = performance.now();
  try {
    const res = await fetch(`${BASE}/recommendations`);
    const ms = Math.round(performance.now() - started);
    const at = Date.now();
    if (res.status === 429) return { at, ms, outcome: "rate-limited" };
    if (res.status === 503) return { at, ms, outcome: "rejected" };
    if (!res.ok) return { at, ms, outcome: "error" };
    const body = await res.json();
    return { at, ms, outcome: body.degraded ? "fallback" : "live", items: body.recommendations ?? [] };
  } catch {
    return { at: Date.now(), ms: Math.round(performance.now() - started), outcome: "error" };
  }
}

export async function getChaos() {
  const res = await fetch(`${BASE}/admin/recommendation/chaos`);
  if (!res.ok) throw new Error(`chaos HTTP ${res.status}`);
  return res.json();
}

export async function setChaos(mode, latencyMs) {
  const res = await fetch(`${BASE}/admin/recommendation/chaos`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ mode, latencyMs }),
  });
  if (!res.ok) throw new Error(`chaos HTTP ${res.status}`);
  return res.json();
}

/** Fires n requests at once - demonstrates the rate limiter (429) and the bulkhead (503). */
export function probeBurst(n = 40) {
  return Promise.all(Array.from({ length: n }, () => probeRecommendations()));
}

export async function fetchProducts() {
  const res = await fetch(`${BASE}/products`);
  if (!res.ok) throw new Error(`products HTTP ${res.status}`);
  return res.json();
}

export async function fetchInventory() {
  const res = await fetch(`${BASE}/inventory`);
  if (!res.ok) throw new Error(`inventory HTTP ${res.status}`);
  return res.json();
}
