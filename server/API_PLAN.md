# API Plan — Current Backend Slice Tracker

Date: 2026-06-12
Status: **Promptfoo Rubric Judge Mapping implemented; 40 focused rubric/promptfoo/job tests pass**

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
   - Eval uses local binary path `tooling/promptfoo-runner/node_modules/.bin/promptfoo`, `--no-cache`, per-run promptfoo config/log dirs, and `results.results[]` parsing with compatibility for `results.outputs[]`.
   - Supported selectors: `$.answer`, `$.data.answer`; persisted secret placeholders fail fast.
   - Commits:
     - `feat(promptfoo): run evaluations with local promptfoo cli`
     - `chore(promptfoo): lock local runner dependencies`
     - `fix(promptfoo): parse real cli result rows`

6. Promptfoo Secret-Store.
   - `ConnectorSecretService` interface + `ConnectorSecretServiceImpl` encrypts/persists secrets via `AesGcmEncryptor` into `connector_secrets` table.
   - Connector create/update wired to `connectorSecretService.saveSecrets()`.
   - Connector test-run resolves `{{secret:KEY}}` placeholders to decrypted values before HTTP calls.
   - `PromptfooConfigGenerator.rejectSecrets()` replaced by `resolveSecretsToEnvRefs()` — maps `{{secret:KEY}}` to `{{env.VQC_SECRET_KEY}}`.
   - `PromptfooCommandExecutor.eval()` accepts `secretEnvVars` map and merges into process environment.
   - `CliPromptfooExecutor` decrypts secrets via `ConnectorSecretService` and passes `VQC_SECRET_*` env vars. Raw secrets never touch disk.
   - Tests: 9 `ConnectorSecretServiceImplTest`, 10 `CliPromptfooExecutorTest`, 7 `TargetApiConnectorServiceImplTest`, 9 `AesGcmEncryptorTest` — 35 total, 0 failures.

7. Promptfoo Rubric Judge Mapping.
   - `RubricAssertionMapper` (SRP) translates `RubricCriterion` entities into Promptfoo `llm-rubric` assertion maps; uses `judgeInstruction`, optional `passCondition`/`failCondition`, `weight`, and `metricKey`.
   - `CriteriaScoreCalculator` (SRP) computes weighted `judgeScore` = Σ(weight × score) / Σ(weight) and applies `isCritical` override: any critical criterion failure → `JudgeStatus.FAIL`.
   - `PromptfooConfigGenerator` delegates assertion building to `RubricAssertionMapper`; adds `defaultTest.options.provider` (`google:gemini-2.5-flash`) when criteria exist. Ground-truth `contains` assertions kept alongside rubric assertions.
   - `PromptfooResultParser` extracts per-criterion results from `gradingResult.componentResults` into `criteriaResultsJson`.
   - `EvaluationJobHandler` delegates score computation to `CriteriaScoreCalculator`; writes `criteriaResultsJson` to `EvaluationResult`.
   - Config: `vqc.promptfoo.grading-provider` and `vqc.promptfoo.grading-api-key` in `PromptfooProperties`.
   - Tests: 7 `RubricAssertionMapperTest`, 9 `CriteriaScoreCalculatorTest`, 11 `CliPromptfooExecutorTest`, 3 `EvaluationJobHandlerTest`, 4 `PromptfooConfigGeneratorTest` — 40 total, 0 failures.
   - Commits:
     - `feat(rubric): map criteria to promptfoo llm-rubric assertions`
     - `feat(rubric): compute weighted score with critical override`
     - `feat(promptfoo): generate rubric assertions in cli config`
     - `feat(evaluation): parse criteria results and compute weighted scores`

## Current Verify Commands

Focused rubric/promptfoo/job suite:

```bash
rtk bash ./mvnw -Dtest=RubricAssertionMapperTest,CriteriaScoreCalculatorTest,CliPromptfooExecutorTest,PromptfooConfigGeneratorTest,EvaluationJobHandlerTest test
```

Focused secret-store/connector/crypto suite:

```bash
rtk bash ./mvnw -Dtest=ConnectorSecretServiceImplTest,AesGcmEncryptorTest,CliPromptfooExecutorTest,TargetApiConnectorServiceImplTest test
```

Previously passed focused suites:

```bash
rtk bash ./mvnw -Dtest=ExportControllerTest,ExportServiceImplTest,ExportJobHandlerTest,LocalObjectStorageServiceTest,JobWorkerTest,JobServiceImplTest test
rtk bash ./mvnw -Dtest=ReviewDecisionControllerTest,ReviewDecisionServiceImplTest test
rtk bash ./mvnw -Dtest=EvaluationRunControllerTest,EvaluationRunServiceImplTest test
rtk bash ./mvnw -Dtest=PromptfooWorkerSmokeTest -Dvqc.promptfoo.smoke=true test
```

## Next Likely Backend Slice

Possible directions:
- Connector timeout/retry behavior.
- Export storage provider beyond local filesystem.
- Dashboard/analytics endpoints.
