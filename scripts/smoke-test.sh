#!/usr/bin/env bash
# Yayin oncesi smoke testi: ayakta olan bir kurulumu disaridan, yalnizca web adresi uzerinden sinar.
#
#   ADMIN_EMAIL=... ADMIN_PASSWORD=... scripts/smoke-test.sh http://localhost
#
# Kontroller: saglik, guvenlik basliklari, API dokumantasyonunun kapali olmasi, demo girisinin reddi, admin girisi,
# yabanci origin'in reddi; proje -> rapor -> is kalemi -> takip kaydi akisi, dashboard, Excel ve degisiklik gecmisi.
# Olusturdugu kayitlari sonunda siler (gecmis olaylari kalir: gecmis yalnizca eklenir). Ilk hatada durur.
# Gerekenler: bash, curl, python3.
set -euo pipefail

BASE="${1:-http://localhost}"
BASE="${BASE%/}"
: "${ADMIN_EMAIL:?ADMIN_EMAIL gerekli}"
: "${ADMIN_PASSWORD:?ADMIN_PASSWORD gerekli}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
STEP=0

ok() { STEP=$((STEP + 1)); printf 'OK   %2d %s\n' "$STEP" "$1"; }
fail() { printf 'FAIL    %s\n' "$1" >&2; exit 1; }
# JSON alanini okur: json <dosya> <python ifadesi, d = govde>
json() { python3 -c "import json,sys; d=json.load(open(sys.argv[1])); print($2)" "$1"; }

# request <metot> <yol> [govde] -> durum kodu; govde $WORK/body, basliklar $WORK/headers
request() {
  local method="$1" path="$2" data="${3:-}"
  local args=(-s -o "$WORK/body" -D "$WORK/headers" -w '%{http_code}' -X "$method" "$BASE$path")
  [[ -n "${TOKEN:-}" ]] && args+=(-H "Authorization: Bearer $TOKEN")
  [[ -n "$data" ]] && args+=(-H 'Content-Type: application/json' --data "$data")
  curl "${args[@]}"
}
expect_status() {
  local want="$1" got="$2" what="$3"
  [[ "$got" == "$want" ]] || fail "$what: beklenen $want, gelen $got ($(head -c 300 "$WORK/body"))"
}
# grep -m1 kendisi durur (pipefail ile "| head" erken cikista SIGPIPE hatasi verebilir).
header() { grep -i -m1 "^$1:" "$WORK/headers" | cut -d' ' -f2- | tr -d '\r' || true; }

# --- Kurulum ---
expect_status 200 "$(request GET /api/health)" "saglik"
[[ "$(json "$WORK/body" "d['status'], d['databaseStatus']")" == "UP UP" ]] || fail "saglik: uygulama ya da veritabani UP degil"
ok "saglik: uygulama ve veritabani UP"

expect_status 200 "$(request GET /)" "web sayfasi"
for h in Content-Security-Policy X-Frame-Options X-Content-Type-Options Referrer-Policy Permissions-Policy; do
  [[ -n "$(header "$h")" ]] || fail "guvenlik basligi yok: $h"
done
[[ "$(header Server)" != *[0-9]* ]] || fail "Server basligi surum gosteriyor: $(header Server)"
ok "web sayfasi ve guvenlik basliklari; sunucu surumu gizli"

request GET /v3/api-docs > /dev/null
[[ "$(header Content-Type)" != application/json* ]] || fail "API dokumantasyonu acik"
ok "API dokumantasyonu kapali"

# --- Giris ---
BODY='{"email":"admin@example.com","password":"Demo1234!"}'
expect_status 401 "$(request POST /api/auth/login "$BODY")" "demo admin girisi"
ok "demo kullanicisi giris yapamiyor"

LOGIN=$(python3 -c 'import json,sys; print(json.dumps({"email": sys.argv[1], "password": sys.argv[2]}))' "$ADMIN_EMAIL" "$ADMIN_PASSWORD")
expect_status 200 "$(request POST /api/auth/login "$LOGIN")" "admin girisi"
TOKEN="$(json "$WORK/body" "d['token']")"
ADMIN_ID="$(json "$WORK/body" "d['user']['id']")"
[[ "$(json "$WORK/body" "d['user']['role']")" == ADMIN ]] || fail "giris yapan kullanici admin degil"
ok "admin girisi ($ADMIN_EMAIL)"

CORS=$(curl -s -o /dev/null -w '%{http_code}' -X OPTIONS "$BASE/api/projects" \
  -H 'Origin: https://baska-site.invalid' -H 'Access-Control-Request-Method: GET')
[[ "$CORS" == 403 ]] || fail "yabanci origin kabul edildi ($CORS)"
ok "yabanci origin'den API istegi reddediliyor"

# --- Akis ---
# JSON govdeleri once degiskene yazilir: macOS'taki bash 3.2, "$( ... )" icindeki "{\"a\":1,\"b\":2}" metnini
# suslu parantez genisletmesine sokup govdeyi bozar.
STAMP="$(date +%Y%m%d%H%M%S)"
BODY="{\"name\":\"Smoke test $STAMP\",\"status\":\"DEVAM_EDIYOR\",\"ownerId\":$ADMIN_ID}"
expect_status 201 "$(request POST /api/projects "$BODY")" "proje"
PROJECT="$(json "$WORK/body" "d['id']")"
# Gecen haftanin raporu, bugunun tarihiyle (rapor anlattigi haftanin ertesi haftasinda yazilabilir).
read -r YEAR WEEK TODAY FOLLOW_UP < <(python3 -c '
import datetime as dt
t = dt.date.today(); y, w, _ = (t - dt.timedelta(weeks=1)).isocalendar()
print(y, w, t.isoformat(), (t + dt.timedelta(days=3)).isoformat())')
BODY="{\"weekNumber\":$WEEK,\"reportDate\":\"$TODAY\",\"progressStage\":\"ANALIZ\",\"overallStatus\":\"YOLUNDA\",\"riskLevel\":\"DUSUK\"}"
expect_status 201 "$(request POST "/api/projects/$PROJECT/reports" "$BODY")" "rapor"
REPORT="$(json "$WORK/body" "d['id']")"
BODY='{"title":"Smoke test kalemi","status":"DEVAM_EDIYOR"}'
expect_status 201 "$(request POST "/api/reports/$REPORT/work-items" "$BODY")" "is kalemi"
WORK_ITEM="$(json "$WORK/body" "d['id']")"
BODY="{\"type\":\"AKSIYON\",\"title\":\"Smoke test aksiyonu\",\"assignee\":\"Smoke\",\"followUpDate\":\"$FOLLOW_UP\",\"status\":\"ACIK\"}"
expect_status 201 "$(request POST "/api/projects/$PROJECT/tracking-items" "$BODY")" "takip kaydi"
ITEM="$(json "$WORK/body" "d['id']")"
ok "proje, $YEAR / $WEEK. hafta raporu, is kalemi ve aksiyon olusturuldu"

expect_status 200 "$(request GET "/api/dashboard/summary?projectId=$PROJECT")" "dashboard"
[[ "$(json "$WORK/body" "d['projects'][0]['hasReport'], d['projects'][0]['openTrackingItems']")" == "True 1" ]] \
  || fail "dashboard projeyi raporuyla ve acik kaydiyla gostermiyor"
ok "dashboard projeyi raporu ve acik kaydiyla gosteriyor"

expect_status 200 "$(request GET "/api/dashboard/export?projectId=$PROJECT")" "Excel"
[[ "$(head -c 2 "$WORK/body")" == PK ]] || fail "Excel ciktisi gecerli bir .xlsx degil"
ok "Excel ciktisi indi ($(header Content-Disposition | grep -o 'cto-ozeti-[0-9-]*\.xlsx' | sed -n 1p))"

expect_status 200 "$(request GET "/api/projects/$PROJECT/history")" "gecmis"
[[ "$(json "$WORK/body" "d['totalElements']")" == 4 ]] || fail "gecmiste 4 olay yok"
[[ "$(json "$WORK/body" "d['content'][0]['actorName']")" != "" ]] || fail "gecmiste yazan kisi yok"
ok "degisiklik gecmisi 4 olayi yazan kisiyle gosteriyor"

# --- Temizlik ---
expect_status 204 "$(request DELETE "/api/tracking-items/$ITEM")" "aksiyonu silme"
expect_status 204 "$(request DELETE "/api/work-items/$WORK_ITEM")" "is kalemini silme"
expect_status 204 "$(request DELETE "/api/reports/$REPORT")" "raporu silme"
expect_status 204 "$(request DELETE "/api/projects/$PROJECT")" "projeyi silme"
ok "olusturulan kayitlar silindi"

echo "Smoke testi gecti: $STEP kontrol, $BASE"
