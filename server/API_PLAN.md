# API Plan — Current Backend Slice Tracker

Date: 2026-06-13
Status: **QC Productivity Features — planning, not started**

Purpose: keep the immediate backend plan short. `API_TODO.md` and `API_TREE.md` remain the source for completed endpoint inventory and resource relationships.

## Completed In Previous Sequences

1. Worker + Promptfoo mock executor.
2. QC Review APIs.
3. Evaluation result QC fields.
4. Export APIs and worker generation.
5. Real Promptfoo CLI integration.
6. Promptfoo Secret-Store.
7. Promptfoo Rubric Judge Mapping.

See git log for full details on each sequence.

## Workflow

Every step below follows the same cycle:

```text
Code (implement feature / modify classes)
  → Write tests
    → Run tests
      → FAIL → fix code → re-run
      → PASS → git add + git commit → next step
```

Never move to the next step until current step's tests pass and changes are committed.

## Current Slice: QC Productivity Features

---

### 8. Bulk Import Test Cases.

#### Step 8.1 — Import service + response DTO

Code:

```text
[NEW]  testcase/response/ImportTestCaseResponse.java          — record(totalRows, importedCount, skippedCount, errors)
[NEW]  testcase/service/TestCaseImportService.java             — interface
[NEW]  testcase/service/impl/TestCaseImportServiceImpl.java    — Excel/CSV parser, file validation, bulk save
[MOD]  dataset/enums/DatasetSourceType.java                    — add IMPORTED_CSV
[MOD]  exception/ErrorCode.java                                — add IMPORT_FILE_EMPTY, IMPORT_FILE_TOO_LARGE, IMPORT_FILE_INVALID_FORMAT, IMPORT_TOO_MANY_ROWS
[MOD]  testcase/repository/TestCaseRepository.java             — add findMaxSortOrderByDatasetId(Long)
```

Test:

```text
[NEW]  TestCaseImportServiceImplTest   — file validation, Excel parse, CSV parse, blank question skip, max rows, sort order
```

Run:

```bash
rtk bash ./mvnw -Dtest=TestCaseImportServiceImplTest test
```

Commit:

```text
feat(testcase): bulk import test cases from excel and csv
```

#### Step 8.2 — Import controller endpoint

Code:

```text
[MOD]  testcase/controller/TestCaseController.java   — add POST /api/v1/datasets/{datasetPublicId}/test-cases/import
```

Test:

```text
[MOD]  TestCaseControllerTest   — add import endpoint tests (200, 400 empty file, 400 bad format, 422 too many)
```

Run:

```bash
rtk bash ./mvnw -Dtest=TestCaseControllerTest,TestCaseImportServiceImplTest test
```

Commit:

```text
feat(testcase): add bulk import endpoint
```

---

### 9. AI Generate Dataset.

#### Step 9.1 — Schema + entity change

Code:

```text
[MOD]  db/migration/V1__init_schema.sql   — add generation_prompt TEXT to datasets table
[MOD]  dataset/entity/Dataset.java         — add generationPrompt field
[MOD]  job/enums/ResourceType.java         — add DATASET
```

Test:

```text
existing DatasetServiceImplTest, DatasetMapperTest must still pass
```

Run:

```bash
rtk bash ./mvnw -Dtest=DatasetServiceImplTest,DatasetMapperTest test
```

Commit:

```text
feat(dataset): add generation_prompt column and dataset resource type
```

#### Step 9.2 — Generation service + job creation

Code:

```text
[NEW]  dataset/request/GenerateDatasetRequest.java                — record(requirementPublicId, count, additionalPrompt)
[NEW]  dataset/response/GenerateDatasetResponse.java              — record(datasetPublicId, jobPublicId, status, message)
[NEW]  dataset/service/DatasetGenerationService.java               — interface
[NEW]  dataset/service/impl/DatasetGenerationServiceImpl.java      — validate, create Job(DATASET_GENERATION), push Redis, return 202
```

Test:

```text
[NEW]  DatasetGenerationServiceImplTest   — validation (dataset DRAFT, requirement ACTIVE, count limits), job creation, Redis push
```

Run:

```bash
rtk bash ./mvnw -Dtest=DatasetGenerationServiceImplTest test
```

Commit:

```text
feat(dataset): create generation job and push to redis queue
```

#### Step 9.3 — Job handler (Gemini AI call)

Code:

```text
[NEW]  dataset/handler/DatasetGenerationJobHandler.java   — load requirement, call ChatClient, parse JSON, bulk insert test_cases, emit events
[MOD]  job/worker/JobWorker.java                          — add DATASET_GENERATION routing
```

Test:

```text
[NEW]  DatasetGenerationJobHandlerTest   — mock ChatClient, parse response, bulk insert, error handling
[MOD]  JobWorkerTest                     — add DATASET_GENERATION routing case
```

Run:

```bash
rtk bash ./mvnw -Dtest=DatasetGenerationJobHandlerTest,JobWorkerTest test
```

Commit:

```text
feat(dataset): generate test cases with gemini ai in worker
```

#### Step 9.4 — Generate controller endpoint

Code:

```text
[MOD]  dataset/controller/DatasetController.java   — add POST /api/v1/datasets/{datasetPublicId}/generate
```

Test:

```text
[MOD]  DatasetControllerTest   — add generate endpoint tests (202, 400, 404, 422)
```

Run:

```bash
rtk bash ./mvnw -Dtest=DatasetControllerTest,DatasetGenerationServiceImplTest test
```

Commit:

```text
feat(dataset): add ai generate endpoint
```

---

### 10. Rubric Decoupling from Project.

#### Step 10.1 — Schema + entity change

Code:

```text
[MOD]  db/migration/V1__init_schema.sql   — rubrics: remove project_id + FK + index, add is_template + index
[MOD]  rubric/entity/Rubric.java          — remove Project field, add isTemplate Boolean
[MOD]  rubric/mapper/RubricMapper.java    — remove project mapping
[MOD]  rubric/response/RubricResponse.java — remove projectPublicId, add isTemplate
```

Test:

```text
[MOD]  RubricMapperTest   — update for removed project field, added isTemplate
```

Run:

```bash
rtk bash ./mvnw -Dtest=RubricMapperTest test
```

Commit:

```text
refactor(rubric): remove project_id from rubrics schema and entity
```

#### Step 10.2 — Service + repository (user-scoped)

Code:

```text
[MOD]  rubric/service/RubricService.java          — change method signatures (remove projectPublicId)
[MOD]  rubric/service/impl/RubricServiceImpl.java — remove project scoping, query by createdBy
[MOD]  rubric/repository/RubricRepository.java    — replace project queries with createdBy + isTemplate queries
[MOD]  rubric/request/CreateRubricRequest.java    — add isTemplate (optional, default false)
```

Test:

```text
[MOD]  RubricServiceImplTest   — update all tests: remove project context, user-scoped
```

Run:

```bash
rtk bash ./mvnw -Dtest=RubricServiceImplTest test
```

Commit:

```text
feat(rubric): user-scoped rubric service without project dependency
```

#### Step 10.3 — Controller (new API paths)

Code:

```text
[MOD]  rubric/controller/RubricController.java   — remove project-nested create/list, add user-scoped POST/GET /api/v1/rubrics
```

Test:

```text
[MOD]  RubricControllerTest   — update API paths, new create/list tests
```

Run:

```bash
rtk bash ./mvnw -Dtest=RubricControllerTest,RubricServiceImplTest test
```

Commit:

```text
feat(rubric): user-scoped rubric controller endpoints
```

#### Step 10.4 — Clone endpoint

Code:

```text
[NEW]  rubric/request/CloneRubricRequest.java               — record(name?)
[MOD]  rubric/service/RubricService.java                    — add cloneRubric method
[MOD]  rubric/service/impl/RubricServiceImpl.java           — deep-copy rubric + published version + criteria
[MOD]  rubric/controller/RubricController.java              — add POST /api/v1/rubrics/{rubricPublicId}/clone
```

Test:

```text
[MOD]  RubricServiceImplTest    — clone tests: deep copy, custom name, no published version
[MOD]  RubricControllerTest     — clone endpoint test
```

Run:

```bash
rtk bash ./mvnw -Dtest=RubricControllerTest,RubricServiceImplTest test
```

Commit:

```text
feat(rubric): clone rubric with versions and criteria
```

#### Step 10.5 — System template seeder

Code:

```text
[NEW]  config/RubricTemplateSeeder.java   — CommandLineRunner, seeds 4 default templates if none exist
```

Test:

```text
[NEW]  RubricTemplateSeederTest   — seeds on empty, skips when templates exist
```

Run:

```bash
rtk bash ./mvnw -Dtest=RubricTemplateSeederTest test
```

Commit:

```text
feat(rubric): seed system rubric templates on first boot
```

#### Step 10.6 — Update evaluation run (remove rubric project check)

Code:

```text
[MOD]  evaluation/service/impl/EvaluationRunServiceImpl.java   — findRubricVersion no longer checks project, only createdBy
```

Test:

```text
[MOD]  EvaluationRunServiceImplTest   — update rubric version lookup tests
[MOD]  EvaluationRunControllerTest    — must still pass
```

Run:

```bash
rtk bash ./mvnw -Dtest=EvaluationRunControllerTest,EvaluationRunServiceImplTest test
```

Commit:

```text
refactor(evaluation): allow cross-project rubric versions in evaluation runs
```

#### Step 10.7 — Rubric sub-resource regression

No code changes. Verify rubric version and criterion endpoints still work.

Run:

```bash
rtk bash ./mvnw -Dtest=RubricVersionControllerTest,RubricVersionServiceImplTest,RubricCriterionControllerTest,RubricCriterionServiceImplTest test
```

If pass → proceed. If fail → fix and amend previous commit.

---

### 11. Quick Evaluate.

#### Step 11.1 — Auto-resolve service logic

Code:

```text
[NEW]  evaluation/request/QuickEvaluateRequest.java             — record(datasetPublicId?, connectorPublicId?, rubricVersionPublicId?, maxConcurrency?)
[MOD]  evaluation/service/EvaluationRunService.java             — add quickEvaluate method
[MOD]  evaluation/service/impl/EvaluationRunServiceImpl.java    — auto-resolve: find sole APPROVED dataset, active connector, latest PUBLISHED rubric version; delegate to createEvaluationRun
[MOD]  exception/ErrorCode.java                                — add QUICK_EVALUATE_AMBIGUOUS (422)
```

Test:

```text
[MOD]  EvaluationRunServiceImplTest   — auto-resolve: 1 dataset/connector/rubric → success; 0 or >1 → 422
```

Run:

```bash
rtk bash ./mvnw -Dtest=EvaluationRunServiceImplTest test
```

Commit:

```text
feat(evaluation): quick evaluate auto-resolve logic
```

#### Step 11.2 — Quick evaluate controller endpoint

Code:

```text
[MOD]  evaluation/controller/EvaluationRunController.java   — add POST /api/v1/projects/{projectPublicId}/quick-evaluate
```

Test:

```text
[MOD]  EvaluationRunControllerTest   — quick evaluate endpoint (202, 422 ambiguous)
```

Run:

```bash
rtk bash ./mvnw -Dtest=EvaluationRunControllerTest,EvaluationRunServiceImplTest test
```

Commit:

```text
feat(evaluation): add quick evaluate endpoint
```

---

### 12. Doc updates + full regression.

Update handoff docs:

```text
[MOD]  server/SERVER_CONTEXT.md   — update [CURRENT_STATE] Persistence, [API_CHANGE], [FUTURE_SLICE]
[MOD]  server/API_TREE.md         — update resource tree, domain relationships, main workflow, known gaps
[MOD]  server/API_TODO.md         — move new endpoints to Completed, update Next
[MOD]  server/API_PLAN.md         — move features to Completed, clear Current Slice
```

Run full regression:

```bash
rtk bash ./mvnw test
```

Commit:

```text
docs(server): update handoff docs for qc productivity features
```
