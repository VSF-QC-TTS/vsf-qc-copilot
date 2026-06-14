# Server API TODO

Date: 2026-06-12

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
- Requirement: create, list, detail, update.
- Dataset/Test Case: dataset create/list/detail/update; test case create/list/update/delete.
- Rubric/Criteria: rubric create/list/detail/update/archive; version create/list/detail/publish/archive; criteria create/list/update/delete.
- Evaluation Run: create (`POST`, 202 async), list under project, detail flat path.
- Evaluation Results: list under run with optional `judgeStatus`/`qcStatus` filters and QC review fields.
- Evaluation Events: list job events under run.
- Job: detail flat path with resolved `resourcePublicId`.
- Worker + Promptfoo mock executor (Step 10).
- Real Promptfoo CLI integration through Redis worker path.
- QC Review: upsert/get review decision by evaluation result; patch by review decision.
- Export: create export job under evaluation run; get export detail; download READY export file; local storage abstraction.
- Promptfoo Secret-Store: `ConnectorSecretService` encrypts/persists connector secrets in `connector_secrets` table with AES-256-GCM; connector test-runs resolve `{{secret:KEY}}` to real values; `PromptfooConfigGenerator` maps secrets to `{{env.VQC_SECRET_*}}`; `CliPromptfooExecutor` passes decrypted secrets as process env vars. Raw secrets never touch config files on disk.
- Promptfoo Rubric Judge Mapping: `RubricAssertionMapper` translates `RubricCriterion` entities into Promptfoo `llm-rubric` assertions with `judgeInstruction`, `passCondition`/`failCondition`, `weight`, and `metricKey`. `CriteriaScoreCalculator` computes weighted `judgeScore` from per-criterion results and applies `isCritical` override (critical fail → `FAIL` status). `PromptfooConfigGenerator` adds `defaultTest.options.provider` (`google:gemini-2.5-flash`) when rubric criteria exist. `PromptfooResultParser` extracts per-criterion results from `componentResults`. Ground-truth `contains` assertions are kept alongside rubric assertions.
- Target connector from cURL: `POST /api/v1/projects/{projectPublicId}/target-api-connectors/from-curl` accepts `{name, rawCurl}` (+ optional description/responseSelector/timeoutSeconds/retryCount), parses cURL into method/URL/headers/body via `CurlParser`, auto-detects and encrypts secrets via `ConnectorSecretDetector`, test-calls the target API, auto-detects response selector via `ResponseSelectorDetector`, and saves only on success. Returns connector detail + test-call result.

## Next

- Choose the next backend slice: connector timeout/retry behavior, export storage provider beyond local filesystem, or dashboard/analytics endpoints.
