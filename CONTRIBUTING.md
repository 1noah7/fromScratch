# Contributing

Read [AGENTS.md](AGENTS.md), [ARCHITECTURE.md](ARCHITECTURE.md), and [ACCOUNTING.md](ACCOUNTING.md). Public maintainer identity: **Pilots r kwl**. Work one authorized milestone at a time; label future features as planned.

Use Java 11 and the existing Gradle Wrapper: `.\gradlew.bat clean build` or `bash ./gradlew clean build`. Do not add dependencies already supplied by RuneLite, change config keys without migration, commit artifacts, or include private account/progression data. Keep changes focused and avoid unrelated formatting.

Separate API observations, pure accounting, Filepath storage, and Swing rendering. Use gameval constants, checked long arithmetic, injected Gson/OkHttp where needed, DEBUG diagnostics, and immutable snapshots. Never block the client thread/lifecycle methods. Explain accounting assumptions, API limitations, and uncertain evidence in comments.

Add relevant tests and user acceptance instructions for behavior changes. Test code must not log in, click, type, or automate RuneScape. Only the user can confirm behavior in-game. Reject reflection, native access, external processes in plugin code, packets, server-directed menu actions, and gameplay automation during review.

Report bugs with versions, reproduction steps, and redacted evidence through the issue template. PR descriptions explain concrete behavior, accounting/compliance decisions, test results, and pending manual acceptance. Preserve license notices; contributions use BSD 2-Clause.

Do not commit, push, publish, or submit without explicit approval. An eventual Plugin Hub PR must follow AGENTS.md attribution requirements and reference an approved release commit after all gates pass.
