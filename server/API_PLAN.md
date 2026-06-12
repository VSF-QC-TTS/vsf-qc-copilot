# API Plan — Current Backend Slice Tracker

Date: 2026-06-12
Status: **Promptfoo CLI integration implemented; runner lockfile pending npm registry access**

Purpose: keep the immediate backend plan short. `API_TODO.md` and `API_TREE.md` remain the source for completed endpoint inventory and resource relationships.

## Completed In This Sequence

1. Worker + Promptfoo mock executor.
   - `JobWorker` consumes Redis queue messages.
   - `EvaluationJobHandler` writes evaluation results and job events.
   - Promptfoo executor uses Strategy: mock mode implemented.
   - Commits:
     - `feat(worker): process evaluation jobs with mock promptfoo`
     - `docs(server): update handoff docs for evaluation worker`

2. QC Review APIs.
   - `PUT /api/v1/evaluation-results/{resultPublicId}/review-decision`
   - `GET /api/v1/evaluation-results/{resultPublicId}/review-decision`
   - `PATCH /api/v1/review-decisions/{reviewDecisionPublicId}`
   - Commits:
     - `feat(review): upsert result review decision`
     - `feat(review): get result review decision`
     - `feat(review): update review decision`

3. Evaluation result QC fields.
   - `GET /api/v1/evaluation-runs/{runPublicId}/results` includes `qcStatus`, `qcNote`, and `picBug`.
   - Supports optional `qcStatus` filter, including derived `NOT_REVIEWED`.
   - Commit:
     - `feat(evaluation): include qc review fields in results`

4. Export APIs and worker generation.
   - `POST /api/v1/evaluation-runs/{runPublicId}/exports`
   - `GET /api/v1/exports/{exportPublicId}`
   - `GET /api/v1/exports/{exportPublicId}/file`
   - Export job generation uses `ExportGenerator` Strategy for JSON and Excel.
   - Excel uses Apache POI `poi-ooxml`; JSON uses Jackson.
   - Export files are written through `ObjectStorageService`; local storage is the current implementation.
   - Commits:
     - `feat(export): create export jobs`
     - `feat(export): get export detail`
     - `feat(export): generate and download export files`
     - `feat(export): abstract export file storage`

5. Real Promptfoo CLI integration.
   - Redis remains the server-to-worker queue boundary; promptfoo is invoked only after `JobWorker` consumes an evaluation job.
   - `CliPromptfooExecutor` generates per-run files under `vqc.promptfoo.work-dir/{runPublicId}`.
   - Config validation runs before eval and stores validation stdout/stderr logs.
   - Eval uses local binary path `tooling/promptfoo-runner/node_modules/.bin/promptfoo`, `--no-cache`, per-run promptfoo config/log dirs, and `results.outputs[]` parsing.
   - Supported selectors: `$.answer`, `$.data.answer`; persisted secret placeholders fail fast.
   - Commit:
     - Pending current commit: `feat(promptfoo): run evaluations with local promptfoo cli`

## Current Verify Commands

Focused promptfoo/job suite:

```bash
rtk bash mvnw -Dtest=CliPromptfooExecutorTest,MockPromptfooExecutorTest,EvaluationJobHandlerTest,JobWorkerTest test
```

Previously passed focused suites:

```bash
rtk bash mvnw -Dtest=ExportControllerTest,ExportServiceImplTest,ExportJobHandlerTest,LocalObjectStorageServiceTest,JobWorkerTest,JobServiceImplTest test
rtk bash mvnw -Dtest=ReviewDecisionControllerTest,ReviewDecisionServiceImplTest test
rtk bash mvnw -Dtest=EvaluationRunControllerTest,EvaluationRunServiceImplTest test
rtk bash mvnw -Dtest=EvaluationRunControllerTest,EvaluationRunServiceImplTest,JobControllerTest,JobServiceImplTest,MockPromptfooExecutorTest,EvaluationJobHandlerTest,JobWorkerTest test
```

## Next Likely Backend Slice

Promptfoo follow-up:
- Generate and commit `tooling/promptfoo-runner/package-lock.json` once npm registry access is allowed.
- Add encrypted secret storage or map richer rubric criteria into Promptfoo judge configuration.
