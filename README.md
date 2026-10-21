# From Scratch

<img src="icon.png" width="48" height="48" alt="Purple coin stack" />

By **Pilots r kwl**. Java 11 RuneLite plugin, BSD 2-Clause license.

Start fresh on your existing Old School RuneScape account. Build a separate virtual bank, earn items and wealth again, unlock legacy equipment, and track your progression.

**Status: Milestone 1 foundation, awaiting manual RuneLite acceptance.** This version supplies a sidebar preview, plugin lifecycle, metadata, and a validated run-state header. Dashboard amounts are placeholders. Starting a challenge, item tracking, saves, overlays, buybacks, and valuation are planned features. This plugin has not been submitted to or approved by the Plugin Hub.

## Development installation

Install a Java 11 JDK and open this repository as a Gradle project. Use the included Gradle Wrapper.

```powershell
.\gradlew.bat clean build
.\gradlew.bat run
```

On Linux/macOS, use `bash ./gradlew clean build` and `bash ./gradlew run`. In IntelliJ, choose a Java 11 Gradle JVM and run the Gradle `run` task or `FromScratchPluginTest.main`.

Follow RuneLite's [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) instructions to log in to the development client. Keep login credentials and launcher tokens out of this repository. Enable **From Scratch** in the plugin list, then click the purple coin sidebar button. **Show foundation help** controls the milestone explanation; the placeholder warning remains visible.

There is no public Plugin Hub installation yet. Screenshots will be added after user-run acceptance; agents have not captured in-game screenshots.

## Challenge rules (planned)

The player keeps levels, quests, diaries, and other character progression. An initial snapshot of the normal bank, inventory, and equipment marks every existing quantity as legacy. All virtual quantities and spendable GP begin at zero. Log in, open the bank, review the snapshot, and confirm activation. Manually clear legacy inventory/equipment you do not intend to use.

Legitimate new acquisitions, including gifts and trades, belong to the challenge. Identical legacy and earned copies coexist through quantity accounting. One earned copy does not unlock every physical copy. Transfers create no earnings. This is an honor-system challenge, with visual warnings subject to RuneLite/Jagex rules.

One run may be active per character. Logout, disabling the plugin, or mobile play does not end it. Ending/resetting will require confirmation and a backup. Offline reconciliation will use observable net changes and request corrections when uncertain. No historical activity will be invented.

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

- Foundation values are static; there is no active challenge or character save yet.
- APIs cannot prove provenance or distinguish identical physical copies. Ambiguous observations require reconciliation.
- A trustworthy bank observation requires opening the bank. A missing container is unknown, not empty.
- POH/death storage, seed vault, looting bag, minigame points, Miscellania, and other special storage are excluded from initialization. Death fees need a manual fallback.
- Mobile play exposes net changes, not full history. GE coverage, variants/charges, and pricing need validation in their milestones.
- In-game acceptance and hosted CI remain pending. A local build does not establish either.

## Data, backup, and reset

M1 creates no progression files and offers no reset/import/export. RuneLite manages presentation config. Keep private run data out of Git.

Starting in M2, the plan is per-character RuneLite `Filepath` storage, versioned checkpoints, a recoverable journal, backups before destructive reset, and explicit confirmation. Recovery/import/export must be tested before being advertised as available. See [ARCHITECTURE.md](ARCHITECTURE.md).

## Troubleshooting and bug reports

- Missing sidebar: launch this checkout's development client, enable From Scratch, toggle once, and inspect relevant log errors.
- Build failure: confirm Java 11, refresh Gradle, and check RuneLite repository/Maven Central access. Report the resolved RuneLite version; the template uses `latest.release`.
- Cache lock error: use a writable `GRADLE_USER_HOME`; do not change system home variables.
- Zero dashboard: expected foundation preview.

Use the bug template with reproduction steps, OS/JDK/RuneLite versions, and a redacted relevant log excerpt. Remove character identities, credentials, local paths, and private saves. Never attach whole configuration folders.

## Documentation

- [Architecture and milestone plan](ARCHITECTURE.md)
- [Accounting policy and invariants](ACCOUNTING.md)
- [Test plan](TEST_PLAN.md)
- [Compatibility evidence](docs/COMPATIBILITY.md)
- [Publication checklist](docs/PUBLICATION.md)
- [Artwork provenance](docs/ARTWORK.md)
- [Contributing](CONTRIBUTING.md), [changelog](CHANGELOG.md), [license](LICENSE)

Created from the [official RuneLite template](https://github.com/runelite/example-plugin). Publication follows the [Plugin Hub](https://github.com/runelite/plugin-hub) and [review process](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review); acceptance is not guaranteed.
