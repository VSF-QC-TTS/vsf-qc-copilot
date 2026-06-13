# Story 00.04b - Feedback And Skeleton Components

## Story Header

**As a** QC member,
**I want** clear loading, empty, and error placeholders,
**so that** I understand whether a screen is loading, empty, or unavailable.

## In Scope

- [WEB] `EmptyState`.
- [WEB] `LoadingSkeleton`.
- [WEB] Basic error panel primitive for route boundaries.

## Out of Scope

- Per-route `loading.tsx`; covered in Epic 12.

## Deferred

- None.

## API/Data Contract

- No backend contract.

## UI States

- Empty state can include optional action.
- Skeleton variants: text line, rectangular, circular, table rows.

## Acceptance Criteria

### AC 1 - Empty state supports action

```text
Platform: [WEB]
Actor: QC member

Given  a list has no records
When   the screen renders EmptyState
Then   it shows title, optional description, and optional action
```

### AC 2 - Skeleton matches container

```text
Platform: [WEB]
Actor: QC member

Given  data is loading
When   LoadingSkeleton renders
Then   it reserves stable dimensions for the target content
```

## Implementation Checklist

- Create `components/feedback/empty-state.tsx`.
- Create `components/feedback/loading-skeleton.tsx`.
- Ensure strings are passed from i18n callers.

## Verification Notes

- Check light/dark contrast.

