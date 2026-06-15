# Server API TODO

Date: 2026-06-15

Purpose: track API slices while keeping `SERVER_CONTEXT.md` short.

Update rule:
- Update this file after each API endpoint commit.
- If an endpoint/path/method/resource relationship changes, update `server/API_TREE.md` in the same change.
- If the change creates important implementation context for future agents, update `server/SERVER_CONTEXT.md` too.

## Completed

- Auth: register, login, refresh-token, logout, verify-email, forgot-password, reset-password.
- User: current user (`GET /api/v1/users/me`).
- Project: create, list, detail, update, archive.
- Mock chatbot: public chat fallback (`POST /mock-chatbot/chat`).
- Target API connector: create, list, detail, update, test-run.
- Requirement: ~~removed~~ from API/domain flow; legacy table remains but datasets no longer reference it.
- Dataset/Test Case: dataset create/list/detail/update; test case create/list/update/delete.
- Rubric/Criteria: user-scoped rubric create/list/detail/update/archive; create rubric + draft v1 in one call; template list/clone; version create/list/detail/publish/archive; version create can optionally clone content/schema/criteria from sourceVersionPublicId; criteria create/list/update/delete.
- AI Rubric Preview: `POST /api/v1/rubrics/generate-preview` uses Spring AI `ChatClient` to return editable rubric content/schema/criteria without persisting.
- Judge Model: project-scoped create/list/update/test-connection with encrypted API key storage and masked responses.
- Evaluation Run: create (`POST`, 202 async) with explicit judge model, list under project, detail flat path.
- Evaluation Results: list under run with optional `judgeStatus`/`qcStatus` filters and QC review fields.
- Evaluation Events: list job events under run.
- Job: detail flat path with resolved `resourcePublicId`.
- Worker + Promptfoo mock executor (Step 10).
- Real Promptfoo CLI integration through Redis worker path.
- QC Review: upsert/get review decision by evaluation result; patch by review decision.
- Export: create export job under evaluation run; get export detail; download READY export file; local storage abstraction.
- Promptfoo Secret-Store: `ConnectorSecretService` encrypts/persists connector secrets in `connector_secrets` table with AES-256-GCM; connector test-runs resolve `{{secret:KEY}}` to real values; `PromptfooConfigGenerator` maps secrets to `{{env.VQC_SECRET_*}}`; `CliPromptfooExecutor` passes decrypted secrets as process env vars. Raw secrets never touch config files on disk.
- Promptfoo Rubric Judge Mapping: `RubricAssertionMapper` translates each `RubricCriterion` into one Promptfoo `llm-rubric` assertion with shared `RubricVersion.content`, testcase `question`, `groundTruth`, `precondition`, and `metadata` as judge context. `CriteriaScoreCalculator` computes weighted `judgeScore` only from known rubric metrics; any failed criterion forces overall `FAIL`, all known criteria passing forces `PASS`. `PromptfooConfigGenerator` uses ground truth as semantic judge context and only falls back to `contains` when no rubric assertion exists. `PromptfooResultParser` extracts per-criterion results from `componentResults`.
- Target connector from cURL: `POST /api/v1/projects/{projectPublicId}/target-api-connectors/from-curl` accepts `{name, rawCurl}` (+ optional description/responseSelector/timeoutSeconds/retryCount), parses cURL into method/URL/headers/body via `CurlParser`, auto-detects and encrypts secrets via `ConnectorSecretDetector`, test-calls the target API, auto-detects response selector via `ResponseSelectorDetector`, and saves only on success. Returns connector detail + test-call result.
- Evaluation Judge Model selection: evaluation runs now reference a project judge model. `CliPromptfooExecutor` maps selected provider/model to Promptfoo provider strings and injects decrypted judge API keys per run (`GEMINI_API_KEY`, `OPENAI_API_KEY`, `ANTHROPIC_API_KEY`, plus `OPENAI_BASE_URL` for OpenAI-compatible custom providers). Rubric version `content` can drive a single holistic `llm-rubric` assertion when no structured criteria exist.

## Next

- Choose the next backend slice: connector timeout/retry behavior, export storage provider beyond local filesystem, or dashboard/analytics endpoints.
