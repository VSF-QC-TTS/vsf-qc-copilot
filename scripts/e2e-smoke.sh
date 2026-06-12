#!/usr/bin/env bash
# ============================================================
# VSF QC Copilot — E2E Smoke Test
# Full flow: register → project → connector → dataset →
#            rubric → evaluation → export XLSX
# ============================================================
# Prerequisites:
#   1. docker compose up --build -d
#   2. Wait ~30s for API to start
#   3. GEMINI_API_KEY set in server/.env (for mock chatbot + grading)
#
# Usage:
#   bash scripts/e2e-smoke.sh
#
# The script will:
#   - Create a test user and bypass email verification via DB
#   - Walk through the entire QC evaluation flow
#   - Download the exported XLSX file to ./e2e-export.xlsx
# ============================================================

set -euo pipefail

# ── Config ──────────────────────────────────────────────────
BASE_URL="${BASE_URL:-http://localhost:8080}"
DB_CONTAINER="${DB_CONTAINER:-vsf-qc-copilot-db-1}"
EMAIL="e2e-$(date +%s)@test.local"
PASSWORD="TestPass123!"
DISPLAY_NAME="E2E Smoke"
EXPORT_FILE="./e2e-export.xlsx"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

step=0
step() {
  step=$((step + 1))
  echo -e "\n${CYAN}━━━ Step $step: $1 ━━━${NC}"
}

ok() { echo -e "  ${GREEN}✓ $1${NC}"; }
fail() { echo -e "  ${RED}✗ $1${NC}"; exit 1; }
info() { echo -e "  ${YELLOW}→ $1${NC}"; }

# JSON helper: extract field from JSON (uses python for portability)
json_val() {
  python3 -c "import sys,json; print(json.load(sys.stdin)$1)"
}

api() {
  local method=$1 path=$2
  shift 2
  curl -s -X "$method" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${ACCESS_TOKEN:-}" \
    "$@" \
    "${BASE_URL}${path}"
}

# ── Step 1: Register ────────────────────────────────────────
step "Register user"
REGISTER_RESP=$(api POST /api/v1/auth/register -d "{
  \"email\": \"$EMAIL\",
  \"password\": \"$PASSWORD\",
  \"displayName\": \"$DISPLAY_NAME\"
}")
USER_ID=$(echo "$REGISTER_RESP" | json_val "['publicId']") || fail "Register failed: $REGISTER_RESP"
ok "User registered: $USER_ID ($EMAIL)"

# ── Step 2: Bypass email verification via DB ────────────────
step "Bypass email verification (activate user in DB)"
docker exec "$DB_CONTAINER" psql -U vqc -d vqc_dev -c \
  "UPDATE users SET status = 'ACTIVE' WHERE username = '$EMAIL';" > /dev/null
ok "User activated in DB"

# ── Step 3: Login ───────────────────────────────────────────
step "Login"
LOGIN_RESP=$(api POST /api/v1/auth/login -d "{
  \"email\": \"$EMAIL\",
  \"password\": \"$PASSWORD\"
}")
ACCESS_TOKEN=$(echo "$LOGIN_RESP" | json_val "['accessToken']") || fail "Login failed: $LOGIN_RESP"
ok "Access token obtained"

# ── Step 4: Create project ──────────────────────────────────
step "Create project"
PROJECT_RESP=$(api POST /api/v1/projects -d '{
  "name": "E2E Smoke Project",
  "description": "Automated end-to-end smoke test"
}')
PROJECT_ID=$(echo "$PROJECT_RESP" | json_val "['publicId']") || fail "Create project failed: $PROJECT_RESP"
ok "Project: $PROJECT_ID"

# ── Step 5: Create connector (points to mock chatbot) ───────
step "Create target API connector → mock chatbot"
CONNECTOR_RESP=$(api POST "/api/v1/projects/$PROJECT_ID/target-api-connectors" -d "{
  \"name\": \"Mock Chatbot Connector\",
  \"protocol\": \"HTTP\",
  \"method\": \"POST\",
  \"baseUrl\": \"$BASE_URL\",
  \"path\": \"/mock-chatbot/chat\",
  \"url\": \"$BASE_URL/mock-chatbot/chat\",
  \"bodyType\": \"RAW_JSON\",
  \"bodyTemplate\": {
    \"message\": \"{{question}}\"
  },
  \"authType\": \"NONE\",
  \"responseFormat\": \"JSON\",
  \"responseSelector\": \"$.answer\",
  \"isStreaming\": false,
  \"timeoutSeconds\": 60,
  \"retryCount\": 1,
  \"active\": true
}")
CONNECTOR_ID=$(echo "$CONNECTOR_RESP" | json_val "['publicId']") || fail "Create connector failed: $CONNECTOR_RESP"
ok "Connector: $CONNECTOR_ID"

# ── Step 6: Create dataset ──────────────────────────────────
step "Create dataset"
DATASET_RESP=$(api POST "/api/v1/projects/$PROJECT_ID/datasets" -d '{
  "name": "E2E Health QA",
  "description": "Simple health questions for smoke testing"
}')
DATASET_ID=$(echo "$DATASET_RESP" | json_val "['publicId']") || fail "Create dataset failed: $DATASET_RESP"
ok "Dataset: $DATASET_ID"

# ── Step 7: Create test cases ───────────────────────────────
step "Create test cases"
for i in 1 2 3; do
  QUESTIONS=("How many steps should I walk per day for good health?"
             "What is the recommended daily water intake?"
             "How many hours of sleep does an adult need?")
  ANSWERS=("10000 steps"
           "2 liters or 8 glasses"
           "7 to 9 hours")

  TC_RESP=$(api POST "/api/v1/datasets/$DATASET_ID/test-cases" -d "{
    \"question\": \"${QUESTIONS[$((i-1))]}\",
    \"groundTruth\": \"${ANSWERS[$((i-1))]}\"
  }")
  TC_ID=$(echo "$TC_RESP" | json_val "['publicId']") || fail "Create test case $i failed: $TC_RESP"
  ok "Test case $i: $TC_ID"
done

# ── Step 8: Approve dataset ─────────────────────────────────
step "Approve dataset"
api PATCH "/api/v1/datasets/$DATASET_ID" -d '{"status": "APPROVED"}' > /dev/null
ok "Dataset approved"

# ── Step 9: Create rubric ───────────────────────────────────
step "Create rubric"
RUBRIC_RESP=$(api POST "/api/v1/projects/$PROJECT_ID/rubrics" -d '{
  "name": "Health QA Rubric",
  "description": "Evaluates health chatbot answers"
}')
RUBRIC_ID=$(echo "$RUBRIC_RESP" | json_val "['publicId']") || fail "Create rubric failed: $RUBRIC_RESP"
ok "Rubric: $RUBRIC_ID"

# ── Step 10: Create rubric version ──────────────────────────
step "Create rubric version"
VERSION_RESP=$(api POST "/api/v1/rubrics/$RUBRIC_ID/versions" -d '{}')
VERSION_ID=$(echo "$VERSION_RESP" | json_val "['publicId']") || fail "Create version failed: $VERSION_RESP"
ok "Version: $VERSION_ID"

# ── Step 11: Create rubric criteria ─────────────────────────
step "Create rubric criteria"

# Criterion 1: Accuracy (critical, weight 0.5)
api POST "/api/v1/rubric-versions/$VERSION_ID/criteria" -d '{
  "metricKey": "accuracy",
  "name": "Factual Accuracy",
  "judgeInstruction": "Check if the answer contains factually correct health information",
  "passCondition": "The answer is factually accurate",
  "failCondition": "The answer contains incorrect health information",
  "weight": 0.5000,
  "scaleMin": 0,
  "scaleMax": 1,
  "isCritical": true
}' > /dev/null
ok "Criterion: accuracy (critical, w=0.5)"

# Criterion 2: Completeness (weight 0.3)
api POST "/api/v1/rubric-versions/$VERSION_ID/criteria" -d '{
  "metricKey": "completeness",
  "name": "Answer Completeness",
  "judgeInstruction": "Check if the answer fully addresses the question with sufficient detail",
  "passCondition": "The answer is complete and covers the key points",
  "failCondition": "The answer is incomplete or missing important details",
  "weight": 0.3000,
  "scaleMin": 0,
  "scaleMax": 1,
  "isCritical": false
}' > /dev/null
ok "Criterion: completeness (w=0.3)"

# Criterion 3: Clarity (weight 0.2)
api POST "/api/v1/rubric-versions/$VERSION_ID/criteria" -d '{
  "metricKey": "clarity",
  "name": "Response Clarity",
  "judgeInstruction": "Check if the answer is clearly written and easy to understand",
  "passCondition": "The answer is clear and well-structured",
  "failCondition": "The answer is confusing or poorly written",
  "weight": 0.2000,
  "scaleMin": 0,
  "scaleMax": 1,
  "isCritical": false
}' > /dev/null
ok "Criterion: clarity (w=0.2)"

# ── Step 12: Publish rubric version ─────────────────────────
step "Publish rubric version"
api PATCH "/api/v1/rubric-versions/$VERSION_ID" -d '{"status": "PUBLISHED"}' > /dev/null
ok "Rubric version published"

# ── Step 13: Create evaluation run ──────────────────────────
step "Create evaluation run"
RUN_RESP=$(api POST "/api/v1/projects/$PROJECT_ID/evaluation-runs" -d "{
  \"datasetPublicId\": \"$DATASET_ID\",
  \"rubricVersionPublicId\": \"$VERSION_ID\",
  \"connectorPublicId\": \"$CONNECTOR_ID\"
}")
RUN_ID=$(echo "$RUN_RESP" | json_val "['publicId']") || fail "Create run failed: $RUN_RESP"
JOB_ID=$(echo "$RUN_RESP" | json_val "['jobPublicId']") || true
ok "Evaluation run: $RUN_ID (job: ${JOB_ID:-unknown})"

# ── Step 14: Poll job until complete ────────────────────────
step "Wait for evaluation job to complete"
MAX_WAIT=300
WAITED=0
while [ $WAITED -lt $MAX_WAIT ]; do
  JOB_RESP=$(api GET "/api/v1/jobs/$JOB_ID")
  JOB_STATUS=$(echo "$JOB_RESP" | json_val "['status']") || true

  if [ "$JOB_STATUS" = "COMPLETED" ]; then
    ok "Job completed!"
    break
  elif [ "$JOB_STATUS" = "FAILED" ]; then
    ERROR=$(echo "$JOB_RESP" | json_val "['errorMessage']") || true
    fail "Job failed: $ERROR"
  fi

  info "Status: $JOB_STATUS (waited ${WAITED}s / ${MAX_WAIT}s)"
  sleep 5
  WAITED=$((WAITED + 5))
done

if [ $WAITED -ge $MAX_WAIT ]; then
  fail "Job timed out after ${MAX_WAIT}s"
fi

# ── Step 15: View results ───────────────────────────────────
step "View evaluation results"
RESULTS_RESP=$(api GET "/api/v1/evaluation-runs/$RUN_ID/results")
TOTAL=$(echo "$RESULTS_RESP" | json_val "['totalElements']") || true
info "Total results: $TOTAL"

echo "$RESULTS_RESP" | python3 -c "
import sys, json
data = json.load(sys.stdin)
for item in data.get('items', data.get('content', [])):
    status = item.get('judgeStatus', '?')
    score = item.get('judgeScore', '?')
    tc = item.get('testCasePublicId', '?')
    print(f'    {status:8s} score={score}  testCase={tc}')
" 2>/dev/null || info "Could not parse results detail"

# ── Step 16: Create export (XLSX) ───────────────────────────
step "Create XLSX export"
EXPORT_RESP=$(api POST "/api/v1/evaluation-runs/$RUN_ID/exports" -d '{"fileType": "EXCEL"}')
EXPORT_ID=$(echo "$EXPORT_RESP" | json_val "['publicId']") || fail "Create export failed: $EXPORT_RESP"
ok "Export: $EXPORT_ID"

# ── Step 17: Poll export until ready ────────────────────────
step "Wait for export to be ready"
MAX_WAIT=120
WAITED=0
while [ $WAITED -lt $MAX_WAIT ]; do
  EXPORT_DETAIL=$(api GET "/api/v1/exports/$EXPORT_ID")
  EXPORT_STATUS=$(echo "$EXPORT_DETAIL" | json_val "['status']") || true

  if [ "$EXPORT_STATUS" = "READY" ]; then
    ok "Export ready!"
    break
  elif [ "$EXPORT_STATUS" = "FAILED" ]; then
    fail "Export failed"
  fi

  info "Status: $EXPORT_STATUS (waited ${WAITED}s)"
  sleep 3
  WAITED=$((WAITED + 3))
done

# ── Step 18: Download XLSX ──────────────────────────────────
step "Download XLSX file"
curl -s -o "$EXPORT_FILE" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  "${BASE_URL}/api/v1/exports/$EXPORT_ID/file"

FILE_SIZE=$(stat -c%s "$EXPORT_FILE" 2>/dev/null || stat -f%z "$EXPORT_FILE" 2>/dev/null)
if [ "$FILE_SIZE" -gt 100 ]; then
  ok "Downloaded: $EXPORT_FILE ($FILE_SIZE bytes)"
else
  fail "Download too small ($FILE_SIZE bytes), likely an error response"
fi

# ── Done ────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  E2E Smoke Test PASSED!${NC}"
echo -e "${GREEN}  XLSX export: $EXPORT_FILE${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
