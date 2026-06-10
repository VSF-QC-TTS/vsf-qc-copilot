# Server API TODO

Date: 2026-06-10

Purpose: track API slices while keeping `CODEX_CONTEXT.md` short. Update this file after each API endpoint commit.

## Completed

- Auth: register, login, refresh-token, logout, verify-email, forgot-password, reset-password.
- User: current user (`GET /api/v1/users/me`).
- Project: create, list, detail, update, archive.
- Mock chatbot: public chat fallback (`POST /mock-chatbot/chat`).
- Target API connector: create, list, detail, update, test-run.

## In Progress

- Requirement API
  - [x] Create requirement
  - [ ] List requirements
  - [ ] Get requirement detail
  - [ ] Update requirement

## Next

- Dataset/Test Case API.
- Rubric/Criteria API.
- Evaluation run/job skeleton.
