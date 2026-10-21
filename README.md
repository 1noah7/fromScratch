# From Scratch

<img src="icon.png" width="48" height="48" alt="Purple coin stack" />

By **Pilots r kwl**. Java 11 RuneLite plugin, BSD 2-Clause license.

Start fresh on your existing Old School RuneScape account. Build a separate virtual bank, earn items and wealth again, unlock legacy equipment, and track your progression.

**Status: Milestones 1 and 2 accepted; Milestone 3 functionally accepted, revised lock appearance awaiting visual acceptance.** Active runs show purple virtual bank quantities and fade zero-owned bank items. Existing M2 saves migrate with a backup while preserving the run and legacy baseline. Acquisition/transfer accounting, mobile reconciliation, buybacks and valuation are not implemented, so live virtual balances remain zero. This plugin has not been submitted to or approved by the Plugin Hub.

## Development installation

Install a Java 11 JDK and open this repository as a Gradle project. Use the included Gradle Wrapper.

```powershell
.\gradlew.bat clean build
.\gradlew.bat run
```

On Linux/macOS, use `bash ./gradlew clean build` and `bash ./gradlew run`. In IntelliJ, choose a Java 11 Gradle JVM and run the Gradle `run` task or `FromScratchPluginTest.main`.

Follow RuneLite's [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) instructions to log in to the development client. Keep login credentials and launcher tokens out of this repository. Enable **From Scratch** and click its purple coin button. Setup instructions/preview summaries appear during initialization; the active dashboard hides original account quantities and wealth. **Detailed setup tooltip** retains its existing configuration key.

There is no public Plugin Hub installation yet. Screenshots will be added after user-run acceptance; agents have not captured in-game screenshots.

## Initial setup

1. Log in and wait for this character's run to load. Before login, Begin is disabled; an unknown identity or storage error blocks setup.
2. Click **Begin From Scratch**, then manually open your normal bank, or begin with it already open. Enter any Bank PIN manually. Capture retries automatically on game ticks while the interface settles or data is unavailable; it does not require a new quantity-change event.
3. Wait for **Bank: Ready**, **Inventory: Ready** and **Equipment: Ready**, then review summaries: item types, occupied slots, total units and legacy coins. Empty inventory and equipment are supported; carrying/equipping an item is unnecessary. If a personal location is Waiting, close the bank, manually open its normal tab for about two seconds, then reopen within 60 seconds without moving items. A null personal container is accepted as empty only after independently verifying every rendered slot on two different game ticks. Bank-side inventory can also provide this proof. Missing/incomplete views remain unknown. Tabs/search do not limit the complete bank container; placeholders/fillers are excluded and variants retain exact IDs.
4. Click **Confirm Snapshot** and accept the confirmation dialog. Keep the bank open and contents unchanged during review. If anything changes, review and confirm the updated summary.
5. Wait for **Active**. The run start time and progression dashboard appear. The original detailed baseline remains saved privately; it is hidden from the active sidebar. All virtual quantities, spendable GP, net worth, gained value and unlocked counts start at zero. Manually clear legacy inventory/equipment you do not intend to use.

The original baseline stays fixed when physical items change. M3 does not account for later activity. Cancel Setup, logout or disable before confirmation discards only the unsaved preview.

## Virtual bank display

While the normal bank is open in an Active run, each visible item shows its virtual bank-available quantity in purple at bottom-right. Physical OSRS quantities and banking actions remain unchanged. Zero-owned items receive a uniform dark neutral-gray tint (#505050, approximately 48% opacity). The darker neutral color is closer to the bank background, reducing the appearance of bright gray rectangles. This is a draw-only fallback, not true sprite grayscale: the background within each item's bounds is also tinted. Ordinary quantity glyphs retain about 52% of their original contrast; purple counters are drawn afterward at full color. Partially visible top and bottom rows receive identical treatment, clipped to the item content viewport. Counters keep their normal coordinates and may be clipped rather than moved. Placeholders show zero; empty slots and bank fillers have no counter. Shared storage, inventory, equipment and specialized storage have no overlay.

The overlay reads actual normal-bank item widgets because RuneLite 1.13.3's callback item list omits items whose top-left corner is outside the clip. Dragged items use RuneLite's supplied drag bounds. If a clipped-origin drag has no supplied bounds, that floating item's effect is temporarily omitted until release, avoiding an incorrectly positioned mask. No mouse offsets are guessed or game input changed.

RuneLite's From Scratch settings provide **Virtual bank quantities**, **Dim locked bank items** (both enabled by default), and **Virtual quantity color** (default #B450FF). Changes apply immediately. The features can be toggled independently. Closing the bank, logging out or disabling the plugin removes rendering.

M3 has no earnings engine or grant-items control: all production quantities remain zero. Future accounting will update the separate total/location ownership model. Positive rendering is covered by synthetic tests; live positive-balance acceptance belongs to a later milestone. See [M3 implementation and acceptance](docs/MILESTONE_3.md).

## Challenge rules

The player keeps levels, quests, diaries, and other character progression. The initial normal-bank, inventory and equipment snapshot marks every captured quantity as legacy, including coins. Virtual quantities and spendable GP begin at zero.

Planned accounting will recognize legitimate acquisitions, including gifts/trades. Identical legacy and earned copies will coexist through quantities; one earned copy will not unlock every copy. Transfers must create no earnings. This is an honor-system challenge, with any future visual warnings subject to RuneLite/Jagex rules.

One run may be active per character. Logout, disabling the plugin, or mobile play does not end it. Reset requires confirmation and an archive before clearing the run. Offline reconciliation is planned for M4; M2 does not reconstruct activity or invent history.

The plugin must never click, withdraw, deposit, equip, buy, trade, type, or otherwise perform player actions. See [ACCOUNTING.md](ACCOUNTING.md).

## Buyback schedule (planned)

Buybacks spend virtual GP and unlock existing locked legacy quantities without real game actions. Eligibility, physical existence, funds, and confirmation must precede an atomic ledger transaction. Naturally earning any untradeable is allowed even if absent from the catalog.

| Item | Virtual GP |
| --- | ---: |
| Fire Cape | 1,000,000 |
| Infernal Cape | 15,000,000 |
| Imbued God Cape | 750,000 each |
| Ava's Assembler | 1,000,000 |
| Dragon Defender | 250,000 |
| Fighter Torso | 2,000,000 |
| Salve Amulet (ei) | 500,000 |
| Ancient Sceptre | 500,000 flat |
| Void melee / ranged / mage helm | 300,000 each |
| Void Knight top / robe | 800,000 each |
| Void Knight gloves | 200,000 |
| Elite top / robe upgrade | Additional 1,000,000 each |
| Full Graceful outfit | 1,000,000 |
| Full Prospector outfit | 1,000,000 |
| Full Angler outfit | 750,000 |
| Full Rogue outfit | 500,000 |
| Full Lumberjack outfit | 500,000 |
| Full Farmer's outfit | 750,000 |

An existing legacy Elite Void top/robe costs 1,800,000 each including its base piece; no physical downgrade. Graceful is an all-or-nothing purchase of six standard pieces, requiring their locked legacy physical quantities. No individual Graceful buybacks; naturally earned pieces remain independent. Other outfit/recolor mappings require an explicit policy before M7. Tradeables, Barrows Gloves, Royal Seed Pod, and uncatalogued items are ineligible. Ancient Sceptre's flat price overrides component pricing.

## Known limitations

- M3 adds bank-only visual ownership display. Live virtual balances stay zero; acquisitions, transfers, GE, buybacks, mobile reconciliation and valuation are not tracked yet.
- APIs cannot prove provenance or distinguish identical physical copies. Ambiguous observations require reconciliation.
- A trustworthy bank observation requires opening the bank. A missing personal container remains unknown unless its complete, settled interface independently verifies emptiness. Unsupported or incomplete widget layouts keep setup blocked.
- POH/death storage, seed vault, looting bag, minigame points, Miscellania, and other special storage are excluded from initialization. Death fees need a manual fallback.
- Notes/doses/charges remain distinct raw IDs in the baseline; canonical ownership mapping is deferred. Mobile reconciliation is not implemented.
- Save recovery is conservative and manual; there is no restore/import/export UI yet. No automatic promotion of a checkpoint occurs when current data is missing or corrupt.
- RuneLite profile-type namespaces separate standard and temporary modes. Storage is permanently local-only on one designated host computer. Cloud storage, cloud credentials, synchronization and a third-party backend are excluded from the project. Gameplay elsewhere must be reconciled on return to the host; reconciliation remains planned for M4.
- M1/M2 and M3 functional behavior passed user acceptance. The revised M3 lock appearance, live performance and hosted CI still need verification. A local build does not establish those. Play-time tracking is deferred.

## Data, backup, and reset

Progression lives in RuneLite's plugin directory: `.runelite/plugin-data/from_scratch/characters/<opaque-character-key>/`. It is never written into this checkout. Character keys derive from the live account hash plus RuneLite world profile type after verification against the selected RS profile; display names and configuration-profile names are not save keys.

`current.json` is the authoritative checksummed versioned checkpoint. M3 uses envelope format 2 with ownership totals/location allocations, retaining header schema 1. Loading a valid M2 format-1 save first forces/verifies an exact-byte `migration-v1-<unique-id>.json` backup, then publishes a new recoverable checkpoint with zero virtual ownership. Run ID, start time, character identity, reset history and legacy baseline are preserved. Only the storage revision changes. Failed validation blocks loading and preserves files; it never substitutes empty ownership for missing/corrupt format-2 data. Older checkpoints/archives are retained. Downgrading requires deliberately restoring a compatible M2 backup with all clients closed; M2 cannot read format 2.

Each successful activation/reset also keeps an immutable `checkpoint-<revision>.json`. Confirmed reset first verifies `archive-<run-id>-<unique-id>.json`, then writes Not Started without starting a replacement. Click **Begin From Scratch** and take a fresh snapshot to start again. Canceling reset leaves saved data and real items untouched.

For backup, close RuneLite and copy the entire `from_scratch` plugin data directory to a private location outside Git. It is never synchronized to a server. Moving the designated host is an explicit manual backup/restore operation, not merging independent runs from several computers. To recover, first back up the entire directory and close all clients. Preserve the damaged current file separately; select a known-good checkpoint/archive for the same character and copy it to `current.json`, then relaunch and use **Retry Load**. Choose deliberately: an archive restores an old active run, while a reset checkpoint keeps it Not Started. Do not delete recovery evidence or edit checksums/identity fields. Future/incompatible versions require compatible software, not a blank replacement. Ask for help if the correct checkpoint is uncertain; do not share private save contents publicly.

Writes run in a background worker and force a recovery checkpoint before atomic current-file replacement. Filesystems without atomic replacement use a recoverable copy fallback. Wait for Active or reset confirmation before closing/disabling. A cancelled/interrupted write may be durable but unacknowledged; reload determines its result. A missing/corrupt current file with recovery evidence blocks new initialization. A transaction journal and restore/import/export UI remain future work. See [ARCHITECTURE.md](ARCHITECTURE.md).

## Troubleshooting and bug reports

- Missing sidebar: launch this checkout's development client, enable From Scratch, toggle once, and inspect relevant log errors.
- No bank overlay: wait for Active, open the normal bank, and check the two bank display settings. PIN screens, shared/special storage, an unknown identity, loading/error states and other characters cannot render. If another plugin draws over the same item corner, temporarily disable that plugin to identify a visual conflict; From Scratch never changes its settings.
- Build failure: confirm Java 11, refresh Gradle, and check RuneLite repository/Maven Central access. Report the resolved RuneLite version; the template uses `latest.release`.
- Cache lock error: use a writable `GRADLE_USER_HOME`; do not change system home variables.
- Setup waits: read the independent Bank/Inventory/Equipment status and Run status instruction. Open the normal bank and enter any PIN manually. For a missing personal observation, close the bank and show the indicated normal inventory/equipment tab for about two seconds, then reopen within 60 seconds without moving items. Empty locations do not require adding an item. If still unavailable, leave setup blocked and collect only `From Scratch capture:` DEBUG lines including `emptyVerification` across these steps (the development run task enables `--debug`). See [diagnostic fields and manual procedure](docs/MILESTONE_2.md#empty-inventoryequipment-correction). No save deletion or reset is needed. A deposit box or cached closed-bank container is insufficient.
- Storage error: keep all files, close other clients, check permissions, then Retry Load. Corrupt/incompatible saves require deliberate recovery or compatible software.
- Zero virtual dashboard: expected for M2; legacy items and GP are excluded from virtual wealth.

Use the bug template with reproduction steps, OS/JDK/RuneLite versions, and a redacted relevant log excerpt. Remove character identities, credentials, local paths, and private saves. Never attach whole configuration folders.

## Documentation

- [Architecture and milestone plan](ARCHITECTURE.md)
- [Accounting policy and invariants](ACCOUNTING.md)
- [Test plan](TEST_PLAN.md)
- [Compatibility evidence](docs/COMPATIBILITY.md)
- [Publication checklist](docs/PUBLICATION.md)
- [Artwork provenance](docs/ARTWORK.md)
- [Milestone 2 implementation and acceptance record](docs/MILESTONE_2.md)
- [Contributing](CONTRIBUTING.md), [changelog](CHANGELOG.md), [license](LICENSE)

Created from the [official RuneLite template](https://github.com/runelite/example-plugin). Publication follows the [Plugin Hub](https://github.com/runelite/plugin-hub) and [review process](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review); acceptance is not guaranteed.
