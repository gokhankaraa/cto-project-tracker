#!/usr/bin/env bash
# Demo kurulumunu (docker-compose.yml) temiz demo verisine dondurur: konteynerler ve veri volume'u silinir, imajlar
# yeniden derlenir, kurulum acilir ve uygulama hazir olunca adresler ve demo girisleri yazilir. Demo verisinin
# tarihleri yuklendigi gune goredir; sifirlanan demo her gun ayni gorunur (or. bir aksiyonun takibi hep gecikmis).
#
#   scripts/demo-reset.sh          # onay sorar
#   scripts/demo-reset.sh --yes    # sormadan (or. sunumdan hemen once)
#
# Yalnizca gelistirme/demo kurulumunu siler. Uretim kurulumunun (docker-compose.prod.yml) proje adi ve volume'u
# ayridir; ona dokunmaz. Gerekenler: bash, docker (compose), curl.
set -euo pipefail

cd "$(dirname "$0")/.."
APP_PORT="${APP_PORT:-8080}"
WEB_PORT="${WEB_PORT:-3000}"

if [[ "${1:-}" != "--yes" ]]; then
  read -r -p "Demo kurulumundaki TUM veri silinip demo verisi yeniden yuklenecek. Devam edilsin mi? [e/H] " answer
  [[ "$answer" == [eE] ]] || { echo "Vazgecildi."; exit 1; }
fi

docker compose down -v
docker compose up -d --build

printf 'Uygulama bekleniyor'
for _ in $(seq 1 90); do
  if curl -sf "http://localhost:$APP_PORT/api/health" > /dev/null && curl -sf "http://localhost:$WEB_PORT/" > /dev/null; then
    printf '\n\nDemo hazir: http://localhost:%s (API: http://localhost:%s, Swagger: /swagger-ui.html)\n' "$WEB_PORT" "$APP_PORT"
    cat <<'EOF'
Demo kullanicilari (parola: Demo1234!):
  cto@example.com     CTO              tum portfoy, dashboard, Excel
  ayse@example.com    proje yoneticisi Atlas IK, Nova POS
  mehmet@example.com  proje yoneticisi e-Belge, Orion Mobil
  admin@example.com   Admin            proje yonetimi
EOF
    exit 0
  fi
  printf '.'
  sleep 2
done
printf '\nUygulama 3 dakikada hazir olmadi. Loglar: docker compose logs app\n' >&2
exit 1
