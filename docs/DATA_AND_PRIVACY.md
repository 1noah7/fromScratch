# Data and privacy

From Scratch has no backend, account registration, cloud synchronization or analytics endpoint. It does not ask for passwords/tokens or access credential stores. Its production source sends no bank contents, character keys, progression, chat, exports or screenshots to a From Scratch service.

## What is stored

RuneLite's scoped Filepath directory is `.runelite/plugin-data/from_scratch`, beneath the normal user home. Character-specific subdirectories contain the run identity/start, immutable legacy snapshot, signed/current bank quantities, journal/checkpoint history, manual corrections, buyback payments/entitlements/legacy contents offsets, observed GE evidence, financial/peak records and host playtime. Identifiers and quantities are private progression, not anonymous statistics.

Atomic journal/checkpoint/current publication, checksums, revision checks and retained recovery archives protect integrity, not confidentiality. Saves and `.fsp` exports are **not encrypted**. Keep them private and secure your Windows account/backups. Never attach them to a public issue. The plugin does not silently create a blank run when loading fails or merge divergent histories on import.

Export uses the local Filepath chooser, excludes credentials/unrelated settings and includes the selected character's required run/history/checkpoints. Import validates format, character identity, checksums, bounded paths/size, complete evidence and revision; it previews/requires confirmation, retains prior files, then atomically selects the verified restored tree. Maximum export/import limits currently include 256 MB total and 4 MB per component. No transfer occurs without user-selected local files.

## Network and authentication boundaries

The plugin requests RuneLite's **bulk public item-price feed** through the existing ItemClient on a separate worker (30-minute cache with retry). It does not submit the player's holdings/character key in that request. No synchronous HTTP occurs during rendering or Examine. RuneLite itself still performs normal game downloads, game connections, authentication, update/Plugin Hub services and its own networking; those are not From Scratch progression synchronization.

The beta bootstrap invokes unmodified RuneLite in developer mode and disables its optional telemetry through a supported argument. The user initiates official Jagex Account login in RuneLite's panel. Authentication/session handling belongs to RuneLite; the From Scratch installer/launcher never reads tokens, captures process environments, copies credential files, or enables insecure credential writing. No credentials or user profiles are bundled. A successful packaging/help test is not proof of authenticated login.

## Installer scope

Windows program files go in versioned folders under `%LOCALAPPDATA%\FromScratchBeta\versions`; a single owned desktop shortcut is created. Installation writes no RuneLite configuration or plugin-side authentication data. Normal RuneLite home is retained when the user launches, so existing same-character progression remains available. Close other clients before using/restoring a run.

Updates install another verified version and switch the owned shortcut. Uninstallation removes only its unchanged allowlisted program files/own shortcut; modified/unrelated files, challenge saves, backups, other sideloaded plugins, normal RuneLite and Jagex credentials remain. PowerShell's execution-policy argument is process-scoped; no machine policy, registry association or administrator installation is changed. No runtime binary download occurs in installer scripts; install Java yourself from the official vendor.

Hidden developer diagnostics are off by default and use rate-limited local DEBUG phase/availability logs. Ordinary errors may contain local context: redact account identifiers, paths and private progression before sharing a small relevant log excerpt. Do not share full client logs, settings folders, authentication panels or save exports.

Technical source locations and review-sensitive behavior: [reviewer map](PLUGIN_HUB_REVIEW.md).
