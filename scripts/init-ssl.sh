#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

if [ -z "${DOMAIN:-}" ]; then
    echo "Error: DOMAIN environment variable is required"
    echo "Usage: DOMAIN=example.com EMAIL=admin@example.com ./scripts/init-ssl.sh"
    exit 1
fi

if [ -z "${EMAIL:-}" ]; then
    echo "Error: EMAIL environment variable is required (for Let's Encrypt notifications)"
    echo "Usage: DOMAIN=example.com EMAIL=admin@example.com ./scripts/init-ssl.sh"
    exit 1
fi

STAGING="${STAGING:-0}"

echo "=== GTD SSL Certificate Setup ==="
echo "Domain:  $DOMAIN"
echo "Email:   $EMAIL"
echo "Staging: $([ "$STAGING" = "1" ] && echo "YES (test certificate)" || echo "NO (production certificate)")"
echo ""

CERTBOT_DIR="$PROJECT_DIR/certbot"
mkdir -p "$CERTBOT_DIR/conf" "$CERTBOT_DIR/www"

NGINX_CONF_DIR="$PROJECT_DIR/nginx"
NGINX_GENERATED="$NGINX_CONF_DIR/nginx-generated.conf"

echo "[1/5] Generating initial Nginx config for ACME challenge..."
sed "s/__DOMAIN__/$DOMAIN/g" "$NGINX_CONF_DIR/nginx-init.conf" > "$NGINX_GENERATED"

echo "[2/5] Starting Nginx for certificate verification..."
cd "$PROJECT_DIR"
docker compose -f docker-compose.prod.yml up -d nginx
sleep 3

if ! docker compose -f docker-compose.prod.yml ps nginx | grep -q "Up\|running"; then
    echo "Error: Nginx failed to start. Check: docker compose -f docker-compose.prod.yml logs nginx"
    exit 1
fi
echo "Nginx is running."

echo "[3/5] Requesting SSL certificate from Let's Encrypt..."
STAGING_ARG=""
if [ "$STAGING" = "1" ]; then
    STAGING_ARG="--staging"
fi

docker run --rm \
    -v "$CERTBOT_DIR/conf:/etc/letsencrypt" \
    -v "$CERTBOT_DIR/www:/var/www/certbot" \
    certbot/certbot certonly \
    --webroot \
    --webroot-path=/var/www/certbot \
    --email "$EMAIL" \
    --agree-tos \
    --no-eff-email \
    -d "$DOMAIN" \
    -d "www.$DOMAIN" \
    $STAGING_ARG

if [ ! -f "$CERTBOT_DIR/conf/live/$DOMAIN/fullchain.pem" ]; then
    echo "Error: Certificate not found after certbot run."
    echo "Check output above for errors. If DNS is not ready, try again later."
    exit 1
fi
echo "Certificate obtained successfully."

echo "[4/5] Switching Nginx to HTTPS configuration..."
sed "s/__DOMAIN__/$DOMAIN/g" "$NGINX_CONF_DIR/nginx.conf" > "$NGINX_GENERATED"

echo "[5/5] Reloading Nginx with HTTPS..."
docker compose -f docker-compose.prod.yml exec nginx nginx -s reload

echo ""
echo "=== SSL Setup Complete ==="
echo "Your app is now available at:"
echo "  https://$DOMAIN"
echo "  https://www.$DOMAIN"
echo ""
echo "HTTP requests are automatically redirected to HTTPS."
echo ""
echo "Certificate auto-renewal is handled by the certbot service in docker-compose.prod.yml."
echo "To test renewal: docker compose -f docker-compose.prod.yml exec certbot certbot renew --dry-run"
