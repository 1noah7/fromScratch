# Friend-and-family beta testing

This is an unpublished, unreviewed candidate until the owner approves distribution. Install only the approved owner-provided **FromScratch-Beta-Setup.exe** from the [canonical repository](https://github.com/PilotsRKwl/fromScratch). [Installation and login](INSTALLATION.md) is the authoritative guide for setup, login, updates and uninstall. The private runtime is included; no developer tools or Java configuration are needed to play.

## Your first session

1. Launch From Scratch Beta and log in through RuneLite's Jagex Accounts panel. Enable the plugin; open the purple coin sidebar.
2. Before starting, bank inventory and equipment yourself. The complete physical bank must remain visible even with Virtual Bank checked in Settings. Read/confirm the attestation and Start confirmation.
3. After the initial save, Virtual Bank activates if its saved setting is ON. Switch native tabs and All. Only positive virtual item types should appear. Disable the single Settings checkbox to see Full Bank.
4. Bank some newly acquired items. Confirm gray physical quantities and bright virtual quantities, locked shading, correct signed counts and responsive normal banking. Mixed physical stacks still permit normal Withdraw-All: respect the virtual count yourself.
5. Examine items normally. Expect the game's description/core price messages, then one From Scratch line with exact signed quantities. No extra menu option.

## A few useful trips

- GE: pending/active partial offers do not appear in history. Complete a buy and sale: one executed row each. Cancel an unfilled offer: no row. Cancel a partial: one partial row for only executed units. Collect/bank normally; no duplicate GP/items/tax or peak increase from merely viewing history.
- Buybacks: inspect catalog categories/search and a confirmation; Cancel must change nothing. With sufficient earned virtual GP, purchase one qualifying legacy item. Charge/entitlement should occur once. Do not spend extra just for testing on a valuable run.
- Pouch/bag: buybacks unlock the equipment, not old contents. Review verified negative offsets, purchase once, then empty/bank the old contents to neutralize them. Never guess unreadable contents. Opening/closing or Rune Pouch upgrades do not require another family payment.
- Close/reopen the bank, switch tabs/views, disable/re-enable the plugin and restart RuneLite. Counts, entitlements, offsets, history, settings, peak and Time Played should persist without freezes.
- Export Progress through the header's Progress files menu. Keep it private. Test Import preview/cancel freely; actually replacing/resetting a run should use disposable progression. Imports replace, not merge, same-character histories.
- Update by running the newer approved setup EXE. Confirm the shortcut and saved challenge still work. Use Windows Settings to uninstall a disposable beta installation: progression/other plugins must remain.

## Reporting a problem

Use [Issues](https://github.com/PilotsRKwl/fromScratch/issues) with the beta version, RuneLite version, Windows version, UI scale, steps, expected/actual behavior and whether restart helps. A cropped screenshot or short redacted error excerpt is useful. Do not attach passwords, tokens, authentication panels, whole logs/config folders or `.fsp` saves.

For a freeze, record the last action and time. An advanced owner can capture `jcmd <RuneLite PID> Thread.print` locally using the Java 11 JDK; it shows stack traces, not an automatic diagnosis. Redact paths/account context and share only relevant threads. Do not keep force-reproducing a freeze against real progression.

Copyable report:

```text
Beta / RuneLite / Windows / UI scale:
What I did:
What I expected:
What happened:
Does restart help? Does Full Bank help?
Relevant redacted screenshot/error excerpt:
```

Mobile/other-PC changes update at the next complete host bank observation. The plugin cannot reconstruct all unseen trades or mobile playtime. Normal bank withdrawals are not proof of consumption; signed deficits are valid. [Full boundaries](FEATURES.md#boundaries).
