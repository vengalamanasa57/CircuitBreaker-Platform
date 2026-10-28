#!/usr/bin/env bash
# End-to-end check of the running platform (Linux/macOS/Git Bash). Needs curl and python3.
#   scripts/smoke-test.sh           full test (~1 minute)
#   scripts/smoke-test.sh --skip-chaos
GW=${GATEWAY_URL:-http://localhost:8080}
SKIP_CHAOS=false; [ "${1:-}" = "--skip-chaos" ] && SKIP_CHAOS=true
failed=0
pass() { echo "  PASS  $1"; }
fail() { echo "  FAIL  $1"; failed=$((failed+1)); }
check() { if eval "$2"; then pass "$1"; else fail "$1"; fi; }
code() { curl -s -o /dev/null -w "%{http_code}" -X "$1" ${3:+-H 'Content-Type: application/json' -d "$3"} "$GW$2"; }
json() { curl -s "$GW$1" | python3 -c "import json,sys;d=json.load(sys.stdin);print($2)"; }
state() { json /resilience/status "next(b['state'] for b in d['circuitBreakers'] if 'recommendation' in b['name'])"; }
chaos() { curl -s -X POST -H 'Content-Type: application/json' -d "{\"mode\":\"$1\"}" "$GW/admin/recommendation/chaos" >/dev/null; }

echo; echo "[1] Routing through the gateway"
check "GET / describes the gateway"        '[ "$(json / "d[\"service\"]")" = "api-gateway" ]'
check "GET /products returns >= 8"         '[ "$(json /products "len(d)")" -ge 8 ]'
check "GET /inventory/1 in stock"          '[ "$(json /inventory/1 "d[\"inStock\"]")" = "True" ]'
check "GET /inventory/8 out of stock"      '[ "$(json /inventory/8 "d[\"inStock\"]")" = "False" ]'
check "GET /recommendations is live"       '[ "$(json /recommendations "d[\"degraded\"]")" = "False" ]'

echo; echo "[2] Business rules and validation"
check "POST /products creates (201)"       '[ "$(code POST /products "{\"name\":\"Smoke\",\"price\":9.9,\"category\":\"Misc\"}")" = "201" ]'
check "Invalid product is 400"             '[ "$(code POST /products "{\"name\":\"\",\"price\":-1,\"category\":\"\"}")" = "400" ]'
check "Unknown product is 404"             '[ "$(code GET /products/99999)" = "404" ]'
check "Over-reserving is 409"              '[ "$(code POST "/inventory/5/reserve?quantity=99999")" = "409" ]'

echo; echo "[3] Gateway status feed"
check "3 services UP"                      '[ "$(json /resilience/status "sum(1 for s in d[\"services\"] if s[\"status\"]==\"UP\")")" = "3" ]'
check "2 bulkheads"                        '[ "$(json /resilience/status "len(d[\"bulkheads\"])")" = "2" ]'

echo; echo "[4] Rate limiter"
n429=0; for _ in $(seq 1 80); do [ "$(code GET /recommendations)" = "429" ] && n429=$((n429+1)); done
check "Burst of 80 gets some 429s ($n429)" '[ $n429 -gt 0 ]'
sleep 3

if [ "$SKIP_CHAOS" = false ]; then
  echo; echo "[5] Circuit breaker lifecycle (~50 s)"
  chaos LATENCY
  for _ in $(seq 1 8); do curl -s -o /dev/null "$GW/recommendations"; [ "$(state)" = "OPEN" ] && break; done
  check "Breaker opens after timeouts"     '[ "$(state)" = "OPEN" ]'
  check "Products unaffected while OPEN"   '[ "$(code GET /products)" = "200" ]'
  chaos NONE; echo "  ...healed; waiting up to 40 s for CLOSED"
  for _ in $(seq 1 40); do sleep 1; curl -s -o /dev/null "$GW/recommendations"; [ "$(state)" = "CLOSED" ] && break; done
  check "Breaker recovers to CLOSED"       '[ "$(state)" = "CLOSED" ]'
fi
echo; [ $failed -eq 0 ] && { echo "ALL CHECKS PASSED"; exit 0; } || { echo "$failed CHECK(S) FAILED"; exit 1; }
