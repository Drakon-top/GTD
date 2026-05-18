#!/usr/bin/env bash
#
# Health check script for GTD backend.
# Intended to be run via cron for uptime monitoring.
#
# Usage:
#   # Check every 5 minutes, log failures:
#   */5 * * * * /opt/gtd/scripts/health-check.sh >> /var/log/gtd-health.log 2>&1
#
# Environment variables:
#   HEALTH_URL    - health endpoint (default: http://localhost:8080/actuator/health)
#   ALERT_WEBHOOK - optional webhook URL for failure alerts (Telegram, Slack, etc.)
#   TIMEOUT       - curl timeout in seconds (default: 10)

set -euo pipefail

HEALTH_URL="${HEALTH_URL:-http://localhost:8080/actuator/health}"
TIMEOUT="${TIMEOUT:-10}"
ALERT_WEBHOOK="${ALERT_WEBHOOK:-}"
TIMESTAMP=$(date -u '+%Y-%m-%dT%H:%M:%SZ')

check_health() {
    local http_code
    local body

    body=$(curl -sf --max-time "$TIMEOUT" -w "\n%{http_code}" "$HEALTH_URL" 2>/dev/null) || {
        echo "$TIMESTAMP [FAIL] Health check unreachable: $HEALTH_URL"
        return 1
    }

    http_code=$(echo "$body" | tail -1)
    local json_body
    json_body=$(echo "$body" | head -n -1)

    if [ "$http_code" -ne 200 ]; then
        echo "$TIMESTAMP [FAIL] Health check returned HTTP $http_code"
        return 1
    fi

    local status
    status=$(echo "$json_body" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)

    if [ "$status" != "UP" ]; then
        echo "$TIMESTAMP [FAIL] Health status: $status (response: $json_body)"
        return 1
    fi

    echo "$TIMESTAMP [OK] status=UP http=$http_code"
    return 0
}

send_alert() {
    local message="$1"
    if [ -n "$ALERT_WEBHOOK" ]; then
        curl -sf --max-time 10 -X POST "$ALERT_WEBHOOK" \
            -H "Content-Type: application/json" \
            -d "{\"text\": \"🔴 GTD Backend DOWN: $message\"}" \
            >/dev/null 2>&1 || true
    fi
}

if ! check_health; then
    send_alert "Health check failed at $TIMESTAMP ($HEALTH_URL)"
    exit 1
fi
