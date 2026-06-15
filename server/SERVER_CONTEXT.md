# Server Context

Date: 2026-06-15
Repo area: `server/`
Last full-suite pass: 393 tests, 0 failures (2026-06-15). Focused rubric/evaluation/frontend checks passed on 2026-06-15 after rubric workflow + judge model changes.

Purpose: this is the server bootstrap handoff. If a user only says "read `server/SERVER_CONTEXT.md`", the agent must use this file to discover the next files to read without asking for more pointers. Current code is the source of truth when docs and implementation differ. The full product target lives in `docs/`; treat docs as roadmap/contract intent unless the user explicitly asks to migrate current code toward them.

Reading tags:
- `[START_HERE]`: always read first; includes rules, docs map, and agent startup order.
- `[API_CHANGE]`: read before adding, removing, or changing an API endpoint.
- `[CURRENT_STATE]`: read before touching existing auth/domain behavior.
- `[FUTURE_SLICE]`: read when implementing evaluation, result/review, export, or other roadmap work.
- `[MAIL]`: read only when changing mail or token-email flows.
- `[CONVENTIONS]`: read before adding public controller/service/repository/mapper code.
- `[TESTS]`: read before choosing which server tests to run.

## [START_HERE] Rules

Rules:
- Prefix shell commands with `rtk`.
- Skip Testcontainers-heavy full test runs unless explicitly requested; use focused `-Dtest=...`.
- Keep `server/.env` untracked and never print secret values.
- Do not commit generated runtime files, real secrets, or logs.
- Keep Java classes under `me.nghlong3004.vqc.api` and include the local class JavaDoc header style already used in code.
- Prefer Lombok `@Builder` object construction for domain entities/DTO-like objects when the type supports it; avoid `new` plus setter chains unless the surrounding code or framework requires it.
- When adding, removing, or changing API endpoints, update both `server/API_TODO.md` and `server/API_TREE.md` in the same change.
- If implementation context changes in a way future agents must know, update this `server/SERVER_CONTEXT.md` handoff too.

## [START_HERE] Docs Map

Docs map:
- `server/SERVER_CONTEXT.md`: read first; this file routes the rest of the backend context.
- `server/API_TREE.md`: current API/resource relationship tree and main workflow.
- `server/API_TODO.md`: completed/in-progress/next API slice tracker.
- `server/API_PLAN.md`: concrete implementation plan for the current in-progress step (classes, packages, tests, commits).
- `server/PROMPTFOO_FINDINGS_PLAN.md`: Promptfoo CLI decision record; read before changing Promptfoo, evaluation execution, result parsing, or worker integration.
- `server/Dockerfile`: multi-stage build (Maven → Node/promptfoo → JRE runtime). Build context is project root.
- `server/.env.example`: template for `server/.env`; copy and fill secrets before running.
- `docker-compose.yml`: local dev stack (PostgreSQL 17, Redis 7, API). All ports bound to 127.0.0.1. Uses `server/.env` via `env_file`.
- `docs/backend-codex-implementation-brief.md`: best full backend implementation brief when building new domains.
- `docs/api_contract_mvp_updated.md`: target MVP API contract, but some paths/fields are older than current server.
- `docs/db_schema.md`: target MVP schema and publicId/internal-id rules.
- `docs/dev-setup-updated.md`: repo/runtime rules, profiles, promptfoo, Docker/VPS.
- `docs/01..06*.md`: product, architecture, ADRs, delivery plan, ownership.

## [START_HERE] Agent Startup Order

Agent startup order:
1. Read this `server/SERVER_CONTEXT.md`.
2. Read `server/API_TODO.md` to know completed/in-progress/next slices.
3. Read `server/API_TREE.md` to understand API/resource relationships and the main workflow.
4. Read `server/API_PLAN.md` before planning or implementing the next backend slice.
5. Read specialized files based on scope:
   - Promptfoo/evaluation worker/result parser: `server/PROMPTFOO_FINDINGS_PLAN.md`.
   - New or changed API endpoint: `server/API_TREE.md`, `server/API_TODO.md`, target controller/service/tests, and relevant docs contract if the path is unclear.
   - Persistence/schema change: current Flyway migrations plus `docs/db_schema.md`.
   - Runtime/setup change: `docs/dev-setup-updated.md`.
   - Product-contract question: `docs/api_contract_mvp_updated.md` and relevant `docs/01..06*.md`.
6. Read broader `docs/` files only when the task needs product contract, DB schema, runtime setup, or new-domain implementation detail.
7. After each backend slice, update `SERVER_CONTEXT.md` plus `API_TODO.md`/`API_PLAN.md`/`API_TREE.md` when their tracked facts change, then commit with `type(scope): summary`.

## [CURRENT_STATE] Auth

Current implemented auth state:
- Actual local auth endpoints live under `/api/v1/auth`: `register`, `login`, `refresh-token`, `logout`, `verify-email`, `forgot-password`, `reset-password`.
- This differs from the docs target `/api/v1/sessions`; for now follow existing code unless the user asks to migrate the contract.
- Login returns access token in the JSON body and refresh token only as HttpOnly `refresh_token` cookie.
- `POST /api/v1/auth/refresh-token` reads `refresh_token` cookie, validates a refresh JWT with `token_type=refresh`, returns a new access token, and rotates the refresh cookie.
- `POST /api/v1/auth/logout` clears `refresh_token` with `Max-Age=0`; no token store/revocation exists yet.
- Protected APIs use `Authorization: Bearer ...`; CSRF is disabled.
- Main `JwtDecoder` accepts only JWTs with `token_type=access`; a named `refreshTokenJwtDecoder` accepts only `token_type=refresh`.
- The access `JwtDecoder` is `@Primary`; keep the refresh decoder named/qualified so Spring Security uses access tokens for protected APIs while refresh-token flow uses refresh tokens.
- Register creates `PENDING_EMAIL_VERIFICATION` users and emails `${WEB_BASE_URL}/verify-email?token=...`.
- Forgot password emails `${WEB_BASE_URL}/reset-password?token=...`; the request must not reveal account existence.

## [CURRENT_STATE] OAuth

OAuth state:
- OAuth code exists for Google/GitHub profile extraction and GitHub email enrichment.
- OAuth success handler currently extracts profile/email, sets a temporary refresh cookie, invalidates session, and redirects; user persistence/linking is still commented/incomplete.
- Treat OAuth as partially implemented, not production-complete.

## [CURRENT_STATE] Domain Choices

Domain choices in current code:
- User entity is `me.nghlong3004.vqc.api.user.entity.User`.
- Public API uses `email`; persistence stores it in `users.username`.
- `UserStatus`: `PENDING_EMAIL_VERIFICATION`, `ACTIVE`, `DISABLED`.
- `Role`: `QC_MEMBER`, `QC_LEAD`, `ADMIN`; `Role#getAuthority()` returns `ROLE_` + enum name.
- `User` uses Lombok builder/defaults; register currently builds users with `User.builder()`.
- Existing domain code prefers Lombok builders for object creation where available; keep that style instead of constructing with `new` and then mutating through setters.

## [CURRENT_STATE] Persistence

Persistence now vs target:
- Current Flyway has five migrations: `V1__init_schema.sql` (auth, project, connector, requirement table kept, dataset without requirement FK, test_case, rubric, rubric version content/schema), `V2__create_jobs_evaluation.sql` (jobs, job_events, judge_models, evaluation_runs with judge model FK, evaluation_results), `V3__create_review_decisions.sql`, `V4__create_export_files.sql`, `V5__create_connector_secrets.sql`.
- Dataset has `generation_prompt TEXT` column for AI generation context.
- Rubrics are user-scoped (nullable `project_id`); `is_template BOOLEAN NOT NULL DEFAULT FALSE` flags system templates. Rubric versions store `content` and optional `output_schema_json`.
- Email verification and password reset tokens are opaque raw values; only SHA-256 hashes are stored.
- `OpaqueTokenService` owns raw token generation and hashing for one-time email tokens.
- Future MVP docs expect main tables to use internal `BIGINT id` plus public `UUID public_id`; APIs should expose `publicId`, not internal `id`.

## [API_CHANGE] Implemented API Slices

Implemented API slices after auth:
- `GET /api/v1/users/me`: returns the current authenticated user by principal username/email.
- Projects under `/api/v1/projects`: create, list with optional status/search/page/sort, detail, update, archive. Project access is owner-scoped through `createdBy`; archive sets status `ARCHIVED` and `archivedAt`.
- Public mock chatbot fallback: `POST /mock-chatbot/chat`; it is intentionally public in `SecurityConfig` so connector test-runs can call it without a JWT.
- Target API connectors:
  - Create/list are nested under `/api/v1/projects/{projectPublicId}/target-api-connectors`.
  - Detail/update/test-run use `/api/v1/target-api-connectors/{connectorPublicId}` and `/test-runs`.
  - Connector access is owner-scoped by authenticated username/email.
  - `secretValues` are write-only. Create/update replace raw secret values in headers/body/auth config with placeholders like `{{secret:KEY}}`; responses return masked `secretRefs`, not raw secrets.
  - `ConnectorSecretService` encrypts raw secret values with AES-256-GCM (`AesGcmEncryptor`) and persists them in `connector_secrets` table. Decryption is used at connector test-run time and evaluation time.
  - Test-run renders `{{question}}`, `{{precondition}}`, and `{{metadata}}`, resolves `{{secret:KEY}}` placeholders to real decrypted values before calling the configured API, and returns masked preview headers. Test-runs now work for authed connectors.
  - `RestClient.Builder` is provided by `ApplicationConfig`; `timeoutSeconds` is accepted but not yet wired into a per-request HTTP timeout.
  - **Create from cURL**: `POST /api/v1/projects/{projectPublicId}/target-api-connectors/from-curl` accepts `{name, rawCurl}` (+ optional description/responseSelector/timeoutSeconds/retryCount). `CurlParser` extracts method/URL/headers/body. `ConnectorSecretDetector` auto-detects and masks sensitive headers with `{{secret:KEY}}` placeholders. Backend test-calls the target API; saves only on success (422 if test fails). `ResponseSelectorDetector` auto-guesses the response JSONPath selector from common keys. Minimal input: just `name` + `rawCurl`.
- Requirements: **REMOVED** from the API/domain flow; `me.nghlong3004.vqc.api.requirement` package is gone. The legacy `business_requirements` table is still created, but datasets no longer reference it.
- Datasets and test cases:
  - Dataset create/list are nested under `/api/v1/projects/{projectPublicId}/datasets`.
  - Dataset detail/update use `/api/v1/datasets/{datasetPublicId}`.
  - Test case create/list are nested under `/api/v1/datasets/{datasetPublicId}/test-cases`.
  - Test case update/delete use `/api/v1/test-cases/{testCasePublicId}`.
  - Dataset/test case access is owner-scoped by authenticated username/email through `createdBy`.
  - Datasets support `DRAFT`, `APPROVED`, and `ARCHIVED`; approving requires 1-100 active test cases.
  - Test cases support `ACTIVE` and `INACTIVE`; `DELETE` hard-deletes a test case per current API contract.
  - Archived datasets reject test case create/update/delete.
- Rubrics, rubric versions, and criteria:
  - Rubrics are now **user-scoped** (not project-scoped). `project_id` is nullable; `is_template` flag marks system templates.
  - `POST /api/v1/rubrics` creates a user-scoped rubric plus draft v1 in one transaction. Request includes `name`, optional `description`, required rubric `content`, optional `outputSchemaJson`, and optional criteria. If criteria are omitted, service seeds a small default criteria set.
  - `POST /api/v1/rubrics/generate-preview` calls Spring AI `ChatClient` and returns editable rubric `content`, `outputSchemaJson`, and criteria without persisting. Frontend uses this as the QC-friendly rubric wizard preview step.
  - `POST /api/v1/projects/{projectPublicId}/rubrics` creates a rubric linked to a project (backward compat).
  - `GET /api/v1/projects/{projectPublicId}/rubrics` lists rubrics under a project.
  - `GET /api/v1/rubrics` lists all rubrics owned by the authenticated user (user-scoped).
  - `GET /api/v1/rubrics/templates` lists system-provided rubric templates.
  - `POST /api/v1/rubrics/{rubricPublicId}/clone` creates a copy of a rubric for the user.
  - Rubric detail/update/archive use `/api/v1/rubrics/{rubricPublicId}`; `DELETE` soft-archives with status `ARCHIVED`.
  - `RubricTemplateSeeder` seeds 5 built-in templates on first boot (idempotent).
  - Version create/list are nested under `/api/v1/rubrics/{rubricPublicId}/versions`; create accepts optional `sourceVersionPublicId` to clone content, output schema, and criteria into the new draft version.
  - `GET /api/v1/rubric-versions` lists all user-owned versions, with optional `status`; frontend start-evaluation uses this to pick any reusable published rubric version.
  - Version detail/update use `/api/v1/rubric-versions/{rubricVersionPublicId}`. Draft versions can patch `content` and `outputSchemaJson`; lifecycle status patch still publishes/archives.
  - Criteria create/list are nested under `/api/v1/rubric-versions/{rubricVersionPublicId}/criteria`.
  - Criteria update/delete use `/api/v1/rubric-criteria/{criterionPublicId}`.
  - Rubric access is owner-scoped by authenticated username/email through `createdBy`.
  - Version numbers are server-managed and auto-increment from the latest version.
  - Rubric `currentVersion` starts as `null`; publishing a draft version sets `publishedAt` and the rubric `currentVersion`.
  - Criteria use **relative integer weights** (1–100, default 1). System normalizes at evaluation time via `Σ(w×s)/Σ(w)`.
  - Publishing requires nonblank rubric version `content` and at least one criterion (no sum constraint).
  - Published/archived versions are immutable for criteria create/update/delete; archived rubrics reject version/criteria mutation.
  - `metricKey` is unique per rubric version and must be lowercase letters/numbers/underscores.
- Judge models:
  - Project-scoped judge models live under `/api/v1/projects/{projectPublicId}/judge-models`.
  - `POST /api/v1/projects/{projectPublicId}/judge-models` creates provider/model config and encrypts the raw API key with AES-256-GCM. Responses return only `apiKeyMasked`.
  - `GET /api/v1/projects/{projectPublicId}/judge-models` lists models, with optional `active`.
  - `PATCH /api/v1/judge-models/{judgeModelPublicId}` updates metadata/config and replaces the encrypted API key only when a new key is supplied.
  - `POST /api/v1/judge-models/{judgeModelPublicId}/test-connection` currently validates ownership/active status and returns the masked config; it does not call external providers yet.
- Evaluation runs and jobs:
  - `POST /api/v1/projects/{projectPublicId}/evaluation-runs` creates an evaluation run + job, validates that the referenced dataset is `APPROVED`, rubric version is `PUBLISHED`, connector is active, and judge model is active under the same project, then pushes a job message to a Redis queue and returns `202 Accepted` with `runPublicId` and `jobPublicId`.
  - `GET /api/v1/projects/{projectPublicId}/evaluation-runs` lists runs under a project with pagination.
  - `GET /api/v1/evaluation-runs/{runPublicId}` returns run detail (flat path, owner-scoped).
  - `GET /api/v1/evaluation-runs/{runPublicId}/results` lists evaluation results with optional `judgeStatus` and `qcStatus` filters, pagination, test-case review context, and QC fields (`qcStatus`, `qcNote`, `picBug`).
  - `GET /api/v1/evaluation-runs/{runPublicId}/events` lists job events in chronological order.
  - `GET /api/v1/jobs/{jobPublicId}` returns job detail with resolved `resourcePublicId` (flat path, owner-scoped).
  - All evaluation/job endpoints are owner-scoped through the project `createdBy` chain.
  - `EvaluationRunStatus`: `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`. `JobStatus`: `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`. `JudgeStatus`: `PASS`, `FAIL`, `WARNING`, `ERROR`.
  - Redis is used as a job queue (`vqc:jobs:queue` list). `JobQueuePublisher` pushes job public IDs only after the surrounding transaction commits when transaction synchronization is active, preventing `JobWorker` from consuming a job before the database row is visible; `JobWorker` consumes messages when `vqc.worker.enabled=true`.
  - `PromptfooExecutor` is Strategy-based: `MockPromptfooExecutor` for `vqc.promptfoo.mode=mock` and `CliPromptfooExecutor` for `vqc.promptfoo.mode=cli`.
  - CLI mode runs only inside the Redis-backed worker path after `JobWorker` consumes a queued evaluation job; HTTP request handlers still only enqueue jobs and never run promptfoo directly.
  - CLI mode writes isolated run artifacts under `vqc.promptfoo.work-dir/{runPublicId}`: `promptfooconfig.json`, `tests.json`, `results.json`, validation/eval stdout/stderr logs, `.promptfoo/`, and `logs/`.
  - `PromptfooCommandExecutor` validates config before eval with `validate config --config`, then runs `eval` with `--no-progress-bar`, `--no-table`, `--no-cache`, `--max-concurrency`, and per-run `PROMPTFOO_CONFIG_DIR`, `PROMPTFOO_LOG_DIR`, `FORCE_COLOR=0`, `PROMPTFOO_MAX_EVAL_TIME_MS`, and `PROMPTFOO_EVAL_TIMEOUT_MS`.
  - CLI exit code `0` is success; exit code `100` is accepted as completed only when `results.json` exists. Validation failure, missing results, timeout, command start failure, malformed JSON, and other non-zero exits fail the job.
  - CLI output parser supports `results.results[]` from `promptfoo@0.121.15` and keeps compatibility with `results.outputs[]`, mapping rows into `PromptfooResult`.
  - CLI config supports response selectors `$.answer` and `$.data.answer` only; unsupported selectors fail fast.
  - CLI config resolves `{{secret:KEY}}` placeholders to `{{env.VQC_SECRET_KEY}}` in generated promptfoo config. `CliPromptfooExecutor` decrypts secrets from the `connector_secrets` table via `ConnectorSecretService` and passes them as `VQC_SECRET_*` environment variables to the promptfoo CLI process. Raw secrets never touch disk.
  - CLI judge provider comes from `EvaluationRun.judgeModel`: `GEMINI` → `google:<model>`, `OPENAI` → `openai:<model>`, `ANTHROPIC` → `anthropic:<model>`, `DEEPSEEK`/`CUSTOM` → OpenAI-compatible provider with optional `OPENAI_BASE_URL`. Decrypted judge API keys are passed per run via provider-specific env vars.
  - Rubric criteria still generate structured `llm-rubric` assertions. If criteria are empty but `RubricVersion.content` exists, config generation creates one holistic rubric assertion from that content.
  - Promptfoo local runner metadata and lockfile exist under `tooling/promptfoo-runner` for `promptfoo@0.121.15`; install `node_modules` locally before real CLI/smoke runs.
  - `EvaluationJobHandler` loads active dataset test cases, runs the executor, writes one `EvaluationResult` per active case, updates run/job counters and statuses, and emits `RUNNING`, `CASE_COMPLETED`, `COMPLETED`, or `FAILED` job events.
- QC review:
  - `PUT /api/v1/evaluation-results/{resultPublicId}/review-decision` upserts one review decision per evaluation result.
  - `GET /api/v1/evaluation-results/{resultPublicId}/review-decision` returns the persisted review decision or a default `NOT_REVIEWED` response when absent.
  - `PATCH /api/v1/review-decisions/{reviewDecisionPublicId}` updates an existing review decision.
  - `QcStatus`: `NOT_REVIEWED`, `PASS`, `FAIL`, `NEED_FIX`, `IGNORED`; write APIs accept only writable statuses (`PASS`, `FAIL`, `NEED_FIX`, `IGNORED`).
  - `NOT_REVIEWED` is derived when no `review_decisions` row exists; no default rows are created.
  - `picBug` is stored as an active user reference (`pic_bug_user_id`) and request payload uses `picBugUserPublicId`.
- Export:
  - `POST /api/v1/evaluation-runs/{runPublicId}/exports` creates `export_files` metadata and an async job (`EXPORT_EXCEL` or `EXPORT_JSON`) with `resourceType=EXPORT_FILE`, pushes the job public ID to Redis, and returns `202 Accepted`.
  - `GET /api/v1/exports/{exportPublicId}` returns owner-scoped export metadata and includes `downloadUrl` only when status is `READY`.
  - `GET /api/v1/exports/{exportPublicId}/file` downloads a READY export file and rejects pending/failed exports with `EXPORT_FILE_NOT_READY`.
  - `JobWorker` routes `EXPORT_EXCEL` and `EXPORT_JSON` jobs to `ExportJobHandler`.
  - Export generation uses an `ExportGenerator` Strategy (`ExcelExportGenerator`, `JsonExportGenerator`) that returns bytes and content type; generators do not choose the storage backend.
  - Export storage uses `ObjectStorageService`; current implementation is local filesystem via `vqc.storage.type=local` and `vqc.storage.local.base-dir`.
  - `export_files` stores storage metadata (`storageProvider`, `storageKey`, `contentType`, `sizeBytes`) instead of a local-only file path.
  - Excel generation uses Apache POI `poi-ooxml` and JSON generation uses Jackson; optional/missing export fields are written blank/default rather than failing.
- Bulk import test cases:
  - `POST /api/v1/datasets/{datasetPublicId}/test-cases/import` accepts `.xlsx` or `.csv` uploads.
  - Returns `ImportTestCaseResponse` with `totalRows`, `importedCount`, `skippedCount`, and per-row `errors[]`.
  - Validates file size (≤5MB), format, and 100-case limit per dataset. Dataset must be `DRAFT`.
- AI generate dataset:
  - `POST /api/v1/datasets/{datasetPublicId}/generate` queues a `DATASET_GENERATION` job (202 Accepted).
  - `DatasetGenerationJobHandler` calls Gemini AI via Spring AI `ChatClient` to generate test cases.
  - Validates dataset is `DRAFT` and test case count + requested count ≤ 100.
  - `GenerateDatasetRequest` has `prompt` (generation context), `count` (5–100), and optional `additionalPrompt`.
- Quick evaluate:
  - `POST /api/v1/projects/{projectPublicId}/quick-evaluate` starts an evaluation with auto-resolve.
  - Null `datasetPublicId`, `connectorPublicId`, `rubricVersionPublicId`, or `judgeModelPublicId` are resolved to the sole candidate; 422 if 0 or >1.
  - Delegates to `createEvaluationRun` after resolution.

## [FUTURE_SLICE] Known Current Gaps

Known current gaps:
- Promptfoo CLI selector support is limited to `$.answer` and `$.data.answer`.
- OAuth persistence/linking remains incomplete.
- Connector response extraction only supports the current simple selector path used by tests.
- S3/R2/MinIO export storage providers are future work; the storage interface is in place and local is the only current provider.

Following backend slices:
- Connector runtime hardening: align connector timeout/retry settings with real outbound calls and Promptfoo execution.
- Export storage provider: add S3/R2/MinIO behind the existing `ObjectStorageService`.

## [FUTURE_SLICE] Product Direction

Future product direction from docs:
- MVP flow: Login -> Project -> Dynamic Target API Connector -> Dataset/Test Cases -> Rubric/Criteria -> Project Judge Model -> Evaluation Run/Job -> Results -> QC Review -> Export.
- Evaluation Run/Job, Worker, Results, QC Review, and Export APIs are implemented (see `[API_CHANGE]`).
- Use `target_api_connectors` / API path `target-api-connectors`, not the older ambiguous `api_connectors`, when implementing connector work.
- Dynamic connector must support manual config first: method/url/headers/body template/response selector, non-streaming JSON, timeout/retry, and secret placeholders.
- Do not log or return raw connector secrets. Store headers/body/auth with placeholders like `{{secret:KEY}}`; return masked values only.
- Long-running evaluation/export work must return `202` and run async via Redis/job/worker; do not run promptfoo inside the HTTP request.
- Promptfoo is a CLI engine. Local command should be project-local/pinned at `tooling/promptfoo-runner/node_modules/.bin/promptfoo`; Node should satisfy Promptfoo requirements, preferably Node 22+ for demo. Do not use global `promptfoo` or `promptfoo@latest` for repeatable demo jobs.
- Demo target is under 100 active test cases, recommended 30-80.
- Mock chatbot API is a required fallback so the demo is not blocked by missing internal APIs.
- Export should use flexible mapping: fill known fields, leave missing/unmapped fields blank, do not fail because optional fields are missing.

## [MAIL] Mail

Mail:
- Mail config maps `.env` keys `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`.
- Mail flow uses Strategy + Factory: `MailType`, `MailRequest`, `MailMessage`, `MailStrategy`, `MailStrategyFactory`.
- Templates are HTML emails with table-friendly structure and inline CSS.

## [CONVENTIONS] Workflow

Development workflow:
- Every step follows: Code → Write tests → Run tests → FAIL? fix and re-run : PASS? `git add . && git commit -m "type(scope): summary"` → next step.
- Never move to the next step until current step's tests pass and changes are committed.
- Commit messages follow conventional format: `feat(scope)`, `fix(scope)`, `refactor(scope)`, `docs(scope)`, `chore(scope)`.
- After each backend slice, update `SERVER_CONTEXT.md`, `API_TODO.md`, `API_TREE.md`, and `API_PLAN.md` when their tracked facts change.

## [CONVENTIONS] API Conventions

API design (Microsoft RESTful Guidelines):
- Use plural nouns for collections: `/rubrics`, `/test-cases`, `/datasets`.
- HTTP methods: GET (read), POST (create/action), PATCH (partial update), DELETE (remove/archive).
- Actions on resources use verb sub-path: `POST /datasets/{id}/generate`, `POST /rubrics/{id}/clone`.
- Collection responses: `{ items, page, size, totalItems, totalPages }`.
- Async long-running operations return `202 Accepted` + `jobPublicId`; poll via `GET /jobs/{jobPublicId}`.
- Errors use Problem Details shape (RFC 9457): `type`, `title`, `status`, `detail`, `instance`, plus `code` and optional `errors[]`.
- Status codes: 200 success, 201 created, 202 accepted, 204 no content, 400 bad request, 404 not found, 409 conflict, 422 business validation.

DTO validation convention:
- Request DTO validation annotations must include explicit `message = "..."` on every constraint (`@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`, `@Pattern`, `@DecimalMin`, `@DecimalMax`).
- Every request DTO field should have `@Schema(description = "...", example = "...")` for Swagger/OpenAPI docs. Mark optional fields with `nullable = true`.
- Request DTOs should be `record` types.
- Public controllers should expose Swagger/OpenAPI request, success response, and error response examples.

Code conventions:
- Public service/repository/mapper interfaces should have JavaDoc with `@param` and `@return` where useful, and link domain types with `{@link ...}`.
- Prefer feature-first packages: `<feature>/controller`, `service`, `service/impl`, `repository`, `entity`, `mapper`, `request`, `response`.
- Prefer Lombok `@Builder` for creating entities/responses/value objects that support it; use setters only for JPA/framework requirements, partial updates, or when an existing local pattern already uses setters.

## [TESTS] Focused Tests

Focused tests:
- Full server suite passed on 2026-06-15 after QC Productivity Features:
  `rtk bash mvnw test` -> 393 tests, 0 failures/errors.
- `ServerApplicationTests` injects dummy test properties for JWT/OAuth/Gemini/base URLs because the full context uses `dev` profile but does not load `server/.env`.
- Existing safe server suite:
  `rtk bash mvnw -Dtest=RoleTest,UserMapperTest,UserServiceImplTest,HtmlMailTemplateRendererTest,EmailVerificationMailStrategyTest,PasswordResetMailStrategyTest,ErrorResponseTest,AuthServiceImplTest,EmailVerificationServiceImplTest,PasswordResetServiceImplTest,AuthControllerTest,JwtTokenServiceImplTest,RefreshTokenCookieFactoryTest test`
- OAuth focused suite:
  `rtk bash mvnw -Dtest=AuthProviderTest,OAuth2UserProfileExtractorTest,OAuth2UserProfileServiceTest,ProviderAwareOAuth2UserServiceTest,GithubEmailOAuth2UserEnricherTest,OAuth2LoginSuccessHandlerTest test`
- Project/connector/mock focused suite:
  `rtk bash mvnw -Dtest=ProjectControllerTest,ProjectServiceImplTest,ProjectMapperTest,MockChatbotControllerTest,MockChatbotServiceImplTest,TargetApiConnectorControllerTest,TargetApiConnectorServiceImplTest,TargetApiConnectorMapperTest,CurlParserTest,ConnectorSecretDetectorTest,ResponseSelectorDetectorTest test`
- Requirement focused suite: **REMOVED** (module deleted).
- Dataset/test case focused suite:
  `rtk bash mvnw -Dtest=DatasetControllerTest,DatasetServiceImplTest,DatasetMapperTest,TestCaseControllerTest,TestCaseServiceImplTest,TestCaseMapperTest test`
- Rubric focused suite:
  `rtk bash mvnw -Dtest=RubricControllerTest,RubricServiceImplTest,RubricMapperTest,RubricVersionControllerTest,RubricVersionServiceImplTest,RubricCriterionControllerTest,RubricCriterionServiceImplTest test`
- Evaluation run/job focused suite:
  `rtk bash mvnw -Dtest=EvaluationRunControllerTest,EvaluationRunServiceImplTest,JobControllerTest,JobServiceImplTest test`
- Promptfoo/job focused suite:
  `rtk bash mvnw -Dtest=CliPromptfooExecutorTest,MockPromptfooExecutorTest,EvaluationJobHandlerTest,JobWorkerTest test`
- Connector secret/crypto focused suite:
  `rtk bash mvnw -Dtest=ConnectorSecretServiceImplTest,AesGcmEncryptorTest,CliPromptfooExecutorTest,TargetApiConnectorServiceImplTest test`
- Real Promptfoo worker smoke test (requires local runner installed and local socket permission):
  `rtk bash mvnw -Dtest=PromptfooWorkerSmokeTest -Dvqc.promptfoo.smoke=true test`
- Export/job focused suite:
  `rtk bash mvnw -Dtest=ExportControllerTest,ExportServiceImplTest,ExportJobHandlerTest,LocalObjectStorageServiceTest,JobWorkerTest,JobServiceImplTest test`
- Public controller tests should cover HTTP status, JSON body, Problem Details validation errors, cookies/headers, and service delegation.
