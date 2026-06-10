# Codex Context

Date: 2026-06-10
Repo area: `server/`

Purpose: this is the short server handoff. Use it before reading broader docs. Current code is the source of truth when docs and implementation differ. The full product target lives in `docs/`; treat docs as roadmap/contract intent unless the user explicitly asks to migrate current code toward them.

Rules:
- Prefix shell commands with `rtk`.
- Skip Testcontainers-heavy full test runs unless explicitly requested; use focused `-Dtest=...`.
- Keep `server/.env` untracked and never print secret values.
- Do not commit generated runtime files, real secrets, or logs.
- Keep Java classes under `me.nghlong3004.vqc.api` and include the local class JavaDoc header style already used in code.

Docs map:
- `docs/backend-codex-implementation-brief.md`: best full backend implementation brief when building new domains.
- `docs/api_contract_mvp_updated.md`: target MVP API contract, but some paths/fields are older than current server.
- `docs/db_schema.md`: target MVP schema and publicId/internal-id rules.
- `docs/dev-setup-updated.md`: repo/runtime rules, profiles, promptfoo, Docker/VPS.
- `docs/01..06*.md`: product, architecture, ADRs, delivery plan, ownership.

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

OAuth state:
- OAuth code exists for Google/GitHub profile extraction and GitHub email enrichment.
- OAuth success handler currently extracts profile/email, sets a temporary refresh cookie, invalidates session, and redirects; user persistence/linking is still commented/incomplete.
- Treat OAuth as partially implemented, not production-complete.

Domain choices in current code:
- User entity is `me.nghlong3004.vqc.api.user.entity.User`.
- Public API uses `email`; persistence stores it in `users.username`.
- `UserStatus`: `PENDING_EMAIL_VERIFICATION`, `ACTIVE`, `DISABLED`.
- `Role`: `QC_MEMBER`, `QC_LEAD`, `ADMIN`; `Role#getAuthority()` returns `ROLE_` + enum name.
- `User` uses Lombok builder/defaults; register currently builds users with `User.builder()`.

Persistence now vs target:
- Current Flyway is intentionally squashed into one initial migration: `V1__init_schema.sql`.
- The old incremental migrations (`V1__enable_extensions.sql` through `V5__create_business_requirements.sql`) were merged because product/Flyway has not been run yet and the current database is empty.
- `V1__init_schema.sql` creates `pgcrypto`, `users`, `email_verification_tokens`, `password_reset_tokens`, `projects`, `target_api_connectors`, `business_requirements`, `datasets`, `test_cases`, `rubrics`, `rubric_versions`, and `rubric_criteria`.
- Email verification and password reset tokens are opaque raw values; only SHA-256 hashes are stored.
- `OpaqueTokenService` owns raw token generation and hashing for one-time email tokens.
- Future MVP docs expect main tables to use internal `BIGINT id` plus public `UUID public_id`; APIs should expose `publicId`, not internal `id`.

Implemented API slices after auth:
- `GET /api/v1/users/me`: returns the current authenticated user by principal username/email.
- Projects under `/api/v1/projects`: create, list with optional status/search/page/sort, detail, update, archive. Project access is owner-scoped through `createdBy`; archive sets status `ARCHIVED` and `archivedAt`.
- Public mock chatbot fallback: `POST /mock-chatbot/chat`; it is intentionally public in `SecurityConfig` so connector test-runs can call it without a JWT.
- Target API connectors:
  - Create/list are nested under `/api/v1/projects/{projectPublicId}/target-api-connectors`.
  - Detail/update/test-run use `/api/v1/target-api-connectors/{connectorPublicId}` and `/test-runs`.
  - Connector access is owner-scoped by authenticated username/email.
  - `secretValues` are write-only. Create/update replace raw secret values in headers/body/auth config with placeholders like `{{secret:KEY}}`; responses return masked `secretRefs`, not raw secrets.
  - Test-run renders `{{question}}`, `{{precondition}}`, and `{{metadata}}`, calls the configured API via `TargetConnectorClient`, and currently extracts `$.answer` only.
  - `RestClient.Builder` is provided by `ApplicationConfig`; `timeoutSeconds` is accepted but not yet wired into a per-request HTTP timeout.
- Requirements:
  - Create/list are nested under `/api/v1/projects/{projectPublicId}/requirements`.
  - Detail/update use `/api/v1/requirements/{requirementPublicId}`.
  - Requirement access is owner-scoped by authenticated username/email through project `createdBy`.
  - `content` is stored trimmed, `version` starts at `1`, and `PATCH` increments `version` only when content changes.
  - `status` supports `ACTIVE` and `ARCHIVED`; use `PATCH status=ARCHIVED` instead of a separate archive endpoint for now.
  - `RequirementServiceImpl` logs each API operation with public IDs/user IDs and never logs raw requirement content.
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
  - Rubric create/list are nested under `/api/v1/projects/{projectPublicId}/rubrics`.
  - Rubric detail/update/archive use `/api/v1/rubrics/{rubricPublicId}`; `DELETE` soft-archives with status `ARCHIVED`.
  - Version create/list are nested under `/api/v1/rubrics/{rubricPublicId}/versions`.
  - Version detail/update use `/api/v1/rubric-versions/{rubricVersionPublicId}`.
  - Criteria create/list are nested under `/api/v1/rubric-versions/{rubricVersionPublicId}/criteria`.
  - Criteria update/delete use `/api/v1/rubric-criteria/{criterionPublicId}`.
  - Rubric access is owner-scoped by authenticated username/email through `createdBy`.
  - Version numbers are server-managed and auto-increment from the latest version.
  - Rubric `currentVersion` starts as `null`; publishing a draft version sets `publishedAt` and the rubric `currentVersion`.
  - Publishing requires at least one criterion and total criterion weight exactly `1.0000`.
  - Published/archived versions are immutable for criteria create/update/delete; archived rubrics reject version/criteria mutation.
  - `metricKey` is unique per rubric version and must be lowercase letters/numbers/underscores.

Known current gaps:
- Connector secrets are not persisted in a real encrypted secret store yet; placeholder resolution for real outbound auth secrets is future work.
- OAuth persistence/linking remains incomplete.
- Connector response extraction only supports the current simple selector path used by tests.
- Long-running evaluation run/job, result/review, and export APIs are still future slices.

Future product direction from docs:
- MVP flow: Login -> Project -> Dynamic Target API Connector -> Requirement -> Dataset/Test Cases -> Rubric/Criteria -> Evaluation Run/Job -> Results -> QC Review -> Export.
- Use `target_api_connectors` / API path `target-api-connectors`, not the older ambiguous `api_connectors`, when implementing connector work.
- Dynamic connector must support manual config first: method/url/headers/body template/response selector, non-streaming JSON, timeout/retry, and secret placeholders.
- Do not log or return raw connector secrets. Store headers/body/auth with placeholders like `{{secret:KEY}}`; return masked values only.
- Long-running evaluation/export work must return `202` and run async via Redis/job/worker; do not run promptfoo inside the HTTP request.
- Promptfoo is a CLI engine. Local command should be project-local/pinned; Docker/VPS image should contain Node + pinned promptfoo. Do not use `promptfoo@latest` for repeatable demo jobs.
- Demo target is under 100 active test cases, recommended 30-80.
- Mock chatbot API is a required fallback so the demo is not blocked by missing internal APIs.
- Export should use flexible mapping: fill known fields, leave missing/unmapped fields blank, do not fail because optional fields are missing.

Mail:
- Mail config maps `.env` keys `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`.
- Mail flow uses Strategy + Factory: `MailType`, `MailRequest`, `MailMessage`, `MailStrategy`, `MailStrategyFactory`.
- Templates are HTML emails with table-friendly structure and inline CSS.

API conventions:
- Errors use Problem Details shape: `type`, `title`, `status`, `detail`, `instance`, plus `code` and optional `errors`.
- Request DTO validation annotations should include explicit messages.
- Public controllers should expose Swagger/OpenAPI request, success response, and error response examples.
- Public service/repository/mapper interfaces should have JavaDoc with `@param` and `@return` where useful, and link domain types with `{@link ...}`.
- Prefer feature-first packages: `<feature>/controller`, `service`, `service/impl`, `repository`, `entity`, `mapper`, `request`, `response`.

Focused tests:
- Full server suite passed on 2026-06-10 after Requirement API:
  `rtk bash mvnw test` -> 109 tests, 0 failures/errors.
- `ServerApplicationTests` injects dummy test properties for JWT/OAuth/Gemini/base URLs because the full context uses `dev` profile but does not load `server/.env`.
- Existing safe server suite:
  `rtk bash mvnw -Dtest=RoleTest,UserMapperTest,UserServiceImplTest,HtmlMailTemplateRendererTest,EmailVerificationMailStrategyTest,PasswordResetMailStrategyTest,ErrorResponseTest,AuthServiceImplTest,EmailVerificationServiceImplTest,PasswordResetServiceImplTest,AuthControllerTest,JwtTokenServiceImplTest,RefreshTokenCookieFactoryTest test`
- OAuth focused suite:
  `rtk bash mvnw -Dtest=AuthProviderTest,OAuth2UserProfileExtractorTest,OAuth2UserProfileServiceTest,ProviderAwareOAuth2UserServiceTest,GithubEmailOAuth2UserEnricherTest,OAuth2LoginSuccessHandlerTest test`
- Project/connector/mock focused suite:
  `rtk bash mvnw -Dtest=ProjectControllerTest,ProjectServiceImplTest,ProjectMapperTest,MockChatbotControllerTest,MockChatbotServiceImplTest,TargetApiConnectorControllerTest,TargetApiConnectorServiceImplTest,TargetApiConnectorMapperTest test`
- Requirement focused suite:
  `rtk bash mvnw -Dtest=RequirementControllerTest,RequirementServiceImplTest,RequirementMapperTest test`
- Dataset/test case focused suite:
  `rtk bash mvnw -Dtest=DatasetControllerTest,DatasetServiceImplTest,DatasetMapperTest,TestCaseControllerTest,TestCaseServiceImplTest,TestCaseMapperTest test`
- Rubric focused suite:
  `rtk bash mvnw -Dtest=RubricControllerTest,RubricServiceImplTest,RubricMapperTest,RubricVersionControllerTest,RubricVersionServiceImplTest,RubricCriterionControllerTest,RubricCriterionServiceImplTest test`
- Public controller tests should cover HTTP status, JSON body, Problem Details validation errors, cookies/headers, and service delegation.
