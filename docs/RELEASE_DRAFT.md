# From Scratch — Windows beta 0.10.0-beta.1

**LOCAL DRAFT — not published, distributed or Plugin Hub approved.** Upload only after the owner's consolidated acceptance. The actual future release URL is not known yet.

**Your bank is full. Your adventure starts at zero.**

Rediscover the thrill of starting with nothing without abandoning your existing OSRS character. Your real items stay put; From Scratch tracks a separate, signed bank challenge as you rebuild.

## In this beta

- Native bank overlays: gray physical counts, bright configurable virtual counts and shaded legacy items.
- Interactive Virtual Bank with native tabs, ordinary banking and a single Full Bank setting.
- Exact signed quantities and buyback status following normal Examine.
- Categorized fixed-price legacy buybacks, one-time Rune/Divine Pouch and open/closed Looting Bag families, with verified old-contents offsets.
- Finalized GE purchases/sales, item history, Peak Progress Value and host Time Played.
- Character-specific local saves, recovery safeguards and explicit Export/Import Progress.

Gameplay is frozen at the accepted M9.2 source baseline; this milestone prepares distribution and documentation.

## Installation

Windows x64; install the official **Temurin 11 JDK** first. No IntelliJ, Git or Gradle is needed by testers. Download the approved ZIP/checksum, verify the SHA-256, extract everything, run **Install.cmd**, then open **From Scratch Beta** on your desktop. Sign in using RuneLite's official developer-mode **Jagex Accounts** panel; no credential file/token collection is involved. Enable From Scratch and follow Before Starting. See [installation](INSTALLATION.md) and [beta guide](BETA_TESTING.md).

Proposed assets (already prepared locally, not uploaded):

- `from-scratch-beta-0.10.0-beta.1-windows.zip`
- `from-scratch-beta-0.10.0-beta.1-windows.zip.sha256`

`package.json` records exact files/hashes; `SOURCE.json` records baseline commit and frozen gameplay tree, explicitly disclosing uncommitted release inputs. Do not describe the current candidate as a fully committed release. Regenerate after any approved release changes.

## Important limitations

This is an independent, voluntary, unreviewed challenge. It does not prevent withdrawals or change real ownership. A mixed stack still exposes the full physical amount. Bank-only observation cannot reconstruct unseen activity; host Time Played excludes mobile/other clients. Price/tax values are estimates where labeled. Search/other Bank Tags may return to Full Bank. Unknown storage contents block buyback rather than being assumed empty.

**Progress is local to this computer.** Exports contain private, unencrypted progression. Back up before updates, keep exports private, and avoid concurrent clients on the same run. No cloud/device sync. Uninstalling the beta preserves challenge saves, settings, credentials and backups. Update by reinstalling the newer approved ZIP; uninstall with the installed version's **Uninstall.cmd**.

Packaged help, an isolated logged-out client window, safe install/update/uninstall and the standard build were checked locally. Owner shortcut/authentication/gameplay acceptance and hosted CI diagnosis remain release gates; see [Milestone 10](MILESTONE_10.md). Do not present these as passed until verified.

## Reporting a problem

Use the [repository issue form](https://github.com/PilotsRKwl/fromScratch/issues/new/choose) with beta/RuneLite/Java/Windows versions and concise reproduction steps. Redact character/account names, private chat, paths and progression. Never upload credentials, whole client folders or `.fsp` exports. The short template is in [BETA_TESTING.md](BETA_TESTING.md).

From Scratch by **Pilots r kwl**. [BSD-2-Clause](../LICENSE). Not affiliated with or endorsed by Jagex, RuneLite or Steam. Official Plugin Hub submission belongs to Milestone 11; no approval is claimed.
