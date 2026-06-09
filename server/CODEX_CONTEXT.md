# Codex Context

Date: 2026-06-09
Repo area: `server/`

Current focus: backend auth/register foundation.

Relevant docs:
- Main implementation brief: `../docs/backend-codex-implementation-brief.md`
- Local agent rule: prefix shell commands with `rtk`.

Implemented:
- Spring Boot backend scaffold under `me.nghlong3004.vqc.api`.
- Local registration endpoint: `POST /api/v1/auth/register`.
- Register request uses API `email`; persistence stores it in `users.username` per MVP note.
- Registration normalizes email, hashes password, defaults display name from email local-part, and returns public user response.
- Duplicate email maps to `EMAIL_ALREADY_EXISTS`, including DB unique-race path.
- Entity renamed to `me.nghlong3004.vqc.api.user.entity.User`.
- `Role` enum implements `GrantedAuthority`.
- Roles: `QC_MEMBER`, `QC_LEAD`, `ADMIN`.
- `Role#getAuthority()` returns `ROLE_` + enum name.
- User has `role` field; default is `QC_MEMBER`.
- `UserDetailsServiceImpl` uses `user.getRole()` instead of string authorities.
- Flyway `V2__create_users.sql` includes `role VARCHAR(50) NOT NULL DEFAULT 'QC_MEMBER'`.
- Mail config maps `.env` keys `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`.
- Registration dispatches an async welcome email after user creation.
- Welcome email template: `src/main/resources/templates/mail/registration-welcome.html`.
- HTML email template uses email-safe table markup, preheader text, and inline CSS.
- Auth register endpoint has OpenAPI annotations with request/response/error examples.
- Errors use Problem Details shape: `type`, `title`, `status`, `detail`, `instance`, plus `code` and optional `errors`.
- Request DTO validation annotations should include explicit messages; `GlobalException` returns these messages in `errors`.
- Service interfaces should declare validation contracts with `@Validated` and `@Valid` on request parameters where applicable.
- Controllers should expose Swagger/OpenAPI request, success response, and error response examples for public APIs.

Tests:
- Added focused non-Testcontainers tests:
  - `RoleTest`
  - `UserMapperTest`
  - `UserServiceImplTest`
  - `HtmlMailTemplateRendererTest`
  - `ErrorResponseTest`
- Avoid Mockito in service test because inline ByteBuddy self-attach fails on this WSL/JDK setup.
- Verified with:
  `rtk bash mvnw -Dtest=RoleTest,UserMapperTest,UserServiceImplTest,HtmlMailTemplateRendererTest,ErrorResponseTest test`
- Result: build success, 7 tests passed.

Known caveats:
- Full `mvn test` starts `ServerApplicationTests` with Testcontainers and is slow.
- Full context test currently also needs env config such as `JWT_SECRET_KEY`.
- `server/.env` exists locally and contains secrets; keep it untracked.

Next likely work:
- Implement docs-contract auth endpoints: `POST /api/v1/sessions`, `GET /api/v1/users/me`, `DELETE /api/v1/sessions/current`.
- Add JWT issuance for local login.
- Seed demo user if needed.
