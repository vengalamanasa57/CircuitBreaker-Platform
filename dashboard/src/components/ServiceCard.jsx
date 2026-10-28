const LABELS = {
  "product-service": "Product Service",
  "inventory-service": "Inventory Service",
  "recommendation-service": "Recommendation Service",
};

export default function ServiceCard({ service }) {
  const status = service.status;
  return (
    <div className="card service-card">
      <div>
        <h3>{LABELS[service.service] ?? service.service}</h3>
        <p className="muted">
          {service.healthyInstances}/{service.instances} instance(s) healthy
        </p>
      </div>
      <span className={`pill ${status.toLowerCase()}`}>
        <span className="dot" />
        {status}
      </span>
    </div>
  );
}
