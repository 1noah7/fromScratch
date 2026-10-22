FROM SCRATCH - WINDOWS BETA CANDIDATE (UNPUBLISHED / NOT PLUGIN HUB APPROVED)

Prerequisite: Windows x64 and the official Temurin 11 JDK.
https://adoptium.net/temurin/releases/?version=11
No IntelliJ, Git or Gradle is required for playing.

1. Obtain the ZIP and matching SHA-256 from the owner's approved release.
2. Extract ALL files to a normal folder; do not run inside the ZIP.
3. Run Install.cmd. No administrator rights or credentials are requested.
4. Double-click the From Scratch Beta desktop shortcut.
5. In RuneLite, open Jagex Accounts, choose Add Jagex Account, and complete
   the official login window yourself. Choose your character, then Play Now.
6. Enable From Scratch in the plugin list and use its purple coin sidebar.

Official authentication instructions:
https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts
Do not send anyone passwords, tokens, credential files or exported saves.
Do not enable insecure-write-credentials. This installer never uses it.

The beta launches unmodified official RuneLite libraries in developer mode.
It is not a normal Plugin Hub installation. Internet is required for the
official game/client downloads, authentication and cached market prices.
RuneLite's normal user directory is retained; From Scratch progression stays
local and character-specific. Close other clients before playing/restoring.
Export Progress before updating. Install a newer extracted package the same
way; it updates the shortcut and keeps older program versions.

To uninstall: close the beta, open its installed version folder beneath
%LOCALAPPDATA%\FromScratchBeta\versions, and run Uninstall.cmd. It removes
only that version's unchanged program files and its own shortcut. It never
deletes challenge progression, backups, RuneLite settings or credentials.

Known limitations: signed BANK-only progress, voluntary challenge rules,
mixed physical stacks, host-only playtime, estimated GE prices/tax, no cloud
sync. A client launch or help check is not authenticated gameplay acceptance.

Documentation / issues: https://github.com/PilotsRKwl/fromScratch
Published release status: https://github.com/PilotsRKwl/fromScratch/releases
This candidate has not been published or distributed.
