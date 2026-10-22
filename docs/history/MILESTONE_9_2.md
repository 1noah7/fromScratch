# Milestone 9.2 — Subdued physical bank quantities

Awaiting manual visual acceptance. No progression, buyback, GE, Bank Tags, configuration, persistence or economic changes.

## Rendering approach

Resolved RuneLite 1.13.3 generates quantity text as part of the item icon; Widget text color does not control it. Its public [Widget quantity-display mode API](https://static.runelite.net/runelite-api/apidocs/net/runelite/api/widgets/Widget.html#setItemQuantityMode(int)) and [ItemQuantityMode.NEVER](https://static.runelite.net/runelite-api/apidocs/net/runelite/api/widgets/ItemQuantityMode.html) allow suppressing that counter without changing the item or quantity.

One existing-plugin BeforeRender subscriber prepares eligible normal-bank widgets before native drawing. Original display modes are retained per current child slot. Only modes that normally show a quantity are suppressed; already-hidden counters from another plugin stay hidden. Replaced/hidden children restore their saved modes. The existing bank after-layer pass draws one physical count at the native top-left position, using RuneLite's small RuneScape font and a black one-pixel shadow.

Final gray: **#A0A0A0**, drawn at full opacity after the existing locked tint. No backing rectangle, masking patch, regenerated item sprite or shared sprite-cache modification. The original native counter is not drawn underneath. Virtual counts retain their existing bottom-right positioning, clipping, formatter and configured color. Physical counts use the complete widget quantity, including legacy copies; native formatting remains exact below 100,000, floored K below 10,000,000 and floored M thereafter. The existing virtual compact threshold and exact Examine formatting are unchanged.

The same path covers Full Bank, Virtual Bank, native tabs and placeholders. No active committed run, a character mismatch, closed bank, reset, disabled plugin or both bank overlays disabled restores native counter modes. Shutdown cleanup is dispatched to the client thread without waiting. The existing subscriber lifecycle is retained; no per-item listeners or bank rebuilds are added.

Per-item counter state is bounded by current child slots. Labels change only when the physical quantity changes; stackability definitions are cached. No render-time disk access, network/price requests, ledger scans, image creation or new font loading. Unexpected preparation errors restore modes, log once and pause this cosmetic feature until plugin re-enable. The feature uses public cosmetic APIs without gameplay actions, packet access, menu interception or internal renderer hooks; Plugin Hub approval remains a separate review.

## Validation and limitations

Incremental `compileJava` and `compileTestJava` passed. **61 focused tests passed**, zero failures/errors: VirtualBankOverlayTest 23, BankItemPainterTest 29, BankOverlayStateTest 8, production-plugin EventBus registration 1. Seven new cases cover gray/virtual colors, native appearance before setup, locked contrast/no extra mask, large physical stacks and differing formatter thresholds, moved/clipped tab layouts, reset/disable/cleanup and duplicate-color avoidance. No clean build or full suite.

The pixel fixtures simulate the native counter pass; they do not run the live game renderer. Exact native font alignment, scaling, placeholders and live suppression/restoration require manual inspection. Dragged items deliberately keep their native counter, avoiding guessed drag coordinates or a missing count when RuneLite omits the drag callback. Existing item dragging, shading and virtual-counter behavior are preserved.

## Manual acceptance

1. With an active run, inspect Full Bank: top-left physical counts gray; bottom-right virtual counts retain the configured color. Check mixed stacks such as physical 110 / virtual 10.
2. Inspect locked items against the unchanged #333333 / 50% tint, placeholders, large stacks, partial rows and scrolling. Check for duplicate glyphs, overlaps, dark masks or a recolored sprite.
3. Switch Virtual Bank/Full Bank and native tabs; change the virtual color. Inspect normal/stretched UI scaling and drag an item; the dragged physical counter should remain native.
4. Disable/re-enable the plugin and both overlay checkboxes, and close/reopen the bank. Native appearance must return while inactive. Test reset/no-run appearance only on disposable progression.
5. Deposit/withdraw normally and check responsiveness and quantity accuracy. No accounting/history changes should result from presentation alone.

Development launch: `.\gradlew.bat run`; follow RuneLite's [Jagex Accounts guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) if needed. Only the player operates RuneScape.

Files: `src/main/java/com/fromscratch/FromScratchPlugin.java`, `src/main/java/com/fromscratch/overlay/VirtualBankOverlay.java`, `BankItemPainter.java`, `BankOverlayRegistration.java`; `src/test/java/com/fromscratch/overlay/VirtualBankOverlayTest.java`; `ARCHITECTURE.md`, `CHANGELOG.md`, this report. Existing uncommitted work remains intact. No commit, push, tag, publication or real progression reset.
