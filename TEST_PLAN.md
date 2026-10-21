# Test plan

## Foundation evidence

Local clean build on Java 11 / RuneLite 1.13.3 passed: **27 JUnit tests, no failures/errors/skips**. A separate compatibility clean build against the retrieved Hub target, 1.13.2, also passed all 27 tests using a temporary dependency override. In-game acceptance and hosted CI remain pending. The launcher main method is not a test.

| Suite | Tests | Coverage |
| --- | ---: | --- |
| RunStateTest | 16 | Empty/versioned header, all states, valid restore, character identity, invalid schema/status/identity/times |
| FromScratchSidebarTest | 6 | Single registration, cleanup, reenable, help/disclosure, zero metrics, EDT guard |
| FoundationResourcesTest | 5 | Metadata, PNG format/dimensions/alpha, Java 11 bytecode, config default |

Headless Swing tests exercise the real panel and fake toolbar boundary without starting RuneScape. No game account or new test dependency is required.

Run `.\gradlew.bat clean build` or `bash ./gradlew clean build`. HTML results: `build/reports/tests/test/index.html`; XML: `build/test-results/test/`. CI uses Temurin 11 on Linux/Windows and the Wrapper; hosted execution is pending.

## M1 manual acceptance — user only

Launch `.\gradlew.bat run` and follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). Agents must not interact with RuneScape. Record OS, RuneLite version, UI scale, result, and redacted errors.

| Case | Steps | Expected |
| --- | --- | --- |
| Identity | Find plugin in development client | Correct name/description, no template greeting |
| Logged-out preview | Enable; click coin button before login | One navigation button, title, preview warning, six zero metrics, recent-acquisition message |
| Logged-in preview | Log in manually | Same preview; no run begins or game items/GP change |
| Cleanup | Disable while open | Navigation/panel removed without error |
| Repeated toggles | Enable/disable repeatedly, including rapid toggles | No duplicate/stale buttons; working panel when enabled |
| Help setting | Toggle Show foundation help | Explanation hides/shows; placeholder warning remains |
| Resize/scale | Resize client and change UI scale | Readable labels/amounts, vertical scrolling, recognizable icon |
| Restart | Close and relaunch | Clean preview; no claim of restored progression |

User screenshots may be added after removing account details. A clean JVM start is not acceptance. Do not mark M1 accepted or begin M2 without user confirmation and authorization.

## Future test matrix — not implemented yet

Test accounting independently of RuneLite. Use synthetic/recorded event batches for adapters and user-run acceptance for rendering/game behavior.

| Area | Required scenarios |
| --- | --- |
| Initialization M2 | Empty/populated bank, placeholders, legacy inventory/equipment/GP, login/bank-open requirements, missing containers, duplicate initialization, character/profile switch, confirmation/cancel |
| Ownership M3/M4 | New item and existing legacy type, multiple copies, deposit/withdraw, split/combine, equip/unequip, fully withdrawn bank stack retained elsewhere |
| Bank UI M3 | Tabs/search, placeholders, clipping/scroll, purple bottom-right count, normal count retained, gray zero quantity, scaling, bank close/plugin disable |
| Transfers M4 | Coalesced event orderings, duplicate batches, legacy transformations, partial visibility, unknown storage, uncertainty, no new wealth from movement |
| Offline M4 | Net increase/decrease, 100->130 sharks, existing virtual quantities, incomplete bank, preserve history, audit corrections, no fabricated gross history |
| Currency M5 | Earn/spend/reserve/refund, exact bounds/overflow rejection, insufficient funds, death debt, ordinary debt rejection, repayment, physical legacy cash never recredits spent virtual GP |
| Valuation M5 | Acquisition, pooled basis/rounding conservation, sales above/below basis, price fluctuation, consumption, untradeable/unpriced, cash/items once, buyback effects |
| GE M6 | Buy/sell, partial fills, committed goods/cash, completed/uncollected positions, tax gross/net once, cancellation/refund, collection deduplication, actual vs offer price, uncertainty |
| Buybacks M7 | Fire Cape, natural duplicate with old copy locked, multiple copies, Infernal price, Ancient Sceptre flat price, full catalog, Graceful complete/partial/natural pieces, Elite Void total, Barrows Gloves/Royal Seed Pod/tradeable exclusions, insufficient/negative GP, atomic crash recovery |
| Persistence M2/M9 | Logout/login, restart, characters/profiles, incomplete initialization, corruption/truncation, schema migration, backup/restore/reset, interrupted append/checkpoint/rename, filesystem fallback, idempotent replay |
| UI M8 | Examine location/quantity/GE/HA, zero/N/A, original message retained, duplicate prevention, outgoing chat untouched, totals, large numbers, timers/mobile disclosure, notification deduplication |

## Release gates

Every milestone: review changed APIs/dependencies for compliance, run meaningful tests and build, then ask the user to accept changed behavior in-game. Keep ACCOUNTING.md's invariant mapping current. Add tests with their implementation; do not assert nonexistent features.

M9 adds migration/recovery fixtures, full regression, bounded-work profiling, and user-run bank/UI/GE acceptance. M10 rechecks current Hub requirements, metadata, art, licensing, privacy, release notes, and approved source commit after all acceptance gates pass.
