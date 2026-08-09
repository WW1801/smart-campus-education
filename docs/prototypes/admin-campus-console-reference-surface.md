# Administrator campus console

## Scope and mode

`frontend/views/dashboard/Index.vue` is the production administrator dashboard in Operate mode. The related application shell is `frontend/views/Home.vue`; the visual reference remains `docs/prototypes/admin-campus-console-reference.html`.

## Audience and job

System administrators first identify high-risk or unresolved teaching-administration work, then open the matching existing workflow. Current metrics and warning rows must come from the existing API rather than prototype demonstration values.

## Direction and memorable moment

The page is a ledger-first campus console: a numbered deep-ink navigation index frames a cool mist workspace, with a continuous white task ledger leading the viewport and operational metrics following it as one divided strip.

## Constraints and unresolved decisions

- Preserve existing routes, role guards, JWT behavior, AI analysis, password change, and logout flows.
- Use Element Plus for production controls and feedback.
- The current API has no task deadline or historical metric series. The UI must state that overdue status and trends cannot yet be calculated instead of inventing values.
