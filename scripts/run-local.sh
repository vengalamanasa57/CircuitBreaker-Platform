#!/usr/bin/env bash
# Runs the whole platform WITHOUT Docker (needs JDK 17+ and Node 18+; Maven is provided by ./mvnw).
# Usage: scripts/run-local.sh          start everything
#        scripts/run-local.sh stop     stop everything
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p logs

stop() {
  [ -f logs/pids ] && xargs -r kill < logs/pids 2>/dev/null || true
  rm -f logs/pids
  for port in 8761 8080 8081 8082 8083 5173; do   # strays, e.g. the dashboard's node child
    command -v lsof >/dev/null && lsof -ti tcp:$port -sTCP:LISTEN | xargs -r kill 2>/dev/null || true
  done
  sleep 2
}
[ "${1:-}" = "stop" ] && { stop; echo "Stopped."; exit 0; }
stop

wait_for() { # name url
  printf "Waiting for %s " "$1"
  for _ in $(seq 1 90); do
    curl -fs "$2" >/dev/null 2>&1 && { echo "up"; return; }
    printf "."; sleep 2
  done
  echo "TIMED OUT - see logs/$1.log"; stop; exit 1
}

start() { # module
  nohup java -jar "$1/target/$1-1.0.0.jar" > "logs/$1.log" 2>&1 &
  echo $! >> logs/pids
}

./mvnw -q -DskipTests package
: > logs/pids
export TRACING_ENABLED=${TRACING_ENABLED:-false}   # no Zipkin locally unless you start it

start eureka-server
wait_for eureka-server http://localhost:8761/actuator/health
for m in product-service inventory-service recommendation-service gateway-service; do start "$m"; done
wait_for gateway-service http://localhost:8080/actuator/health

printf "Waiting for all services to register "
for _ in $(seq 1 60); do
  [ "$(curl -s -m 3 http://localhost:8080/resilience/status | grep -o '"status":"UP"' | wc -l)" -ge 3 ] && break
  printf "."; sleep 2
done
echo " ok"
echo "Warming up the gateway..."
for _ in $(seq 1 6); do
  for route in products inventory recommendations; do curl -s -m 15 -o /dev/null "http://localhost:8080/$route" || true; done
done

(cd dashboard && [ -d node_modules ] || npm ci --no-audit --no-fund)
(cd dashboard && nohup npm run dev > ../logs/dashboard.log 2>&1 & echo $! >> ../logs/pids)

echo
echo "Dashboard : http://localhost:5173"
echo "Gateway   : http://localhost:8080"
echo "Eureka    : http://localhost:8761"
echo "Verify    : scripts/smoke-test.sh"
echo "Stop with : scripts/run-local.sh stop"
