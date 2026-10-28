#!/usr/bin/env bash
# Scripted proof of resilience. Run while the platform is up (default gateway: localhost:8080).
# Shows the full breaker lifecycle in the terminal: healthy -> slow service -> OPEN -> recovery.
GW=${GATEWAY_URL:-http://localhost:8080}
state() { curl -s "$GW/resilience/status" | python3 -c "import json,sys;print(next(b['state'] for b in json.load(sys.stdin)['circuitBreakers'] if 'recommendation' in b['name']))"; }
call()  { curl -s -o /dev/null -D /tmp/cb_h -w "%{http_code} in %{time_total}s" "$GW/recommendations"; grep -qi '^x-fallback: true' /tmp/cb_h && echo -n " [FALLBACK]"; echo; }
chaos() { curl -s -X POST -H 'Content-Type: application/json' -d "$1" "$GW/admin/recommendation/chaos" >/dev/null; }

echo "1) Healthy:                breaker=$(state)"; call
echo; echo "2) Injecting 4s latency (gateway time limit is 3s)"; chaos '{"mode":"LATENCY","latencyMs":4000}'
for i in 1 2 3 4 5 6; do printf "   request %d -> " $i; call; done
echo "   breaker=$(state)   <- fallbacks now served instantly"
echo; echo "3) Other services are unaffected:"; curl -s -o /dev/null -w "   GET /products -> %{http_code} in %{time_total}s\n" "$GW/products"
echo; echo "4) Healing the service and waiting 11s for HALF_OPEN"; chaos '{"mode":"NONE"}'; sleep 11
echo "   breaker=$(state)"
for i in 1 2 3; do printf "   trial %d -> " $i; call; done
echo "   breaker=$(state)"
