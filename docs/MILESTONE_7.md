# Milestone 7 — Legacy Buybacks

By **Pilots r kwl**. M6 is user accepted at `v0.6.0-grand-exchange` / `90bbee1`. The original M7 baseline is user accepted. The [post-M7 refinement](POST_MILESTONE_7.md) supersedes the original layout and pouch-content scope below and awaits separate manual acceptance. No M8, gameplay automation, inventory/equipment accounting, cloud service or publication. No real player save was read or changed during development.

## Design and implementation

`buyback/BuybackCatalog` is an explicit RuneLite **1.13.3** gameval allowlist with stable catalog/role IDs. `BuybackState` holds catalog version, one-copy role entitlements, tier, unique purchase IDs, fixed payments, dates and the verified migration archive reference. Its constructor replays the small curated purchase list to verify every price, tier and cumulative GP amount. No arbitrary item purchases, vouchers, notes, placeholders, dummy/minigame replacements, broken equipment or max-cape substitutions qualify.

`BuybackEngine` validates active/anchored state, physical bank presence, remaining locked stock and available GP. The live controller additionally checks `ItemComposition.isTradeable`, `isGeTradeable`, `getNote` and `getPlaceholderTemplateId` on the client thread, failing closed for unavailable definitions. Each purchase is revision-bound and requires a fresh complete bank equal to the committed physical cursor. Confirmation/cancel never operates OSRS. A purchase revalidates its eligibility/funds again at the repository boundary.

One `BUYBACK` bank-journal operation contains the purchase ID, catalog ID, exact payment and timestamp. Raw P/B and the immutable initial bank stay unchanged; payment, entitlement and history publish together only after the existing forced journal/checkpoint/current protocol acknowledges success. A failed write keeps the old renderer/UI authority. Repeated confirmation, stale revision, conflicting client and repeated entitlement attempts cannot pay twice. The same serial storage worker handles bank and GE events. No network, filesystem work, item pricing or ledger reconstruction occurs in rendering.

## Complete fixed-price catalog

| Combat & Utility | GP |
| --- | ---: |
| Fire Cape | 1,000,000 |
| Infernal Cape | 15,000,000 |
| Imbued Saradomin / Guthix / Zamorak / Ancient Cape | 750,000 each |
| Ava's Assembler | 1,000,000 |
| Dragon Defender | 250,000 |
| Fighter Torso | 2,000,000 |
| Salve Amulet (ei) | 500,000 |
| Ancient Sceptre | 500,000 flat; no component fee |
| Rune Pouch / Divine Rune Pouch | 1,000,000 once, shared |

| Void Knight | GP |
| --- | ---: |
| Melee / Ranger / Mage Helm | 300,000 each |
| Top / Robe | 800,000 each |
| Gloves | 200,000 |
| Elite Top / Elite Robe outright | 1,800,000 each |
| Elite upgrade after corresponding paid base | 1,000,000 each |

One-helm regular set is 2.1M; Elite is 4.1M. A base and Elite form share one copy budget. An upgrade raises its permission tier and favors the physically present Elite copy; it does not add a second copy. Owning an Elite entitlement also covers its base form. No downgrade is performed or required.

| Complete Skilling Outfit | GP | Required roles |
| --- | ---: | ---: |
| Graceful | 1,000,000 | 6 |
| Prospector | 1,000,000 | 4 |
| Angler | 750,000 | 4 |
| Rogue | 500,000 | 5 |
| Lumberjack | 500,000 | 4 |
| Farmer's Outfit | 750,000 | 4 |

Every role needs a qualifying banked locked copy. A missing piece rejects the entire purchase. Naturally positive additional copies remain independent; the purchase releases only one legacy copy per role. There are no individual Graceful purchases. Supported recolors share the role and cannot multiply the grant. Barrows Gloves and Royal Seed Pod are excluded. Future catalog additions require an explicit version/policy change.

## Pouch family and copy allocation

`rune_pouch_family` explicitly contains BH_RUNE_POUCH **12791**, BH_RUNE_POUCH_TROUVER **24416**, DIVINE_RUNE_POUCH **27281**, DIVINE_RUNE_POUCH_TROUVER **27509**. The tradeable Rune pouch note **24587** and minigame replacement pouches are excluded. One payment creates one family role with stored quantity **1**, not four item grants. Either physical form qualifies. The original M7 build did not read contents; the post-M7 safeguard now reads reported varbits on explicit request and requires player verification. No physical contents are modified by the plugin.

For a purchased role, aggregate raw B across its allowlisted variants as R. Store a nonpositive bank floor f=min(0,R) at purchase. Subsequent bank/correction operations advance f=max(f,min(0,R)); they never lower it. The paid allowance is one when R>=f, otherwise 1+(R-f). Nonpositive remainder remains signed; positive allowance is capped at one and actual bank capacity. Allocate natural positive aggregate units first, then that paid allowance, favoring the purchased Void tier. Equal-tier choices use deterministic item-ID order. Later raw reductions consume virtual units first. Upgrades preserve the existing floor and cannot refill an already-withdrawn paid copy.

This floor matters when the player already withdrew other legacy copies: L4/P1/B-3, then buy one remaining cape, displays **1**, while raw B remains -3. Returning the other three raises the floor toward zero but still displays **1**, never four paid copies. Withdrawing one again displays zero. A normal bought pouch withdrawn from P1/B0 becomes P0/B-1 with displayed quantity zero; returning it, or the Divine form, restores one allowance. Standard-to-Divine and reverse substitutions keep one payment, even across restart. If both forms exist, the paid allowance remains one; extra genuinely positive bank progress can independently support additional physical copies. Every floor transition shares the existing atomic bank checkpoint and is independently checked by persistence.

Negative remainder stays associated with an actual present variant or a previously negative variant when absent, so native placeholders/search retain the deficit. No physical item or slot is invented. Definitions and bank-only observations cannot identify identical instances or prove how a transformation occurred outside the bank; this is a documented deterministic honor-system allocation. The original raw exact-ID P/B history is retained.

## GP, wealth and statistics

```
raw GP       = B(COINS)
committed GP = sum(unique persisted buyback payments)
available GP = raw GP - committed GP
```

All arithmetic is checked long arithmetic. Insufficient/negative available GP blocks purchases. Normal bank movements can still lower raw/available GP. Corrections never refund or delete commitments. Real coins are untouched.

The Bank GP tile and coin overlay show **available GP**. Its tooltip retains exact raw/committed amounts. Other raw B values retain the accepted equation; approved item overlays use the bounded paid-role projection above. Current Bank Net Worth values this projection with cached current prices, including available coins **once**. No second expenditure subtraction and no market value invented from a buyback price. Missing untradeable prices remain missing/priced-subtotal evidence. Buybacks do not change historical gain quotes, high-water marks or GE records. Total Value Lost / Spent adds committed buyback GP once to explicit expenses and separately estimated GE fees.

The accepted bank-only Items Unlocked metric was current positive IDs, not reconstructed lifetime acquisitions. M7 preserves that natural-progress metric for other IDs and counts each durable purchased equipment role once, including when withdrawn. Natural and purchased variants within a paid role do not count again; the pouch family is one role and a full Graceful set six. No missing lifetime acquisition data is invented.

Manual Bank Corrections still edit **raw** bank progress. A short editor note explains that paid allowances and GP commitments remain separate. Corrections do not rewrite the initial baseline or refund purchases. There are no automatic refunds or entitlement revocations in this milestone.

## UI

After the post-M7 refinement, Legacy Buybacks sits directly below statistics and above Played Elsewhere, Grand Exchange, Bank History and reset, with two matching cached-sprite tiles: Buyback Shop (Fire Cape) and Buyback History (Rune pouch). Shop has name search, three categories, Available GP, actual item sprites, dynamic upgrade price and explicit eligibility status. Both pouch names find the same entry. Purchase confirmation shows sprite/name, exact price, available/remainder and concise wrapped explanation. The pouch-specific confirmation explains the one-payment upgrade/revert coverage. Double confirmation is guarded; Cancel/window-close writes nothing. Confirm closes the shop; reopen it after save for fresh eligibility/budget. A stale shop request is rejected rather than overwriting newer data.

Separate read-only purchase history shows one row per payment, cached sprite, name/family, integer GP price, piece count and local readable date. It is naturally bounded by the 26 curated catalog entries; no thousands-row journal scan is needed. Bank and GE history remain separate. All existing config keys, green quantities, neutral tint, partial-row clipping, placeholders, navigation artwork and setup/reset safeguards remain.

## Schema and recovery

Format **8** adds required buyback state and optional immutable purchase evidence to audits. Entitlement JSON records explicit quantity=1, permission tier and paid-copy bank floor; purchase evidence retains its original role floors. A genuine format-7 load verifies its full original chain, archives every original JSON checkpoint/ledger byte-for-byte with forced hash-verified manifest, then appends BUYBACK_BASELINE. Run ID/start, initial bank, raw P/B, financial quotes/high-water/expenses, GE state/history and corrections remain unchanged. No past buybacks are inferred and no new bank anchor is needed. Formats 1–6 retain their prior verified archive/migration rules. No player files are stored in Git.

Existing Filepath locking, serial background saves, CAS, head verification, interrupted-write recovery and context/character guards remain. Load verifies the M7 archive as well as older archive references. Missing/corrupt/newer payloads do not default to empty spending or a new run. Reset still requires typed confirmation and successful archive; no reset was performed. Back up the entire character directory with the client closed, including all linked segments and nested archives.

## Validation and performance

Final `.\gradlew.bat clean build`: **BUILD SUCCESSFUL**, 1m 40s, seven tasks. **802 passed, 0 failures, 0 errors, 0 skipped, 57 suites**: all 723 earlier tests plus 79 M7 cases. Existing Gradle-9 deprecation and test-launcher unchecked warnings remain; Java 11 target/Wrapper/dependencies are unchanged. `git diff --check` is clean. Tests use synthetic temporary Filepath roots, fake source definitions and no OSRS account/network. Actual API definition checks, item sprites, bank lifecycle, UI scaling and native responsiveness require user-only acceptance. A clean build is not live acceptance.

| Synthetic workload | Final measurement |
| --- | --- |
| 1,200+ IDs, all 26 purchases, 250 projections | 46.153 ms total (~0.185 ms/projection); projection is published per committed update, never recomputed per rendered item/frame |
| Same bank and purchase ledger, 64 forced bank commits | 4,909.291 ms worker wall; 2,625.000 ms worker CPU; 2,284.291 ms combined IO/scheduling remainder |
| Retained bank regression, 1,200 IDs / 256 forced commits | 17,612.591 ms worker wall; 4,375.000 ms CPU; 100 aggregate captures 23.027 ms |
| Retained dashboard regression, 100 updates / 1,200 IDs | 6.866 ms EDT wall; Windows CPU sample below timer resolution |
| Retained GE regression, 128 forced commits / 1,200 IDs | 6,801.181 ms worker wall; 2,625.000 ms CPU; 30 retained rows |
| Retained wealth regression, 96 forced commits / 1,296 financial events | 6,468.582 ms worker wall; 3,375.000 ms CPU; 100 cached valuations 24.988 ms |

These uncontrolled local filesystem/JIT samples are not native bank latency, FPS or direct disk-only timings. CPU timers are quantized; wall-minus-CPU includes scheduling. There is no synchronous file/network work added to the critical client/render path. Compare real enabled/disabled banking during user acceptance; no live performance guarantee is claimed.

New suites: BuybackModelTest, BuybackPipelineTest, BuybackPersistenceTest, BuybackUiTest, BuybackPerformanceTest. They cover prices, explicit copy budgets, pouch direction/locked variants/note exclusion, natural copies, Void upgrade totals, complete outfit atomicity, unchanged L/P/B/financial/GE history, live definition rejection, revision-bound controller/save/overlay ordering, duplicate confirmation, outside-bank restart, correction/spending persistence, corrupt/missing data, exact M6 migration, stale clients, failed/lost-ack writes, catalog/history/dashboard and a 1,200-ID workload with all 26 purchases. Earlier format-5/6 fixtures now explicitly remove the new field when constructing genuine historical bytes; earlier behavior assertions remain.

## User-only manual acceptance

1. Keep your accepted run. Back up its complete local character directory with RuneLite closed. Launch `.\gradlew.bat run` from the project root. Follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). Only the user operates RuneScape.
2. Enable From Scratch. Open your normal bank alone. Confirm existing balances, financial/GE histories, corrections and start date survive the one-time format-8 migration; no inventory/equipment/GE walkthrough or reset should appear.
3. Open Legacy Buybacks. Check category/name filters, sprite clarity, available budget and one shared pouch entry. Missing physical copies, note-only pouch, missing outfit pieces and insufficient GP must not purchase. Cancel a confirmation and verify unchanged counters/history/GP.
4. With a locked banked Fire Cape and enough available GP, inspect confirmation, approve once, and wait for durable acknowledgment. Counter becomes one; real quantity/coin stack stay unchanged. Available GP and worth fall by 1M once; Lost / Spent rises by 1M; gained value is unchanged. Repeat/double-click must not pay again. Reopen the shop for current budget.
5. Test the pouch form you own. Purchase once for 1M. Inspect one history row/one role. Manually withdraw/redeposit: counter 1→0 while absent→1. Upgrade/revert through ordinary gameplay when convenient and bank the result: one available copy, same payment. The other nonexistent form gets no synthetic bank slot. Check genuinely locked variants only if you already own them; never buy extra items just for testing. Use the post-M7 preparation procedure before any new pouch purchase; the plugin never moves or changes physical runes.
6. If you already possess the relevant items, check regular/Elite Void prices and base-to-Elite additional 1M; only one normal/Elite paid copy is available at a time. Test a full outfit only when intentionally willing to spend its virtual GP; incomplete/canceled purchases must not charge or partially unlock.
7. Naturally earn/deposit another copy when practical: its positive bank progress is independent of the one paid allowance. Withdraw/redeposit mixed copies to check virtual-first reductions. If you already have several qualifying legacy copies, withdraw all but one before buying it: the remaining copy must unlock; returning the older withdrawals must not multiply the paid grant. With multiple pouch forms, confirm at most one **paid** copy is allocated.
8. Close/reopen bank, relog, restart and disable/re-enable. Entitlements/payments/history persist; returning a previously withdrawn item still works. Inspect absent negative variants in Manual Bank Corrections; native placeholders retain original mapping. Ordinary corrections never refund commitments.
9. Recheck GE buy/sell/fill/collection then banking: no extra item/GP debit, no duplicate fee or new financial gain from the buyback itself. Perform normal deposits/withdrawals, rapid scrolling/search/tabs, top/bottom clipped rows, dragging, resizing and custom colors. Compare native bank response with the plugin disabled; report any regression with opt-in sampled diagnostics.
10. Open/cancel reset only; do not reset the accepted run to test this milestone. Confirm the existing typed RESET safeguard remains.

## Known limits and publication considerations

- Bank-only quantities do not prove individual-copy provenance, out-of-bank holdings/consumption or arbitrary transformations. Paid family substitutions are restricted to the explicit allowlist. Existing raw high-water financial estimates remain bank-based and are not a full lifetime acquisition engine.
- One paid copy per curated equipment role; no repeated-copy shop, refund or entitlement-revocation editor. A changed/corrected raw baseline can affect allocation; the original payment remains auditable and never creates an absent item.
- Broken/mangled, minigame replacements, max-cape combinations and unlisted upgrades are not implicitly supported. The appended exact ID table is authoritative for catalog version 1. Additional variants require verification and tests.
- Untradeables can have no verified market price. The shop price never supplies a market quote. GE tax/provenance/offline limits from M6 are unchanged.
- No new dependencies, remote requests, menu manipulation, input injection, game actions, widget/sprite mutation or special-storage reads. ItemComposition/ItemManager, Swing and existing Filepath APIs are supported public APIs; Plugin Hub review remains a future approval, not a guarantee.

## Verified catalog-version-1 item IDs

These exact constants and item names were read from the resolved RuneLite 1.13.3 source JAR. Runtime definition validation is an additional safeguard. Unlisted items never qualify. Tier 2 is the Elite Void upgrade; each role has quantity one.

| Entitlement role | Tier | Verified eligible item IDs |
| --- | ---: | --- |
| fire_cape | 1 | 6570, 24223 |
| infernal_cape | 1 | 21295, 24224 |
| saradomin_cape | 1 | 21791, 24248 |
| guthix_cape | 1 | 21793, 24249 |
| zamorak_cape | 1 | 21795, 24250 |
| ancient_cape | 1 | 34633, 34637, 34639 |
| assembler | 1 | 22109, 24222 |
| defender | 1 | 12954, 19722, 24143, 27008 |
| torso | 1 | 10551, 24175, 28067, 28069 |
| salve_ei | 1 | 12018, 25278 |
| ancient_sceptre | 1 | 27624, 27626 |
| rune_pouch_family | 1 | 12791, 24416, 27281, 27509 |
| void_melee | 1 | 11665, 24185, 26477, 27007 |
| void_ranger | 1 | 11664, 24184, 26475, 27006 |
| void_mage | 1 | 11663, 24183, 26473, 27005 |
| void_gloves | 1 | 8842, 24182, 26467, 27002 |
| void_top | 1 | 8839, 24177, 26463, 27000 |
| void_robe | 1 | 8840, 24179, 26465, 27001 |
| void_top | 2 | 13072, 24178, 26469, 27003 |
| void_robe | 2 | 13073, 24180, 26471, 27004 |
| graceful_hood | 1 | 11850, 13579, 13591, 13603, 13615, 13627, 13667, 21061, 24743, 25069, 27444, 30045 |
| graceful_cape | 1 | 11852, 13581, 13593, 13605, 13617, 13629, 13669, 21064, 24746, 25072, 27447, 30048 |
| graceful_top | 1 | 11854, 13583, 13595, 13607, 13619, 13631, 13671, 21067, 24749, 25075, 27450, 30051 |
| graceful_legs | 1 | 11856, 13585, 13597, 13609, 13621, 13633, 13673, 21070, 24752, 25078, 27453, 30054 |
| graceful_gloves | 1 | 11858, 13587, 13599, 13611, 13623, 13635, 13675, 21073, 24755, 25081, 27456, 30057 |
| graceful_boots | 1 | 11860, 13589, 13601, 13613, 13625, 13637, 13677, 21076, 24758, 25084, 27459, 30060 |
| prospector_hat | 1 | 12013, 29472 |
| angler_hat | 1 | 13258, 25592 |
| lumberjack_hat | 1 | 10941 |
| prospector_top | 1 | 12014, 29474 |
| angler_top | 1 | 13259, 25594 |
| lumberjack_top | 1 | 10939 |
| prospector_legs | 1 | 12015, 29476 |
| angler_legs | 1 | 13260, 25596 |
| lumberjack_legs | 1 | 10940 |
| prospector_boots | 1 | 12016, 29478 |
| angler_boots | 1 | 13261, 25598 |
| lumberjack_boots | 1 | 10933 |
| rogue_mask | 1 | 5554 |
| rogue_top | 1 | 5553 |
| rogue_trousers | 1 | 5555 |
| rogue_gloves | 1 | 5556 |
| rogue_boots | 1 | 5557 |
| farmer_hat | 1 | 13646, 13647 |
| farmer_top | 1 | 13642, 13643 |
| farmer_legs | 1 | 13640, 13641 |
| farmer_boots | 1 | 13644, 13645 |

## Files and Git status

Branch `master`, accepted HEAD `90bbee1`; no commit, push, tag, reset or publication. 
23
 tracked files modified and 
14
 new files; no staged changes. Build artifacts/private saves are absent from Git status.

Modified:

- `ACCOUNTING.md`
- `AGENTS.md`
- `ARCHITECTURE.md`
- `CHANGELOG.md`
- `README.md`
- `TEST_PLAN.md`
- `src/main/java/com/fromscratch/bank/BankActions.java`
- `src/main/java/com/fromscratch/bank/BankAudit.java`
- `src/main/java/com/fromscratch/bank/BankController.java`
- `src/main/java/com/fromscratch/bank/BankCorrectionDialog.java`
- `src/main/java/com/fromscratch/bank/BankEngine.java`
- `src/main/java/com/fromscratch/bank/BankPanel.java`
- `src/main/java/com/fromscratch/bank/BankRun.java`
- `src/main/java/com/fromscratch/bank/BankSource.java`
- `src/main/java/com/fromscratch/bank/BankTooltips.java`
- `src/main/java/com/fromscratch/bank/RuneLiteBankSource.java`
- `src/main/java/com/fromscratch/overlay/BankOverlayState.java`
- `src/main/java/com/fromscratch/persistence/BankRepository.java`
- `src/main/java/com/fromscratch/persistence/BankRunCodec.java`
- `src/main/java/com/fromscratch/wealth/BankValuation.java`
- `src/test/java/com/fromscratch/bank/BankFixtures.java`
- `src/test/java/com/fromscratch/persistence/FinancialPersistenceTest.java`
- `src/test/java/com/fromscratch/persistence/GePersistenceTest.java`

Added:

- `docs/MILESTONE_7.md`
- `src/main/java/com/fromscratch/buyback/BuybackCatalog.java`
- `src/main/java/com/fromscratch/buyback/BuybackDialog.java`
- `src/main/java/com/fromscratch/buyback/BuybackEngine.java`
- `src/main/java/com/fromscratch/buyback/BuybackEntry.java`
- `src/main/java/com/fromscratch/buyback/BuybackPurchase.java`
- `src/main/java/com/fromscratch/buyback/BuybackState.java`
- `src/main/java/com/fromscratch/buyback/BuybackView.java`
- `src/main/java/com/fromscratch/persistence/BuybackCodec.java`
- `src/test/java/com/fromscratch/bank/BuybackPipelineTest.java`
- `src/test/java/com/fromscratch/bank/BuybackUiTest.java`
- `src/test/java/com/fromscratch/buyback/BuybackModelTest.java`
- `src/test/java/com/fromscratch/persistence/BuybackPerformanceTest.java`
- `src/test/java/com/fromscratch/persistence/BuybackPersistenceTest.java`
