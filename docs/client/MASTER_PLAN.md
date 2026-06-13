# VQC Copilot Client - Orchestrator Plan

> Date: 2026-06-13
> Audience: frontend orchestrator / implementation agents
> Product type: internal QC dashboard for QC engineers, QC leads, and admins
> Backend truth: `server/SERVER_CONTEXT.md`, `server/API_TREE.md`, current server DTOs

This file is the client-side control plane. It should let an orchestrator understand
what exists, what to build next, which contracts are safe, and where the detailed
story files live. Detailed screen/task work belongs in `docs/client/story/**`.

## 1. Read Order For Agents

1. Read `server/SERVER_CONTEXT.md`.
2. Read `server/API_TREE.md`.
3. Read this file.
4. Read the target story file listed in [Build Order](#8-build-order).
5. If a story links child markdown files, read the story `README.md` first, then
   the child files in listed order.

## 2. Current Product Shape

VQC Copilot is a dashboard for chatbot quality evaluation:

```text
Login
  -> Projects
    -> Target API Connectors
    -> Requirements
    -> Datasets
      -> Test Cases
      -> Import / AI Generate
    -> Rubrics / Versions / Criteria
    -> Evaluation Runs / Jobs
      -> Results
      -> QC Review
      -> Export
```

The client must follow the implemented backend, not older target docs, when there
is a mismatch.

## 3. Frontend Direction

| Area | Decision |
| --- | --- |
| Framework | Next.js App Router |
| UI language | Internal B2B dashboard, Linear-like clean/professional density |
| Styling | Tailwind CSS v4 + CSS variables |
| Component base | shadcn/ui customized; do not ship defaults unchanged |
| Server state | TanStack Query |
| Global UI/auth state | Zustand |
| Forms | React Hook Form + Zod |
| Tables | TanStack Table |
| Charts | Recharts |
| Icons | `@phosphor-icons/react` only |
| i18n | `next-intl`, Vietnamese primary, English secondary |
| Async jobs | Polling-first against existing backend; SSE is deferred |

Design constraints:
- No landing page; first public screen is login.
- Desktop is primary; tablet/mobile must remain usable.
- Labels above inputs; no placeholder-as-label.
- Skeletons should match page shape; avoid generic spinners.
- All visible strings go through i18n keys.
- Access tokens stay in memory only; never write JWTs to localStorage.

## 4. Backend Contract Rules

These rules override older client docs and examples.

### Auth And User

- Auth endpoints live under `/api/v1/auth`.
- Login returns `{ accessToken, tokenType, expiresInSeconds, user }`.
- Refresh uses `POST /api/v1/auth/refresh-token` with the server-managed
  `refresh_token` HttpOnly cookie.
- `UserResponse` shape:

```ts
type UserResponse = {
  publicId: string;
  email: string;
  displayName: string;
  role: 'QC_MEMBER' | 'QC_LEAD' | 'ADMIN';
  status: 'PENDING_EMAIL_VERIFICATION' | 'ACTIVE' | 'DISABLED';
  lastLoginAt: string | null;
};
```

Client route protection must not depend on middleware reading a Zustand token.
Use locale middleware for routing and a client auth bootstrap/guard for protected
dashboard pages.

### Pagination

All backend list responses use:

```ts
type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};
```

Do not use `content` or `totalElements` in client docs or code.

### Jobs

Current backend exposes:
- `GET /api/v1/jobs/{jobPublicId}`
- `GET /api/v1/evaluation-runs/{runPublicId}/events`

There is no implemented `GET /api/v1/jobs/{jobPublicId}/stream` endpoint. The
client MVP must poll `GET /jobs/{id}` and optionally fetch run events. SSE is a
deferred backend dependency.

### Evaluation And Export

- `POST /api/v1/projects/{projectPublicId}/evaluation-runs` returns
  `{ runPublicId, jobPublicId, status, message }`.
- `POST /api/v1/projects/{projectPublicId}/quick-evaluate` returns the same shape.
- `POST /api/v1/evaluation-runs/{runPublicId}/exports` returns
  `{ exportPublicId, jobPublicId, status, message }`.
- `GET /api/v1/exports/{exportPublicId}` returns `downloadUrl` only when ready.

### Connector

Connector create/update uses the current server DTO names:

```ts
type TargetApiConnectorRequest = {
  name: string;
  description?: string;
  rawCurl?: string;
  protocol?: 'HTTP';
  method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  baseUrl?: string;
  path?: string;
  url: string;
  headers?: Record<string, unknown>;
  queryParams?: Record<string, unknown>;
  pathParams?: Record<string, unknown>;
  bodyType?: 'NONE' | 'RAW_JSON' | 'RAW_TEXT';
  bodyTemplate?: Record<string, unknown>;
  bodyTemplateText?: string;
  authType?: 'NONE' | 'BEARER' | 'BASIC' | 'API_KEY';
  authConfig?: Record<string, unknown>;
  secretValues?: Record<string, string>;
  responseFormat?: 'JSON';
  responseSelector: string;
  isStreaming?: boolean;
  streamingType?: string;
  streamingEventSelector?: string;
  timeoutSeconds?: number;
  retryCount?: number;
  active?: boolean;
};
```

Raw secret values are write-only. Responses expose masked `secretRefs`, never raw
secrets.

### QC Review

Write payload uses `picBugUserPublicId`; response exposes `picBug` as a user
summary:

```ts
type ReviewDecisionRequest = {
  qcStatus: 'PASS' | 'FAIL' | 'NEED_FIX' | 'IGNORED';
  qcNote?: string;
  picBugUserPublicId?: string;
};
```

### Limits

- Import test cases accepts `.xlsx` and `.csv`.
- Import max file size is 5 MB.
- A dataset can have at most 100 active test cases.
- AI generation `count` is 5-100 and cannot push the dataset over 100 active cases.
- Dataset create requires `sourceType`; manual UI creation should send `MANUAL`.

## 5. Story File Standard

Every story markdown must contain:

- Metadata: epic, story id, depends on, backend contract source.
- Story Header using `As a / I want / so that`.
- `In Scope`, `Out of Scope`, and `Deferred`.
- API/Data Contract with current backend paths and response names.
- UI States.
- Acceptance Criteria as `[WEB]` Given/When/Then blocks.
- Implementation Checklist.
- Verification Notes.

If a story is too large, create a folder:

```text
docs/client/story/{epic}/{story-id}-{slug}/README.md
docs/client/story/{epic}/{story-id}-{slug}/{part}.md
```

The folder `README.md` owns ordering and links to every child file.

## 6. Global UI Inventory

Build shared UI in Epic 0 before feature screens:

- `PageShell`, `Sidebar`, `Header`, `Breadcrumb`
- `DataTable`, pagination, sorting, filtering toolbar
- `StatusBadge`, `MetricCard`, `EmptyState`, `LoadingSkeleton`
- `ConfirmDialog`, form field wrappers, file upload dropzone
- `JobProgress` polling UI
- `LanguageSwitcher`, `ThemeToggle`

## 7. Deferred Dependencies

These are intentionally not frontend blockers unless a story says otherwise:

| Dependency | Current decision |
| --- | --- |
| Job SSE stream | Deferred. Use polling-first MVP. |
| Dashboard aggregate API | Deferred. Use available list/detail endpoints or empty/limited widgets. |
| Cross-project recent runs API | Deferred. Do not fabricate global run data. |
| User profile update/password endpoint | Deferred unless backend adds it. Settings can show read-only profile. |
| S3/R2 export storage | Deferred; client uses existing export detail/download endpoints. |

## 8. Build Order

| Order | Epic | Story |
| --- | --- | --- |
| 0.1 | Foundation | [Project scaffolding](story/00-foundation/00.01-project-scaffolding.md) |
| 0.2 | Foundation | [API client layer](story/00-foundation/00.02-api-client-layer.md) |
| 0.3 | Foundation | [Auth store](story/00-foundation/00.03-auth-store.md) |
| 0.4 | Foundation | [Shared UI components](story/00-foundation/00.04-shared-ui-components/README.md) |
| 0.5 | Foundation | [Data table](story/00-foundation/00.05-data-table.md) |
| 0.6 | Foundation | [Theme and dark mode](story/00-foundation/00.06-theme-dark-mode.md) |
| 0.7 | Foundation | [i18n setup](story/00-foundation/00.07-i18n-setup.md) |
| 0.8 | Foundation | [Job progress polling](story/00-foundation/00.08-job-progress-polling.md) |
| 1.1 | Auth | [Auth layout](story/01-auth/01.01-auth-layout.md) |
| 1.2 | Auth | [Login](story/01-auth/01.02-login.md) |
| 1.3 | Auth | [Register](story/01-auth/01.03-register.md) |
| 1.4 | Auth | [Verify email](story/01-auth/01.04-verify-email.md) |
| 1.5 | Auth | [Forgot and reset password](story/01-auth/01.05-password-reset.md) |
| 1.6 | Auth | [Protected route guard](story/01-auth/01.06-protected-route-guard.md) |
| 2.1 | Dashboard Shell | [Sidebar navigation](story/02-dashboard-shell/02.01-sidebar-navigation.md) |
| 2.2 | Dashboard Shell | [Header and breadcrumb](story/02-dashboard-shell/02.02-header-breadcrumb.md) |
| 2.3 | Dashboard Shell | [Dashboard layout](story/02-dashboard-shell/02.03-dashboard-layout.md) |
| 2.4 | Dashboard Shell | [Dashboard home](story/02-dashboard-shell/02.04-dashboard-home.md) |
| 3.1 | Projects | [Project list](story/03-projects/03.01-project-list.md) |
| 3.2 | Projects | [Create project](story/03-projects/03.02-create-project.md) |
| 3.3 | Projects | [Project overview](story/03-projects/03.03-project-overview.md) |
| 4.1 | Connectors | [Connector list](story/04-connectors/04.01-connector-list.md) |
| 4.2 | Connectors | [Create/edit connector](story/04-connectors/04.02-create-edit-connector.md) |
| 4.3 | Connectors | [Connector test run](story/04-connectors/04.03-connector-test-run.md) |
| 5.1 | Requirements | [Requirement CRUD](story/05-requirements/05.01-requirement-crud.md) |
| 6.1 | Datasets | [Dataset list](story/06-datasets/06.01-dataset-list.md) |
| 6.2 | Datasets | [Create dataset](story/06-datasets/06.02-create-dataset.md) |
| 6.3 | Datasets | [Dataset detail](story/06-datasets/06.03-dataset-detail.md) |
| 7.1 | Test Cases | [Test case table](story/07-test-cases/07.01-test-case-table.md) |
| 7.2 | Test Cases | [Test case editor](story/07-test-cases/07.02-test-case-editor.md) |
| 7.3 | Test Cases | [Bulk import](story/07-test-cases/07.03-bulk-import.md) |
| 7.4 | Test Cases | [AI generate](story/07-test-cases/07.04-ai-generate.md) |
| 8.1 | Rubrics | [Rubric list](story/08-rubrics/08.01-rubric-list.md) |
| 8.2 | Rubrics | [Create rubric](story/08-rubrics/08.02-create-rubric.md) |
| 8.3 | Rubrics | [Rubric detail and versions](story/08-rubrics/08.03-rubric-detail-versions.md) |
| 8.4 | Rubrics | [Version detail criteria list](story/08-rubrics/08.04-version-detail-criteria-list.md) |
| 8.5 | Rubrics | [Criteria editor](story/08-rubrics/08.05-criteria-editor.md) |
| 9.1 | Evaluation | [Evaluation list](story/09-evaluation-runs/09.01-evaluation-list.md) |
| 9.2 | Evaluation | [Start evaluation](story/09-evaluation-runs/09.02-start-evaluation.md) |
| 9.3 | Evaluation | [Run detail](story/09-evaluation-runs/09.03-run-detail.md) |
| 9.4 | Evaluation | [Results table](story/09-evaluation-runs/09.04-results-table.md) |
| 9.5 | Evaluation | [Result detail panel](story/09-evaluation-runs/09.05-result-detail-panel.md) |
| 10.1 | QC Review | [Review decision form](story/10-qc-review/10.01-review-decision-form.md) |
| 10.2 | QC Review | [Update review](story/10-qc-review/10.02-update-review.md) |
| 11.1 | Export | [Export dialog and download](story/11-export/11.01-export-dialog-download.md) |
| 12.1 | Polish | [Settings](story/12-polish-settings/12.01-settings.md) |
| 12.2 | Polish | [Error boundaries](story/12-polish-settings/12.02-error-boundaries.md) |
| 12.3 | Polish | [Loading and transitions](story/12-polish-settings/12.03-loading-transitions.md) |
| 12.4 | Polish | [Responsive audit](story/12-polish-settings/12.04-responsive-audit.md) |

## 9. Acceptance For Documentation

Before treating client docs as ready:

```bash
rtk rg "QC_ENGINEER|fileUrl|responseBodySelector|10MB|1000|Response: 202 \\{ publicId, jobPublicId \\}" docs/client/story
```

The command should find no stale contract usage except explicit deferred notes.
