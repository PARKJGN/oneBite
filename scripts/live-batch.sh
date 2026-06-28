#!/usr/bin/env bash
# oneBite 전체 배치 라이브 검증: 회원가입 → 로그인(JWT) → 동의/슬롯 → pipeline:run → 확인 → dispatch:run.
#
# 사전조건:
#   1) 인프라:  docker compose -f infra/docker-compose.yml up -d   (postgres + kafka)
#   2) 앱 기동 (요약 제공사 택1, internal 토큰 포함):
#      - GLM:    GLM_API_KEY=... ONEBITE_INTERNAL_TOKEN=... ./gradlew bootRun \
#                  --args='--onebite.summarizer=glm --onebite.messaging.kafka=true'
#      - Claude: ANTHROPIC_API_KEY=... 동일
#      (messaging.kafka=true=실 Kafka 발행. 기동 시 Flyway가 V1~V7 적용)
#   3) 이 스크립트 실행:  bash scripts/live-batch.sh
#
# 환경변수(선택):
#   BASE_URL               기본 http://localhost:8080
#   CATEGORIES             기본 economy,politics  (살아있는 고볼륨 피드 → 신선도 안전)
#   LANG_OUT               기본 ko
#   ONEBITE_INTERNAL_TOKEN /internal 공유 시크릿. 미지정 시 infra/.env 에서 읽음.
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BASE="${BASE_URL:-http://localhost:8080}"
CATEGORIES="${CATEGORIES:-economy,politics}"
LANG_OUT="${LANG_OUT:-ko}"
TODAY="$(date +%F)"
UNAME="live_$(date +%s)"
CAT_JSON="$(printf '%s' "$CATEGORIES" | awk -F, '{for(i=1;i<=NF;i++){printf "%s\"%s\"",(i>1?",":""),$i}}')"
INTERNAL_TOKEN="${ONEBITE_INTERNAL_TOKEN:-$(grep -s '^ONEBITE_INTERNAL_TOKEN=' "$ROOT/infra/.env" 2>/dev/null | cut -d= -f2-)}"

say(){ printf '\n\033[1;36m== %s ==\033[0m\n' "$1"; }
num(){ grep -o "\"$1\":[0-9]*" | head -1 | grep -o '[0-9]*'; }
strval(){ grep -o "\"$1\":\"[^\"]*\"" | head -1 | sed "s/.*\"$1\":\"\\([^\"]*\\)\".*/\\1/"; }

say "1) 회원가입 ($UNAME)"
SIGNUP="$(curl -s -X POST "$BASE/auth/signup" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$UNAME\",\"password\":\"Pw!12345\",\"nickname\":\"live\"}")"
echo "$SIGNUP"

say "2) 로그인 → JWT access 토큰 발급"
LOGIN="$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$UNAME\",\"password\":\"Pw!12345\"}")"
TOKEN="$(printf '%s' "$LOGIN" | strval token)"
if [ -z "${TOKEN:-}" ]; then echo "!! 토큰 발급 실패: $LOGIN"; exit 1; fi
echo "token=${TOKEN:0:24}…(JWT)"
AUTH=(-H "Authorization: Bearer $TOKEN")

say "3) 동의 게이트 통과: 타임존 + 언어 + pushPermission=granted"
curl -s -X PATCH "$BASE/me" "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d "{\"timezone\":\"Asia/Seoul\",\"outputLanguage\":\"$LANG_OUT\",\"pushPermission\":\"granted\"}"; echo

say "4) 슬롯 생성 (categories=$CATEGORIES)"
curl -s -X POST "$BASE/slots" "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d "{\"categoryCodes\":[$CAT_JSON]}"; echo

say "5) 파이프라인 실행 — 실제 요약 호출 (generated>=1 이면 성공)"
PIPE="$(curl -s -X POST "$BASE/internal/pipeline:run" -H "X-Internal-Token: $INTERNAL_TOKEN")"
echo "$PIPE"
GEN="$(printf '%s' "$PIPE" | num generated)"

say "6) 오늘 발송분 확인 (/today)"
TODAY_JSON="$(curl -s "$BASE/today" "${AUTH[@]}")"
echo "$TODAY_JSON"
EID="$(printf '%s' "$TODAY_JSON" | num editionId)"

if [ -n "${EID:-}" ]; then
  say "7) 에디션 상세 (/editions/$EID) — oneLine·3요소·crossInsight 확인"
  curl -s "$BASE/editions/$EID" "${AUTH[@]}"; echo
else
  echo "(editionId 없음 — generated=$GEN. 0이면 신선 기사 0건 또는 요약 실패. 앱 로그 확인.)"
fi

say "8) 디스패치 ($TODAY) — Kafka 푸시 작업 produce"
curl -s -X POST "$BASE/internal/dispatch:run?date=$TODAY" -H "X-Internal-Token: $INTERNAL_TOKEN"; echo

say "완료"
echo "Kafka 확인:  docker compose -f infra/docker-compose.yml exec kafka \\"
echo "  /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \\"
echo "  --topic onebite.push.jobs --from-beginning --timeout-ms 5000"
