# Epic 0 - Foundation

Epic 0 creates the client shell, API contract layer, shared UI primitives, i18n,
theme support, and job progress infrastructure. It must be completed before any
feature screen starts.

## Build Order

1. [00.01 Project scaffolding](00.01-project-scaffolding.md)
2. [00.02 API client layer](00.02-api-client-layer.md)
3. [00.03 Auth store](00.03-auth-store.md)
4. [00.04 Shared UI components](00.04-shared-ui-components/README.md)
5. [00.05 Data table](00.05-data-table.md)
6. [00.06 Theme and dark mode](00.06-theme-dark-mode.md)
7. [00.07 i18n setup](00.07-i18n-setup.md)
8. [00.08 Job progress polling](00.08-job-progress-polling.md)

## Epic Contract

- Use current backend contracts from `docs/client/MASTER_PLAN.md`.
- Access token remains memory-only.
- Paginated responses use `items` and `totalItems`.
- Job progress is polling-first; SSE is deferred.

