# Story 00.04 - Shared UI Components

> Epic: 0 - Foundation
> Depends on: 00.01
> Backend contract source: `docs/client/MASTER_PLAN.md`

## Story Header

**As a** frontend implementation agent,
**I want** reusable dashboard primitives,
**so that** feature screens are consistent and do not reimplement common UI behavior.

## In Scope

- [WEB] Status and metric primitives.
- [WEB] Feedback states.
- [WEB] Form, dialog, and confirmation primitives.

## Out of Scope

- DataTable; covered by story 00.05.
- Layout shell; covered by Epic 2.

## Deferred

- None.

## Child Files

1. [Status badge](status-badge.md)
2. [Feedback and skeleton](feedback-and-skeleton.md)
3. [Forms, dialogs, and metrics](forms-dialogs-metrics.md)

## Acceptance Criteria

### AC 1 - Component groups are implemented

```text
Platform: [WEB]
Actor: Frontend implementation agent

Given  the shared UI story is complete
When   feature screens import shared primitives
Then   status, feedback, form, dialog, and metric components are available from stable paths
```

## Implementation Checklist

- Complete each child markdown file in listed order.
- Export shared primitives from stable component paths.
- Keep user-facing strings provided by callers through i18n.

## Verification Notes

- Verify all child files before marking story 00.04 complete.
