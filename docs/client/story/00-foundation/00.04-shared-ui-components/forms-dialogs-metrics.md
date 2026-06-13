# Story 00.04c - Forms, Dialogs, And Metrics

## Story Header

**As a** QC member,
**I want** consistent forms, confirmations, uploads, and metrics,
**so that** repeated workflows feel predictable across the dashboard.

## In Scope

- [WEB] `FormField`.
- [WEB] `ConfirmDialog`.
- [WEB] `MetricCard`.
- [WEB] `FileUploadDropzone`.

## Out of Scope

- Entity-specific forms.
- DataTable controls.

## Deferred

- None.

## API/Data Contract

- File upload dropzone must allow `.xlsx` and `.csv` configuration and 5 MB max
  for test-case import callers.

## UI States

- Form errors appear below fields.
- Confirm dialog supports destructive and default variants.
- Metric card supports loading/empty value display.

## Acceptance Criteria

### AC 1 - Field labels are accessible

```text
Platform: [WEB]
Actor: QC member

Given  a form field is rendered
When   the user focuses the input
Then   the visible label is associated with the control
And    helper or error text is announced when present
```

### AC 2 - Confirm dialog blocks accidental destructive actions

```text
Platform: [WEB]
Actor: QC member

Given  a destructive action is requested
When   ConfirmDialog opens
Then   the action does not run until the user confirms
```

## Implementation Checklist

- Create `components/forms/form-field.tsx`.
- Create `components/ui/confirm-dialog.tsx`.
- Create `components/ui/metric-card.tsx`.
- Create `components/forms/file-upload-dropzone.tsx`.

## Verification Notes

- Verify mobile touch targets are at least 44px where applicable.

