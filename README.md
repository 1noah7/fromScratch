# From Scratch

<img src="icon.png" width="48" height="48" alt="Purple coin stack" />

By **Pilots r kwl**. Java 11 RuneLite plugin, BSD 2-Clause license.

Start fresh on your existing Old School RuneScape account. Build a separate virtual bank, earn items and wealth again, unlock legacy equipment, and track your progression.

**Status: Milestone 1 accepted; Milestone 2 bank-detection correction awaiting manual retest.** The first M2 manual test exposed an event-dependent bank-readiness bug; its correction preserves existing saves and snapshot rules. You can initialize a run from the normal bank, inventory and equipment, preserve that legacy baseline per character, and reset with an archive. Virtual ownership and wealth remain zero: acquisition tracking, overlays, buybacks and valuation are not implemented yet. This plugin has not been submitted to or approved by the Plugin Hub.

## Development installation

Install a Java 11 JDK and open this repository as a Gradle project. Use the included Gradle Wrapper.

```powershell
.\gradlew.bat clean build
.\gradlew.bat run
```

On Linux/macOS, use `bash ./gradlew clean build` and `bash ./gradlew run`. In IntelliJ, choose a Java 11 Gradle JVM and run the Gradle `run` task or `FromScratchPluginTest.main`.

Follow RuneLite's [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) instructions to log in to the development client. Keep login credentials and launcher tokens out of this repository. Enable **From Scratch** and click its purple coin button. Before setup, one short instruction explains the capture scope; **Detailed setup tooltip** controls extra hover detail. Instructions disappear during setup/after activation, while capture status and milestone limitations remain visible. The existing configuration key is preserved.

There is no public Plugin Hub installation yet. Screenshots will be added after user-run acceptance; agents have not captured in-game screenshots.

## Initial setup

1. Log in and wait for this character's run to load. Before login, Begin is disabled; an unknown identity or storage error blocks setup.
2. Click **Begin From Scratch**, then manually open your normal bank, or begin with it already open. Enter any Bank PIN manually. Capture retries automatically on game ticks while the interface settles or data is unavailable; it does not require a new quantity-change event.
3. Wait for **Bank: Ready**, **Inventory: Ready** and **Equipment: Ready**, then review summaries: item types, occupied slots, total units and legacy coins. Empty inventory and equipment are supported; carrying/equipping an item is unnecessary. If a personal location is Waiting, close the bank, manually open its normal tab for about two seconds, then reopen within 60 seconds without moving items. A null personal container is accepted as empty only after independently verifying every rendered slot on two different game ticks. Bank-side inventory can also provide this proof. Missing/incomplete views remain unknown. Tabs/search do not limit the complete bank container; placeholders/fillers are excluded and variants retain exact IDs.
4. Click **Confirm Snapshot** and accept the confirmation dialog. Keep the bank open and contents unchanged during review. If anything changes, review and confirm the updated summary.
5. Wait for **Active**. The run start time and saved baseline summaries appear. All virtual quantities, spendable GP, net worth, gained value and unlocked counts start at zero. Manually clear legacy inventory/equipment you do not intend to use.

The original baseline stays fixed when physical items change. M2 does not account for any later activity. Cancel Setup, logout or disable before confirmation discards only the unsaved preview.

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

- M2 preserves initialization only. Virtual balances stay zero; acquisitions, transfers, overlays, GE, buybacks and valuation are not tracked yet.
- APIs cannot prove provenance or distinguish identical physical copies. Ambiguous observations require reconciliation.
- A trustworthy bank observation requires opening the bank. A missing personal container remains unknown unless its complete, settled interface independently verifies emptiness. Unsupported or incomplete widget layouts keep setup blocked.
- POH/death storage, seed vault, looting bag, minigame points, Miscellania, and other special storage are excluded from initialization. Death fees need a manual fallback.
- Notes/doses/charges remain distinct raw IDs in the baseline; canonical ownership mapping is deferred. Mobile reconciliation is not implemented.
- Save recovery is conservative and manual; there is no restore/import/export UI yet. No automatic promotion of a checkpoint occurs when current data is missing or corrupt.
- RuneLite profile-type namespaces separate standard and temporary modes. Storage is permanently local-only on one designated host computer. Cloud storage, cloud credentials, synchronization and a third-party backend are excluded from the project. Gameplay elsewhere must be reconciled on return to the host; reconciliation remains planned for M4.
- M2 in-game acceptance and hosted CI remain pending. A local build does not establish either. Play-time tracking is deferred.

## Data, backup, and reset

Progression lives in RuneLite's plugin directory: `.runelite/plugin-data/from_scratch/characters/<opaque-character-key>/`. It is never written into this checkout. Character keys derive from the live account hash plus RuneLite world profile type after verification against the selected RS profile; display names and configuration-profile names are not save keys.

`current.json` is the authoritative checksummed versioned checkpoint. Each successful activation/reset also keeps an immutable `checkpoint-<revision>.json`. Confirmed reset first creates and verifies `archive-<run-id>-<unique-id>.json`, then writes a Not Started record. It does not automatically create a new run. Click **Begin From Scratch** and take a new snapshot to start again. Canceling the reset dialog does nothing to saved data or real items.

For backup, close RuneLite and copy the entire `from_scratch` plugin data directory to a private location outside Git. It is never synchronized to a server. Moving the designated host is an explicit manual backup/restore operation, not merging independent runs from several computers. To recover, first back up the entire directory and close all clients. Preserve the damaged current file separately; select a known-good checkpoint/archive for the same character and copy it to `current.json`, then relaunch and use **Retry Load**. Choose deliberately: an archive restores an old active run, while a reset checkpoint keeps it Not Started. Do not delete recovery evidence or edit checksums/identity fields. Future/incompatible versions require compatible software, not a blank replacement. Ask for help if the correct checkpoint is uncertain; do not share private save contents publicly.

Writes run in a background worker and force a recovery checkpoint before atomic current-file replacement. Filesystems without atomic replacement use a recoverable copy fallback. Wait for Active or reset confirmation before closing/disabling. A cancelled/interrupted write may be durable but unacknowledged; reload determines its result. A missing/corrupt current file with recovery evidence blocks new initialization. A transaction journal and restore/import/export UI remain future work. See [ARCHITECTURE.md](ARCHITECTURE.md).

## Troubleshooting and bug reports

- Missing sidebar: launch this checkout's development client, enable From Scratch, toggle once, and inspect relevant log errors.
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
