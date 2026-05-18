#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

CERTBOT_DIR="$PROJECT_DIR/certbot"

if [ ! -d "$CERTBOT_DIR/conf/live" ]; then
    echo "Error: No certificates found. Run scripts/init-ssl.sh first."
    exit 1
fi

echo "$(date '+%Y-%m-%d %H:%M:%S') Starting certificate renewal check..."

docker run --rm \
    -v "$CERTBOT_DIR/conf:/etc/letsencrypt" \
    -v "$CERTBOT_DIR/www:/var/www/certbot" \
    certbot/certbot renew --quiet

echo "$(date '+%Y-%m-%d %H:%M:%S') Reloading Nginx..."
cd "$PROJECT_DIR"
docker compose -f docker-compose.prod.yml exec -T nginx nginx -s reload

echo "$(date '+%Y-%m-%d %H:%M:%S') Renewal check complete."
