# Server API TODO

Date: 2026-06-11

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
- Evaluation Results: list under run with optional `judgeStatus` filter.
- Evaluation Events: list job events under run.
- Job: detail flat path with resolved `resourcePublicId`.
- Worker + Promptfoo mock executor (Step 10).
- QC Review: upsert/get review decision by evaluation result; patch by review decision.

## Next

- Evaluation Results: add QC review fields/filter.
- Export endpoints (Phase 8).
