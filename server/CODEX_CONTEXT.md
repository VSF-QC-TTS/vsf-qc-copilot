# Codex Context

Date: 2026-06-09
Repo area: `server/`

Rules:
- Prefix shell commands with `rtk`.
- Skip Testcontainers-heavy full test runs unless explicitly requested.
- Keep `server/.env` untracked and never print secret values.

Auth state:
- Local auth endpoints live under `/api/v1/auth`: `register`, `login`, `verify-email`, `forgot-password`, `reset-password`.
- Login returns the access token in the response body and the refresh token only as an HttpOnly `refresh_token` cookie.
- CSRF is disabled; protected APIs use `Authorization: Bearer ...`.
- `JwtDecoder` accepts only JWTs with `token_type=access`.
- Register creates `PENDING_EMAIL_VERIFICATION` users and emails `${WEB_BASE_URL}/verify-email?token=...`.
- Forgot password emails `${WEB_BASE_URL}/reset-password?token=...`; the request must not reveal account existence.

Domain choices:
- User entity is `me.nghlong3004.vqc.api.user.entity.User`.
- `Role` implements `GrantedAuthority`; values are `QC_MEMBER`, `QC_LEAD`, `ADMIN`.
- `Role#getAuthority()` returns `ROLE_` plus the enum name.
- Public API uses `email`; persistence stores it in `users.username`.

Persistence:
- Flyway has only `V1__enable_extensions.sql` and `V2__create_users.sql`.
- `V2__create_users.sql` includes `users`, `email_verification_tokens`, and `password_reset_tokens` because migrations have not been run yet.
- Email verification and password reset tokens are opaque random raw values; only SHA-256 hashes are stored.
- `OpaqueTokenService` owns raw token generation and hashing for one-time email tokens.

Mail:
- Mail config maps `.env` keys `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`.
- Mail flow uses Strategy + Factory: `MailType`, `MailRequest`, `MailMessage`, `MailStrategy`, `MailStrategyFactory`.
- Templates are HTML emails with table-friendly structure and inline CSS.

API conventions:
- Errors use Problem Details shape: `type`, `title`, `status`, `detail`, `instance`, plus `code` and optional `errors`.
- Request DTO validation annotations should include explicit messages.
- Public controllers should expose Swagger/OpenAPI request, success response, and error response examples.
- Public service/repository/mapper interfaces should have JavaDoc with `@param` and `@return` where useful, and link domain types with `{@link ...}`.

Testing:
- Use focused tests instead of full `mvn test` when possible:
  `rtk bash mvnw -Dtest=RoleTest,UserMapperTest,UserServiceImplTest,HtmlMailTemplateRendererTest,EmailVerificationMailStrategyTest,PasswordResetMailStrategyTest,ErrorResponseTest,AuthServiceImplTest,EmailVerificationServiceImplTest,PasswordResetServiceImplTest,AuthControllerTest test`
- Public controller tests should cover HTTP status, JSON body, Problem Details validation errors, cookies/headers, and service delegation.
