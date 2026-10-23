# Historical development notes

These files preserve milestone reports, old design decisions, test evidence and superseded instructions. **Statuses, paths, setup commands and proposed workflows describe their original point in development. They are not current requirements.**

Milestones 1 through 9.2 are owner-accepted. Gameplay is frozen at 9.2. Milestone 10 prepares an unpublished beta and documentation; Milestone 11 will handle official submission and publication. Old milestone tags are retained even where they point to an earlier commit.

Use the [documentation index](../README.md), [current architecture](../technical/ARCHITECTURE.md), [accounting policy](../technical/ACCOUNTING.md), [validation plan](../technical/TEST_PLAN.md) and [installation guide](../INSTALLATION.md) for current behavior.

## Archive contents

- `MILESTONE_1.md` through `MILESTONE_9_2.md`, associated correction reports and `POST_MILESTONE_7.md`: development chronology, including experiments later replaced.
- `ARCHITECTURE_PRE_M10.md`, `ACCOUNTING_PRE_M10.md`, `TEST_PLAN_PRE_M10.md`: the accumulated technical documents before this cleanup. Their historical sections remain available in full; current concise guides replace them.
- `AGENTS_PRE_M10.md`: prior project instructions, including superseded milestone restrictions. Root [AGENTS.md](../../AGENTS.md) remains the tooling entry point.
- `MILESTONE_10_PREPARATION.md`: the initial beta preparation and CI investigation record. The [current M10 report](../MILESTONE_10.md) records the subsequently passing CI and final preparation.
- `COMPATIBILITY.md`: earlier platform/build acceptance notes, not current CI status.
- `INSTALLATION_SCRIPTS_10_5.md` and `RELEASE_DRAFT_SCRIPTS_10_5.md`: the superseded CMD/PowerShell distribution. Current installation uses the native setup EXE; the old ZIP is preserved.
- `*_MULTI_CONTAINER.md`: retired pre-bank-only architecture and tests. Inventory/equipment ownership tracking must not be reconnected to production.

In particular, the old multi-step Rune Pouch preparation wizard and earlier Virtual Bank disable/defer decisions are superseded. Current automatic contents adjustments and accepted native bank filtering remain unchanged.
