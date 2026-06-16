# Client Context

Date: 2026-06-16
Repo area: `client/`
Last lint pass: 0 errors, 0 warnings (2026-06-16) via `eslint`.
Purpose: this is the client bootstrap handoff. If a user only says "read `client/CLIENT_CONTEXT.md`", the agent must use this file to discover the next files to read without asking for more pointers. Current code is the source of truth when docs and implementation differ. The full product target lives in `docs/`; treat docs as roadmap/contract intent unless the user explicitly asks to migrate current code toward them.

Reading tags:
- `[START_HERE]`: always read first; includes rules, docs map, and agent startup order.
- `[CURRENT_STATE]`: read before adding, removing, or changing pages, screens, routing, or auth guards.
- `[STATE_MANAGEMENT]`: read before working on API integration, Zustand stores, or TanStack Query.
- `[CONVENTIONS]`: read before adding new React components, forms, tables, icons, or translating strings.
- `[TESTS]`: read before running validation/build steps.

## [START_HERE] Rules

Rules:
- Prefix verbose development, build, and linting commands with `rtk` (e.g. `rtk pnpm run build`, `rtk pnpm run lint`) if the `rtk` CLI is installed.
- Use `pnpm` as the package manager inside `client/` directory.
- Access token remains **memory-only**; it must never be written to `localStorage` or `sessionStorage`.
- Route protection must be handled by client auth bootstrap/guard (`AuthGuard`), not localized server middleware.
- All user-facing text strings must go through the i18n localization keys using `next-intl` (Vietnamese default, English fallback).
- Only `@phosphor-icons/react` is allowed for icons. Do not import `lucide-react` or browser icons.
- DTO validation on frontend forms should use React Hook Form paired with Zod schemas matching backend validations.
- If client implementation context changes, update this `client/CLIENT_CONTEXT.md` handoff.

## [START_HERE] Docs Map

Docs map:
- `client/CLIENT_CONTEXT.md`: read first; this file routes the rest of the frontend context.
- `docs/client/MASTER_PLAN.md`: client-side orchestrator plan listing build order, architectural choices, and inventory.
- `docs/FRONTEND_API_REFERENCE.md`: target API endpoints, enums, parameters, and payloads.
- `docs/client/story/`: folders detailing individual epic user stories and implementation checklists.
- `client/package.json`: package dependencies, configurations, and script definitions.

## [START_HERE] Agent Startup Order

Agent startup order:
1. Read this `client/CLIENT_CONTEXT.md`.
2. Read `docs/client/MASTER_PLAN.md` to understand the build order and shared UI inventory.
3. Check the target screen/route folder under `client/src/app/[locale]/`.
4. Read the corresponding story file under `docs/client/story/` before planning or implementing client changes.
5. In case of API contract questions, cross-reference `docs/FRONTEND_API_REFERENCE.md` and `server/SERVER_CONTEXT.md`.
6. Run `pnpm run build` or `pnpm run lint` before committing frontend changes.
7. Commit changes using conventional commit formats: `type(scope): summary`.

## [CURRENT_STATE] Auth & Protected Routes

Current implemented auth state:
- All auth screens are grouped under `(auth)` locale route: `/login`, `/register`, `/verify-email`, `/forgot-password`, `/reset-password`.
- Login saves access token to Zustand store (`useAuthStore`) in memory.
- Access token refresh:
  - handoff runs automatically via `axios-auth-refresh` when backend returns 401 with error code `ACCESS_TOKEN_EXPIRED`.
  - calls `POST /api/v1/auth/refresh-token` using the HttpOnly refresh token cookie.
- Silent login check: `AuthGuard` attempts to refresh the access token on initial load. If successful, user is marked authenticated. If not, redirected to `/login`.
- `AuthBootstrap` wires store methods (`getTokenFn`, `clearAuthFn`, `onRefreshedFn`) with the base Axios client.

## [CURRENT_STATE] Routing and Pages

The routing structure follows Next.js App Router localized paths: `client/src/app/[locale]`.
- `(auth)` group:
  - `login/page.tsx`: login form, saves user profile to store.
  - `register/page.tsx`: registers user.
  - `verify-email/page.tsx`: processes verification token.
  - `forgot-password/page.tsx`: sends reset link.
  - `reset-password/page.tsx`: updates password with token.
- `(dashboard)` group:
  - `dashboard/page.tsx`: home overview.
  - `projects/page.tsx`: lists projects.
  - `projects/[projectId]/page.tsx`: project detail with summaries of connectors, datasets, judge models, recent evaluations, and project readiness checklist.
  - `projects/[projectId]/connectors/page.tsx`: lists target API connectors.
  - `projects/[projectId]/connectors/[connectorId]/page.tsx`: edit connector and test-run panel.
  - `projects/[projectId]/connectors/new/page.tsx`: create connector (supports manual config or auto-creating from cURL).
  - `projects/[projectId]/datasets/page.tsx`: lists datasets.
  - `projects/[projectId]/datasets/[datasetId]/page.tsx`: dataset detail containing test case table (supports manual edits, bulk import from Excel/CSV, and AI test case generation).
  - `projects/[projectId]/evaluations/page.tsx`: lists evaluation runs.
  - `projects/[projectId]/evaluations/[runId]/page.tsx`: details of an evaluation run including event streams/polling, status, case completions, and results list with inline QC review panel.
  - `projects/[projectId]/judge-models/page.tsx`: lists provider model configurations and connection testing.
  - `rubrics/page.tsx`: lists independent/user-scoped rubrics.
  - `rubrics/[rubricId]/page.tsx`: rubric detail with draft and published versions list.
  - `rubrics/[rubricId]/versions/[versionId]/page.tsx`: rubric version criteria list; page is split into page.tsx and criteria-editor-panel.tsx.
  - `settings/page.tsx`: user profile settings.

## [STATE_MANAGEMENT] Zustand & React Query

State management details:
- **Global UI State**: Zustand stores under `client/src/lib/store/`:
  - `useAuthStore`: handles `accessToken`, `user` profile, and `isAuthenticated` status.
  - `useSidebarStore`: persists sidebar open/collapsed state.
- **Server State**: TanStack Query (React Query) manages data fetching and mutations:
  - Standard React Query keys used:
    - Project: `['projects']`, `['project', projectId]`
    - Evaluations: `['evaluations', projectId, 'recent']`, `['evaluation-run', runId]`
    - Results & Review: `['evaluation-results', runId]`, `['review-decision', resultId]`
    - Connectors: `['connectors', projectId]`, `['connector', connectorId]`
    - Datasets & Cases: `['datasets', projectId]`, `['dataset', datasetId]`, `['test-cases', datasetId]`
    - Rubrics & Criteria: `['rubrics']`, `['rubric', rubricId]`, `['rubric-versions', rubricId]`, `['criteria', versionId]`
    - Jobs: `['job', jobPublicId]`
    - System/Readiness Checklist: `['project-readiness', projectId, ...]`
- **Colocated DTO/Response Types**: Specific UI-only states/types (e.g., `EvaluationResultRow` or component props types) are colocated inside leaf files. Main backend DTO/Response shapes (like `EvaluationRunDetail`, `DatasetDetailResponse`, and basic auth/project responses) are centralized in `client/src/lib/api/types.ts` to ensure reusability.

## [CURRENT_STATE] Async Jobs & Streams

Async job handling:
- **Polling**: Hook `useJobProgress` polls `GET /api/v1/jobs/{jobPublicId}` every 3 seconds while active, executing success or failure callbacks on completion.
- **Streaming**: Hook `useEvaluationRunEventsStream` establishes custom `fetch`-based stream connection to `GET /api/v1/evaluation-runs/{runPublicId}/events/stream`.
  - Appends `Authorization: Bearer <access_token>` manually since native browser `EventSource` cannot set custom headers.
  - Automatically reconnects up to 2 times on errors before falling back.

## [CONVENTIONS] Frontend Guidelines

Coding conventions:
- **Component File Size**: Flag UI files exceeding 500 lines (Medium) and 1,000 lines (Critical) as violating Single Responsibility Principle (e.g., version detail `page.tsx` contains criteria editor panel). Split subcomponents into specialized files under a colocated folder or under `src/components/`.
- **Styling**: Tailwind CSS v4 paired with custom shadcn-inspired components inside `client/src/components/ui`. Light/Dark mode is supported via `next-themes` and variables in `client/src/styles/globals.css`.
- **Translations**: `next-intl` configuration requires both English (`en.json`) and Vietnamese (`vi.json`) keys under `client/messages/`. Default locale is `vi`.
- **Forms**: React Hook Form combined with Zod validation schemas (`client/src/lib/validations/`). Error boundaries render validation messages.
- **Icons**: Import strictly from `@phosphor-icons/react` package. No other icon library should be utilized.
- **API client**: All calls must go through the configured `apiClient` helper inside `client/src/lib/api/client.ts` to ensure automatic auth header attachment, refresh logic, and error normalization.

## [TESTS] Development and Validation

Commands (run within the `client/` subdirectory):
- Run development server: `pnpm run dev`
- Build production bundle: `pnpm run build`
- Validate formatting and linting: `pnpm run lint`
- Validate translations: `pnpm run i18n:check`
