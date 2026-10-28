import { useEffect, useState } from "react";
import { fetchInventory, fetchProducts } from "../api";

/**
 * What a CUSTOMER sees. Products and stock come from two separate services; the
 * "Recommended for you" strip degrades to Top Sellers when the gateway serves a fallback.
 */
export default function Storefront({ reco, onRefreshReco }) {
  const [products, setProducts] = useState(null);
  const [stock, setStock] = useState({});
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const [p, inv] = await Promise.all([fetchProducts(), fetchInventory()]);
        if (!active) return;
        setProducts(Array.isArray(p) ? p : []);
        setStock(Object.fromEntries((Array.isArray(inv) ? inv : []).map((i) => [i.productId, i.quantity])));
        setError("");
      } catch (e) {
        if (active) setError(e.message);
      }
    }
    load();
    const id = setInterval(load, 3000);
    return () => { active = false; clearInterval(id); };
  }, []);

  const degraded = reco?.outcome === "fallback";

  return (
    <div className="card storefront">
      <div className="store-head">
        <h3>Customer view: storefront</h3>
        <span className="muted">what shoppers see</span>
      </div>

      <div className={`reco ${degraded ? "degraded" : ""}`}>
        <div className="reco-head">
          <strong>{degraded ? "Top sellers" : "Recommended for you"}</strong>
          {reco && (
            <span className={`pill ${degraded ? "degraded" : "up"}`}>
              {degraded ? "FALLBACK - recommendation service unavailable" : "LIVE"}
            </span>
          )}
          <button className="link-btn" onClick={onRefreshReco}>refresh</button>
        </div>
        <div className="reco-items">
          {(reco?.items ?? []).slice(0, 4).map((r) => (
            <span key={r.productId} className="chip">{r.name}</span>
          ))}
          {!reco && <span className="muted">loading...</span>}
        </div>
      </div>

      {error && <p className="error">Catalogue unavailable: {error}</p>}
      <div className="products">
        {(products ?? []).map((p) => {
          const qty = stock[p.id];
          return (
            <div key={p.id} className="product">
              <div className="product-name">{p.name}</div>
              <div className="muted">{p.category}</div>
              <div className="product-foot">
                <strong>${Number(p.price).toFixed(2)}</strong>
                <span className={qty === 0 ? "stock out" : "stock in"}>
                  {qty === undefined ? "..." : qty === 0 ? "Out of stock" : `${qty} in stock`}
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
