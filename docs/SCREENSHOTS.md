# Interface previews and gameplay captures

## Current interface previews

These PNGs render the **actual current application components with sample data**. They are not live gameplay captures or a real player's progression. The RuneLite look and feel, native application fonts, layouts, labels and item sprites are retained; no replacement interface was designed for marketing. README captions disclose the sample data.

| Preview | Component and capture |
| --- | --- |
| [Dashboard](media/dashboard.png) / [compact crop](media/dashboard-overview.png) | `BankPanel`; full panel and a crop ending after its six statistics. |
| [Legacy Buybacks](media/legacy-buybacks.png) / [storage category](media/legacy-buybacks-utility.png) | `BuybackView`; real catalog, prices, statuses, scrolling and category filtering. |
| [GE Purchases](media/ge-purchases.png) / [GE Sales](media/ge-sales.png) | `GeHistoryDialog` and `GeHistoryView`; finalized example trades and disclosed tax estimates. |
| [Bank History](media/bank-history.png) | `BankHistoryDialog`; sample deposits, withdrawals and a legacy offset. |
| [Item History](media/item-history.png) | `ItemHistoryDialog`; sample evidence, distinct from corrections. |
| [Before Starting](media/setup.png) | `BankPanel` setup card. |

Reproduce on Windows using `scripts/Prepare-UiPreviews.ps1` from the repository root. The isolated `src/preview` tool paints production components at 2× resolution. It uses an empty user home under ignored `build/marketing`, reads no saves, and opens only standalone sample Swing dialogs. It never starts RuneScape. Official RuneLite item PNGs are cached under that build directory; see [artwork provenance](ARTWORK.md). Minor font/scaling differences from a live player's settings are expected.

## Owner gameplay capture checklist

No live RuneScape captures are included yet. Native Virtual Bank, bank shading/quantity rendering and gameplay cannot be demonstrated by these standalone Swing panels. Only the owner operates RuneScape. The following genuine captures will complete the showcase without any automated gameplay or reset of real progression.

Save approved, cropped PNGs in `docs/media/` with the proposed names below. Use the same RuneLite size/UI scale, enough resolution for legible labels, and a normal live view. Crop out character/account names, chat, friends/clan lists, authentication panels, private paths and unrelated windows. Do not edit quantities/statuses to simulate gameplay. If a public demo character/disposable run is used, say so in the caption.

| Proposed file | Capture and suggested caption |
| --- | --- |
| `gameplay-dashboard.png` | Main From Scratch sidebar with six statistics and adjacent bank. “Your comeback, one milestone at a time.” |
| `gameplay-bank-quantities.png` | Locked and available items, including a mixed physical/virtual stack. Gray physical count, bright virtual count and readable dark shading. “Your bank. Two realities.” |
| `gameplay-virtual-tabs.png` | Virtual Bank with a selected native tab, title visible, qualifying items only. “Find what your challenge has earned.” |
| `gameplay-buybacks.png` | Categorized Combat / Skilling / Utility catalog with genuine sprites/prices/statuses. “Earn it back, on your terms.” |
| `gameplay-ge-history.png` | A real completed/partial-canceled Purchases or Sales history, executed quantity/GP/tax visible. “Every completed trade, clearly recorded.” |
| `gameplay-examine.png` | Native Examine plus one exact signed From Scratch line. “Know exactly where you stand.” |
| `gameplay-progress.png` | Legible Peak Progress Value, Current Bank Net Worth and Time Played on the existing dashboard. “Watch your fresh start grow.” |

Capture Full Bank and Virtual Bank at the same genuine state if illustrating filtering. Never suggest mixed physical stacks enforce withdrawal limits. Do not perform a valuable buyback, import, reset or large trade just for a picture. Existing history and a safe demonstration are sufficient.

After owner approval, place the strongest genuine bank image under README's “Your Own Fresh Bank” section and replace sample previews only where a live image adds value. Keep captions honest about their source. Do not add broken placeholder links. Original banner/social-preview artwork is documented in [ARTWORK.md](ARTWORK.md).
