# Story 00.04a - StatusBadge

## Story Header

**As a** QC member,
**I want** statuses shown with consistent badges,
**so that** I can scan evaluation, job, dataset, and review states quickly.

## In Scope

- [WEB] `StatusBadge` supports all known backend enum labels.
- [WEB] Running states have restrained motion.

## Out of Scope

- Custom per-screen badge variants.

## Deferred

- None.

## API/Data Contract

Statuses include `PASS`, `FAIL`, `WARNING`, `ERROR`, `NOT_REVIEWED`, `NEED_FIX`,
`IGNORED`, `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `CANCELLED`, `DRAFT`,
`APPROVED`, `PUBLISHED`, `ARCHIVED`, `ACTIVE`, and `INACTIVE`.

## UI States

- Unknown status renders a neutral badge with the raw value.

## Acceptance Criteria

### AC 1 - Known statuses map to colors

```text
Platform: [WEB]
Actor: QC member

Given  a known backend status is rendered
When   StatusBadge receives that status
Then   the badge uses the documented semantic color
```

### AC 2 - Unknown status is safe

```text
Platform: [WEB]
Actor: QC member

Given  the backend returns an unexpected status
When   StatusBadge renders it
Then   the UI does not crash
And    the badge uses a neutral style
```

## Implementation Checklist

- Create `components/ui/status-badge.tsx`.
- Keep size variants `sm` and `md`.
- Add accessible text and avoid color-only meaning.

## Verification Notes

- Render every status in light and dark mode.

