# API Plan — Current Backend Slice Tracker

Date: 2026-06-15
Status: **Ready for next backend slice**

Purpose: keep the immediate backend plan short. `API_TODO.md` and `API_TREE.md` remain the source for completed endpoint inventory and resource relationships.

## Completed In Previous Sequences

1. Worker + Promptfoo mock executor.
2. QC Review APIs.
3. Evaluation result QC fields.
4. Export APIs and worker generation.
5. Real Promptfoo CLI integration.
6. Promptfoo Secret-Store.
7. Promptfoo Rubric Judge Mapping.
8. Bulk Import Test Cases.
9. AI Generate Dataset.
10. Rubric Decoupling from Project.
11. Quick Evaluate.
12. Promptfoo QC result mapping/progress hardening.
13. Promptfoo Red-team backend job/API.

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

## Current Slice: None

---

*(Ready for next slice planning)*
