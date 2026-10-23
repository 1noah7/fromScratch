# Artwork provenance

Original artwork generated for From Scratch using the built-in **imagegen** tool. The M4 UI pass explicitly replaces the earlier M1 coin artwork. The earlier locally bundled icon was the input reference; no OSRS sprite, screenshot, remote asset pack, credential or production generation dependency is included. Runtime dashboard/history icons separately use RuneLite's supported item sprite API.

Final edit prompt (use case: precise-object-edit):

> Use case: precise-object-edit. Asset type: From Scratch RuneLite sidebar navigation icon, final exports 24x24 and 48x48 pixels. Input image: current original purple coin stack, replace its noisy detail with a much simpler crisp recognizable OSRS-style stack of coins recolored purple. Three overlapping short cylindrical stacks of round coins viewed isometrically, clear elliptical top faces, dark purple edge outlines and stepped lavender highlights. Entire stack fits a square with a little transparent padding. Deliberate low-resolution pixel art, limited purple palette, solid clean pixel clusters, no blur or glow. Transparent background. No quantity numbers, letters, labels, watermark, UI, frame or panel. Maintain purple branding. Favor the familiar simple old-school coin-stack silhouette, readable when reduced to 24 pixels.

The generated RGBA artwork was mechanically fitted to its alpha bounds, resized with nearest-neighbor resampling and centered with transparent padding. Only small PNG exports are kept in the repository:

| Path | Dimensions | Encoded bytes | Use |
| --- | --- | ---: | --- |
| `icon.png` | 48x48 | 2,246 | Hub root icon; within 48x72 maximum |
| `src/main/resources/com/fromscratch/icon.png` | 24x24 | 706 | Navigation and missing-sprite fallback; 2,304 decoded RGBA bytes |

Both are genuine PNGs with visible purple pixels and transparent padding, tested through the actual classpath resource. The sidebar export was visually inspected at its output size. Earlier M4 synthetic sidebar previews used this fallback for item icons. Current M10 previews use official item sprites where requested by the component, with the same production fallback behavior. Recognition at the user's display scale still requires manual acceptance.

Supplied with the project under BSD 2-Clause to the extent applicable. There is no external artwork retrieval or image generation in the production plugin.

## Milestone 10 title artwork

`docs/media/from-scratch-hero.svg` and its 1280×640 PNG counterpart are original code-native title/social artwork created with `scripts/Prepare-Marketing.ps1`. Simple geometric purple chest/coins and illustrative contrasting counters communicate the brand; **they are not RuneScape screenshots**. No Steam assets, OSRS sprites, downloaded asset pack or external generation service was used for this banner. The PNG was visually inspected for legibility and framing; final GitHub/mobile rendering remains owner review. Reproduce on Windows with the script (Segoe UI text; SVG has Arial/sans-serif fallbacks). Keep original source artwork and lossless PNG together under the project's BSD-2-Clause terms to the extent applicable.

Real gameplay captures are not available yet. [SCREENSHOTS.md](SCREENSHOTS.md) separates authentic sample-component previews from the remaining privacy-safe owner gameplay captures. No game screenshot is fabricated. Branding does not imply Jagex, RuneLite or Steam affiliation.

## Milestone 10 interface previews

Windows installer branding reuses the original root `icon.png`. `scripts/Prepare-WindowsIcon.ps1` converts it into `beta/windows-native/FromScratch.ico` with PNG-backed 16/32/48/64/256-pixel frames using nearest-neighbor scaling. It is the same artwork, not a new logo. Setup, uninstaller, launcher and shortcuts use this asset; manual Windows scaling acceptance remains required.

The dashboard, setup, catalog and history PNGs in `docs/media` are lossless 2× renders of the unchanged production Swing components, using RuneLite's look and feel/fonts and explicitly synthetic fixture progress. The dashboard overview is a crop of that same panel. They show neither a real account nor a reconstructed game interface. Source: `src/preview/java/com/fromscratch/preview/InterfacePreviews.java`; reproduction: `scripts/Prepare-UiPreviews.ps1`.

Exact item sprites are retrieved from RuneLite's official `https://static.runelite.net/cache/item/icon/<itemId>.png` service into ignored `build/marketing/icons`, then supplied through the existing `BankIcons` provider. These are OSRS game assets owned by Jagex, included only as part of accurate interface illustrations; they are not original From Scratch artwork or relicensed under BSD. RuneLite application fonts/look and feel remain their respective authors' work. No download or rendering helper is added to the production plugin or release JAR.
