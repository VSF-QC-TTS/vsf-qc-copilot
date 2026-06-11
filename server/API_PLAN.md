# API Plan — Phase 6: Worker + Promptfoo Mock (Step 10)

Date: 2026-06-11
Status: **IN PROGRESS**
Prerequisite: Steps 0–9 completed (6 evaluation/job API endpoints, 226 tests passing).

## Goal

Implement the async worker that consumes jobs from the Redis queue and executes evaluation runs.
In dev mode, use a mock executor that returns fake results.
In prod mode, delegate to the promptfoo CLI (placeholder, not wired to real promptfoo yet).

## Existing Infrastructure (already in codebase)

```
Config:
  PromptfooProperties     → vqc.promptfoo.mode (mock/cli), workDir, command, maxConcurrency
  WorkerProperties        → vqc.worker.enabled (true), queueKey (vqc:jobs:queue)
  RedisConfig             → StringRedisTemplate bean

Publisher:
  JobQueuePublisher       → RPUSH jobPublicId to Redis queue (called by EvaluationRunServiceImpl)

Entities:
  Job                     → id, publicId, jobType, status, resourceType, resourceId, projectId, ...
  JobEvent                → id, publicId, jobId, eventType, payloadJson, createdAt
  EvaluationRun           → id, publicId, projectId, datasetId, rubricVersionId, connectorId, status, ...
  EvaluationResult        → id, publicId, evaluationRunId, testCaseId, actualAnswer, judgeScore, ...

Enums:
  JobStatus               → PENDING, PROCESSING, COMPLETED, FAILED
  EvaluationRunStatus     → PENDING, RUNNING, COMPLETED, FAILED
  JudgeStatus             → PASS, FAIL, WARNING, ERROR

Repositories:
  JobRepository           → findByPublicIdAndCreatedBy, findByPublicId
  JobEventRepository      → findByJobIdOrderByCreatedAtAsc
  EvaluationRunRepository → findByPublicIdAndCreatedBy, findByJobPublicId
  EvaluationResultRepository → findByEvaluationRunId
```

## Implementation Steps

### Step 10a — PromptfooExecutor interface + MockPromptfooExecutor

Create:
- `evaluation/executor/PromptfooExecutor.java` — interface
- `evaluation/executor/PromptfooResult.java` — result record
- `evaluation/executor/MockPromptfooExecutor.java` — mock impl
- `evaluation/executor/CliPromptfooExecutor.java` — CLI placeholder

```
PromptfooExecutor interface:
  List<PromptfooResult> evaluate(List<TestCase> testCases, RubricVersion rubricVersion, TargetApiConnector connector)

PromptfooResult record:
  (Long testCaseId, String actualAnswer, BigDecimal judgeScore, JudgeStatus judgeStatus, String judgeReason, Integer latencyMs, String errorMessage)

MockPromptfooExecutor:
  - @Component + @ConditionalOnProperty(name = "vqc.promptfoo.mode", havingValue = "mock")
  - For each test case: generate random PASS/FAIL/WARNING (weighted 70/20/10)
  - Random judgeScore 0.0–1.0, simulated latencyMs 50–500ms
  - Sleep 100–300ms per case to simulate real execution
  - Return List<PromptfooResult>

CliPromptfooExecutor:
  - @Component + @ConditionalOnProperty(name = "vqc.promptfoo.mode", havingValue = "cli")
  - Log warning "CLI mode not yet implemented, returning empty results"
  - Return empty list (placeholder for future real integration)
```

Tests: `MockPromptfooExecutorTest` — verify result count, judge status distribution, latency range.
Verify: `./mvnw -Dtest=MockPromptfooExecutorTest test`

### Step 10b — EvaluationJobHandler

Create:
- `evaluation/handler/EvaluationJobHandler.java` — orchestrates a single evaluation job

```
EvaluationJobHandler:
  Dependencies: JobRepository, JobEventRepository, EvaluationRunRepository,
                EvaluationResultRepository, TestCaseRepository, PromptfooExecutor
  
  void handle(UUID jobPublicId):
    1. Find Job by publicId → update status PROCESSING, emit JobEvent("PROCESSING")
    2. Find EvaluationRun by job.resourceId → update status RUNNING
    3. Load test cases from dataset (run.datasetId) → only ACTIVE ones
    4. Load rubric version (run.rubricVersionId) and connector (run.connectorId)
    5. Call promptfooExecutor.evaluate(testCases, rubricVersion, connector)
    6. For each PromptfooResult → save EvaluationResult row
    7. Update job: progressCurrent++, emit JobEvent("PROGRESS", payload with current/total)
    8. When all done → update EvaluationRun status COMPLETED, Job status COMPLETED
    9. Emit JobEvent("COMPLETED")
    10. On exception → update both to FAILED, emit JobEvent("FAILED", error message)
```

Tests: `EvaluationJobHandlerTest` — verify happy path (status transitions, result rows created),
       failure path (exception → FAILED status), empty test cases edge case.
Verify: `./mvnw -Dtest=EvaluationJobHandlerTest test`

### Step 10c — JobWorker (Redis consumer)

Create:
- `job/worker/JobWorker.java` — Redis BLPOP consumer loop

```
JobWorker:
  Dependencies: StringRedisTemplate, WorkerProperties, EvaluationJobHandler

  @ConditionalOnProperty(name = "vqc.worker.enabled", havingValue = "true", matchIfMissing = true)
  Implements SmartLifecycle (or use @EventListener(ApplicationReadyEvent))

  Run loop in a daemon thread:
    while (running):
      result = redisTemplate.opsForList().leftPop(queueKey, Duration.ofSeconds(5))
      if result != null:
        try:
          evaluationJobHandler.handle(UUID.fromString(result))
        catch Exception:
          log.error("Failed to process job {}", result, e)
```

Tests: `JobWorkerTest` — verify BLPOP delegates to handler, handles parse errors gracefully.
Verify: `./mvnw -Dtest=JobWorkerTest test`

### Step 10d — Config profiles

Update:
- `application-dev.yml`: `vqc.promptfoo.mode: mock`, `vqc.worker.enabled: true`
- `application-prod.yml`: `vqc.promptfoo.mode: cli`, `vqc.worker.enabled: true`

### Step 10e — Commit + full verify

```
Verify all: ./mvnw -Dtest=MockPromptfooExecutorTest,EvaluationJobHandlerTest,JobWorkerTest test
Full suite: ./mvnw test
Commit: feat(worker): evaluation job worker with mock promptfoo executor
```

## Step 11 — Docs Update

After Step 10:
- `API_TODO.md`: move Worker to Completed, set QC Review as Next.
- `SERVER_CONTEXT.md`: remove worker from Known Gaps, add to CURRENT_STATE, update test count.
- `API_PLAN.md`: mark this plan as COMPLETED.
- Commit: `docs(server): update handoff docs for evaluation worker`

## File Tree After Completion

```
evaluation/
  controller/EvaluationRunController.java        ← existing
  entity/EvaluationRun.java                      ← existing
  entity/EvaluationResult.java                   ← existing
  enums/EvaluationRunStatus.java                 ← existing
  enums/JudgeStatus.java                         ← existing
  executor/PromptfooExecutor.java                ← NEW
  executor/PromptfooResult.java                  ← NEW
  executor/MockPromptfooExecutor.java            ← NEW
  executor/CliPromptfooExecutor.java             ← NEW
  handler/EvaluationJobHandler.java              ← NEW
  mapper/EvaluationRunMapper.java                ← existing
  repository/EvaluationRunRepository.java        ← existing
  repository/EvaluationResultRepository.java     ← existing
  request/CreateEvaluationRunRequest.java        ← existing
  response/*.java                                ← existing
  service/EvaluationRunService.java              ← existing
  service/impl/EvaluationRunServiceImpl.java     ← existing

job/
  controller/JobController.java                  ← existing
  entity/Job.java                                ← existing
  entity/JobEvent.java                           ← existing
  enums/*.java                                   ← existing
  repository/*.java                              ← existing
  response/*.java                                ← existing
  service/JobQueuePublisher.java                 ← existing
  service/JobService.java                        ← existing
  service/impl/JobServiceImpl.java               ← existing
  worker/JobWorker.java                          ← NEW
```
