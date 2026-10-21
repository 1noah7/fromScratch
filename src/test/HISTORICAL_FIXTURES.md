# Historical compatibility fixtures

M4 bank-only tracking supersedes multi-container acquisition/correlation and Update Progress.
The old `controller/SetupController`, personal/container collectors, `ui/FromScratchPanel`,
review editor and tracking engine/session now live **only in the test source set**.
They are not registered, injected or packaged in the production plugin JAR.

The original 417 tests remain executable to preserve accepted M1–M3 behavior and
frozen pre-migration semantics. Historical M4 tests describe the retired implementation,
not current acceptance requirements. `LegacyOverlayFixture` adapts their saved bank
allocations/deficits to the current bank-only projection; geometry/Java2D tests still
exercise the production renderer.

Current-contract evidence comes from `bank/BankOnlyPipelineTest`, `BankContractTest`,
`BankPanelTest`, and `persistence/BankPersistenceTest`. No synthetic fixture may operate
the game, read real progression or create a production grant-items control.

Lombok's existing compile-only annotation processor also processes these historical
test fixtures. No new production dependency was introduced.
