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
- Evaluation Event Stream: `GET /api/v1/evaluation-runs/{runPublicId}/events/stream` streams job events as `text/event-stream` so the run detail UI can update without waiting for polling.
- Job: detail flat path with resolved `resourcePublicId`.
- Worker + Promptfoo mock executor (Step 10).
- Real Promptfoo CLI integration through Redis worker path.
- QC Review: upsert/get review decision by evaluation result; patch by review decision.
- Export: create export job under evaluation run; get export detail; download READY export file; local storage abstraction.
- Promptfoo Secret-Store: `ConnectorSecretService` encrypts/persists connector secrets in `connector_secrets` table with AES-256-GCM; connector test-runs resolve `{{secret:KEY}}` to real values; `PromptfooConfigGenerator` maps secrets to `{{env.VQC_SECRET_*}}`; `CliPromptfooExecutor` passes decrypted secrets as process env vars. Raw secrets never touch config files on disk.
- Promptfoo Rubric Judge Mapping: `RubricAssertionMapper` translates each `RubricCriterion` into one Promptfoo `llm-rubric` assertion with shared `RubricVersion.content`, testcase `question`, `groundTruth`, `precondition`, and `metadata` as judge context. `CriteriaScoreCalculator` computes weighted `judgeScore` only from known rubric metrics; any failed criterion forces overall `FAIL`, all known criteria passing forces `PASS`. `PromptfooConfigGenerator` uses ground truth as semantic judge context and only falls back to `contains` when no rubric assertion exists. `PromptfooResultParser` extracts per-criterion results from `componentResults`.
- Evaluation QC Result Mapping: Promptfoo grader/provider errors in component assertions now map to `JudgeStatus.ERROR` instead of `FAIL`. Result list responses include structured `criteriaResults` with metric key, criterion name, status, score, reason, and graderError so QC can quickly see why each criterion passed, failed, or errored.
- Evaluation Worker Progress: `EvaluationJobHandler` no longer holds one long transaction around Promptfoo CLI execution. Job status and events commit before/during CLI stages (`RUNNING`, `LOADING_TEST_CASES`, `RUNNING_PROMPTFOO`, `PARSING_RESULTS`, `PERSISTING_RESULTS`, `COMPLETED`/`FAILED`) so polling/SSE consumers can observe progress instead of staying at 0%.
- Target connector from cURL: `POST /api/v1/projects/{projectPublicId}/target-api-connectors/from-curl` accepts `{name, rawCurl}` (+ optional description/responseSelector/timeoutSeconds/retryCount), parses cURL into method/URL/headers/body via `CurlParser`, auto-detects and encrypts secrets via `ConnectorSecretDetector`, test-calls the target API, auto-detects response selector via `ResponseSelectorDetector`, and saves only on success. Returns connector detail + test-call result.
- Evaluation Judge Model selection: evaluation runs now reference a project judge model. `CliPromptfooExecutor` maps selected provider/model to Promptfoo provider strings and injects decrypted judge API keys per run (`GEMINI_API_KEY`, `OPENAI_API_KEY`, `ANTHROPIC_API_KEY`, plus `OPENAI_BASE_URL` for OpenAI-compatible custom providers). Rubric version `content` can drive a single holistic `llm-rubric` assertion when no structured criteria exist.
- Bulk import test cases: `POST /api/v1/datasets/{datasetPublicId}/test-cases/import` accepts `.xlsx` or `.csv` and returns `ImportTestCaseResponse`.
- AI generate dataset: `POST /api/v1/datasets/{datasetPublicId}/generate` queues a `DATASET_GENERATION` job to generate test cases via Gemini.
- Rubric Decoupling from Project: Rubrics are now user-scoped, schema updated, template seeder added, clone endpoint added.
- Quick evaluate: `POST /api/v1/projects/{projectPublicId}/quick-evaluate` auto-resolves dataset/connector/rubric/judge and starts an evaluation run.
- Promptfoo Red-team: `POST /api/v1/projects/{projectPublicId}/red-team-runs` queues a local Promptfoo CLI red-team run without Promptfoo Cloud/UI login. Backend persists `red_team_runs`, routes `RED_TEAM_RUN` jobs, generates red-team tests, evaluates them against a target connector, stores artifacts under the Promptfoo work dir, and exposes list/detail/results APIs.

## Next

- Choose the next backend slice: frontend red-team/QC UX, connector timeout/retry behavior, export storage provider beyond local filesystem, or dashboard/analytics endpoints.
