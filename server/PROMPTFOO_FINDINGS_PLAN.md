# Promptfoo Findings and Plan

Date: 2026-06-12
Status: implemented and smoke verified. Keep this file as the planning/decision artifact for the real Promptfoo CLI slice.

## Process Reminder

- First read current handoff/context files.
- Then collect current implementation facts and official docs facts.
- Then write a plan in markdown.
- Only after approval, edit code. This slice has already passed review, implementation, focused tests, and a gated real CLI smoke test.
- Keep each backend slice focused and commit separately.

## Current Repo Facts

- Real Promptfoo CLI integration is implemented. `server/SERVER_CONTEXT.md`, `server/API_TODO.md`, and `server/API_PLAN.md` now track follow-up backend slices such as secret-store support and richer rubric judge mapping.
- Redis is the queue boundary between API server and worker:
  - API creates an evaluation job and publishes the job public ID.
  - Worker consumes the Redis queue and invokes Promptfoo after loading the job/run context.
  - Promptfoo itself is not a Redis consumer in this slice.
- Evaluation already runs async through Redis/job/worker:
  - `JobWorker` consumes job public IDs.
  - `EvaluationJobHandler` loads active test cases, calls `PromptfooExecutor`, writes one `EvaluationResult` per active test case, updates run/job counters, and emits job events.
- `PromptfooExecutor` is already a Strategy:
  - `MockPromptfooExecutor` is enabled by `vqc.promptfoo.mode=mock` and returns generated local results.
  - `CliPromptfooExecutor` is enabled by `vqc.promptfoo.mode=cli` and runs the local pinned Promptfoo CLI after the Redis-backed worker consumes an evaluation job.
- Current promptfoo config properties:
  - `vqc.promptfoo.mode`
  - `vqc.promptfoo.work-dir`
  - `vqc.promptfoo.binary-path`
  - `vqc.promptfoo.max-concurrency`
  - `vqc.promptfoo.max-eval-time-ms`
  - `vqc.promptfoo.per-test-timeout-ms`
- `application-dev.yaml` uses mock mode by default.
- `application-prod.yaml` uses CLI mode with the local pinned binary path, not global `promptfoo`.
- Target connector config stores placeholders such as `{{question}}`, `{{precondition}}`, `{{metadata}}`, and `{{secret:KEY}}`.
- Connector secrets are not stored in a real encrypted secret store yet. This is important because CLI execution cannot recover raw secret values from persisted connector config.
- Connector response extraction in current test-run flow only supports simple `$.answer`.

## Official Promptfoo Docs Facts

Sources:
- Command line docs: https://www.promptfoo.dev/docs/usage/command-line/
- HTTP/HTTPS provider docs: https://www.promptfoo.dev/docs/providers/http/
- Configuration guide: https://www.promptfoo.dev/docs/configuration/guide/

Relevant facts:
- `promptfoo eval` accepts `--config <paths...>` for config files.
- `promptfoo eval` accepts `--output <paths...>` and supports JSON output among other formats.
- `promptfoo eval` supports `--max-concurrency <number>`.
- `promptfoo eval` supports `--no-progress-bar` and `--no-table`, useful for non-interactive worker execution.
- Promptfoo may return exit code `100` when test cases fail, while exit code `1` is used for other errors. This means backend integration should not treat every non-zero exit as infrastructure failure without considering configured behavior.
- HTTP/HTTPS provider config supports:
  - `id: http` or `id: https`
  - `config.url`
  - `config.method`
  - `config.headers`
  - `config.body`
  - `config.queryParams`
  - `config.transformResponse`
- HTTP provider body can be a JSON object or a string.
- HTTP provider templates can render variables from test `vars`.
- Tests can define per-case `vars`.
- Tests can define `assert` entries such as `contains`; this is usable as a first-pass mapping from `groundTruth`.
- Promptfoo `transformResponse` expects a JavaScript expression using variables such as `json`, `text`, and `context`; it does not accept JSONPath directly.
- Promptfoo supports environment-variable references such as `{{env.API_TOKEN}}`, but this slice will not map persisted `{{secret:...}}` placeholders to environment variables.

## Main Design Questions Before Coding

1. How should secrets be handled in the first real CLI slice?
   - Decision: fail fast if persisted connector config contains `{{secret:...}}`.
   - Do not write secrets into generated Promptfoo config.
   - Do not map secrets to environment variables until encrypted secret storage exists.

2. How strict should failed assertions be?
   - Decision: exit code `0` means completed; exit code `100` also means completed if `results.json` exists.
   - Missing `results.json`, timeout, command-not-found, malformed JSON, validation failure, or other non-zero exit codes are job failures.

3. What output JSON shape should the parser target?
   - Decision: parse Promptfoo JSON from `results.results[]` for `promptfoo@0.121.15`, while keeping parser compatibility with `results.outputs[]`.
   - Parser maps each output into internal `PromptfooResult`.
   - Malformed or missing output raises `PromptfooExecutionException`.

4. Should the backend generate JSON config or YAML config?
   - Decision: generate JSON files to avoid adding YAML dependencies.

5. How much rubric logic belongs in Promptfoo config now?
   - Decision: use ground truth assertions as the first slice.
   - Full rubric/criteria scoring is a follow-up slice.

## Implemented Plan

### Phase 0 - Runtime Pinning

- Local runner exists under `tooling/promptfoo-runner`.
- Promptfoo is pinned to `promptfoo@0.121.15`.
- `package.json` and `package-lock.json` are committed for the pinned runner.
- Backend must call the local binary from `tooling/promptfoo-runner/node_modules/.bin/promptfoo`.
- Do not use global `promptfoo`; do not use `promptfoo@latest`.
- Node must satisfy Promptfoo requirements; prefer Node 22+ for the demo environment.

### Phase 1 - CLI Executor Skeleton

- Keep Promptfoo execution behind `PromptfooExecutor`; change the internal executor contract if needed so it receives `EvaluationRun` context.
- Implement `CliPromptfooExecutor` behind `vqc.promptfoo.mode=cli`.
- Add properties only if needed:
  - `maxEvalTimeMs`
  - `perTestTimeoutMs`
  - keep `maxConcurrency`
- For each evaluation job, create a unique run directory at `vqc.promptfoo.work-dir/{runPublicId}`.
- Set per-run environment:
  - `PROMPTFOO_CONFIG_DIR={runDir}/.promptfoo`
  - `PROMPTFOO_LOG_DIR={runDir}/logs`
  - `FORCE_COLOR=0`
- Store generated artifacts in the run directory:
  - `promptfooconfig.json`
  - `tests.json`
  - `results.json`
  - `validate.stdout.log`
  - `validate.stderr.log`
  - `eval.stdout.log`
  - `eval.stderr.log`

### Phase 1.5 - Validate Before Eval

- Run config validation before eval:

```bash
"$PROMPTFOO_BIN" validate config --config "$RUN_DIR/promptfooconfig.json"
```

- Validation failure is treated as job failure.
- Save validation stdout/stderr under the run directory.
- Only run `promptfoo eval` after validation succeeds.

### Phase 1.6 - Eval Command

- Run eval with this command shape:

```bash
FORCE_COLOR=0 \
PROMPTFOO_CONFIG_DIR="$RUN_DIR/.promptfoo" \
PROMPTFOO_LOG_DIR="$RUN_DIR/logs" \
PROMPTFOO_MAX_EVAL_TIME_MS="$MAX_EVAL_TIME_MS" \
PROMPTFOO_EVAL_TIMEOUT_MS="$PER_TEST_TIMEOUT_MS" \
"$PROMPTFOO_BIN" eval \
  --config "$RUN_DIR/promptfooconfig.json" \
  --output "$RUN_DIR/results.json" \
  --no-progress-bar \
  --no-table \
  --no-cache \
  --max-concurrency "$MAX_CONCURRENCY"
```

- Use `--no-cache` by default to avoid stale API responses.
- Consider `--no-write` only if Promptfoo local history/view is not needed for debugging.

### Phase 2 - Config Generation

- Generate Promptfoo config from:
  - target connector URL/method/headers/body/query params
  - test case question/precondition/metadata/groundTruth
  - allow-listed response selector mapping:
    - `$.answer` -> `json.answer`
    - `$.data.answer` -> `json.data.answer`
- Preserve current async job model; do not call Promptfoo from HTTP request handlers.
- Unsupported response selectors fail fast with a clear error.
- Do not attempt advanced JSONPath support in this slice.
- If connector config contains `{{secret:...}}`, fail fast with a clear error.
- First-pass assertion mapping:
  - If `groundTruth` exists, add a simple assertion.
  - If no `groundTruth`, still run the case and parse output, marking status according to Promptfoo result where possible.

### Phase 3 - Output Parsing

- Parse JSON output from `promptfoo@0.121.15`:

```json
{
  "results": {
    "results": []
  }
}
```

- Map each result row into existing `PromptfooResult`, extracting when available:
  - test case id
  - actual answer
  - score
  - status
  - reason
  - latency
  - error
  - raw JSON
- Store raw per-case JSON for traceability.
- If a case is missing from output, keep existing `EvaluationJobHandler` behavior: write an `ERROR` result for that case.
- Malformed or missing output raises `PromptfooExecutionException`.

### Phase 4 - Tests

- Add unit tests for config generation.
- Add unit tests for output parsing using the real `results.results[]` fixture shape and compatibility coverage for `results.outputs[]`.
- Add a gated real smoke test: `JobWorker -> EvaluationJobHandler -> CliPromptfooExecutor -> promptfoo@0.121.15 -> local HTTP target`.
- Add process execution tests using a fake local command/script, not real network or real Promptfoo.
- Add tests for:
  - config validation command success/failure
  - local binary path usage
  - per-run environment variables
  - `$.answer` -> `json.answer`
  - `$.data.answer` -> `json.data.answer`
  - unsupported selector fail-fast
  - secret placeholder fail-fast
  - exit code `100` with valid `results.json` is parsed
  - exit code `100` without `results.json` fails
  - `--no-cache` is included
  - mock mode remains unchanged
- Extend focused suite:
  - `MockPromptfooExecutorTest`
  - new `CliPromptfooExecutorTest`
  - `EvaluationJobHandlerTest`
  - `JobWorkerTest`

### Phase 5 - Docs and Handoff

- Update `server/API_PLAN.md`.
- Update `server/API_TODO.md`.
- Update `server/SERVER_CONTEXT.md`.
- Update `server/API_TREE.md` only if workflow wording changes.
- Keep and commit `server/PROMPTFOO_FINDINGS_PLAN.md` as a planning artifact.
- Document remaining gaps clearly:
  - secret storage/export to Promptfoo env
  - full rubric criteria scoring
  - non-`$.answer` selectors beyond simple paths
  - timeout/retry behavior alignment with connector config

## Recommended First Slice

Implement CLI mode for no-secret HTTP connectors using generated JSON config, local pinned `promptfoo@0.121.15`, config validation, isolated run directories, captured logs, `--no-cache`, and `results.results[]` parsing. Keep mock mode unchanged. Treat secret placeholder support, advanced selectors, and advanced rubric scoring as follow-up slices.
