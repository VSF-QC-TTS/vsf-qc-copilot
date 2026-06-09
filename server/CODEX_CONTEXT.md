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
- Current Flyway has `V1__enable_extensions.sql` and `V2__create_users.sql`.
- `V2__create_users.sql` includes `users`, `email_verification_tokens`, and `password_reset_tokens` because migrations have not been run yet.
- Email verification and password reset tokens are opaque raw values; only SHA-256 hashes are stored.
- `OpaqueTokenService` owns raw token generation and hashing for one-time email tokens.
- Future MVP docs expect main tables to use internal `BIGINT id` plus public `UUID public_id`; APIs should expose `publicId`, not internal `id`.

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
- Existing safe server suite:
  `rtk bash mvnw -Dtest=RoleTest,UserMapperTest,UserServiceImplTest,HtmlMailTemplateRendererTest,EmailVerificationMailStrategyTest,PasswordResetMailStrategyTest,ErrorResponseTest,AuthServiceImplTest,EmailVerificationServiceImplTest,PasswordResetServiceImplTest,AuthControllerTest,JwtTokenServiceImplTest,RefreshTokenCookieFactoryTest test`
- OAuth focused suite:
  `rtk bash mvnw -Dtest=AuthProviderTest,OAuth2UserProfileExtractorTest,OAuth2UserProfileServiceTest,ProviderAwareOAuth2UserServiceTest,GithubEmailOAuth2UserEnricherTest,OAuth2LoginSuccessHandlerTest test`
- Public controller tests should cover HTTP status, JSON body, Problem Details validation errors, cookies/headers, and service delegation.
