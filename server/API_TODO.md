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

## Next

- Choose the next backend slice: richer rubric judge mapping (Promptfoo assertions from rubric criteria), connector timeout/retry behavior, or an export storage provider beyond local filesystem.
