# Artwork provenance

Original artwork generated for From Scratch using the built-in imagegen tool. No copied game sprite, API credential, or production graphics-generation dependency is included.

Final prompt:

> Use case: logo-brand. Asset type: original pixel-art RuneLite plugin navigation icon for From Scratch. Create one compact stack of old fantasy MMORPG coins, purple #B450FF with darker purple outlines and subtle gold #FFD54F highlights. Handcrafted low resolution pixel sprite aesthetic, hard square pixel edges, simple clear silhouette recognizable when reduced to 24x24 pixels. Transparent background, centered stack fills canvas with small transparent padding. No text, letters, numbers, border, watermark, drop shadow, checkerboard, environment, or copied game artwork. Deliver an actual PNG with alpha transparency, ideally 48x48 pixels if supported. This asset will be mechanically resized to Plugin Hub limits and a 24x24 sidebar resource.

The large generated PNG was mechanically fitted with transparent padding using nearest-neighbor resampling. Only small RGBA PNG exports are kept in the repository:

| Path | Dimensions | Encoded bytes | Use |
| --- | --- | ---: | --- |
| `icon.png` | 48x48 | 2,700 | Hub root icon; within 48x72 maximum |
| `src/main/resources/com/fromscratch/icon.png` | 24x24 | 842 | Sidebar; 2,304 decoded RGBA bytes |

Both are genuine PNGs with transparent and visible pixels verified by tests. Small exports avoid loading the large generation output at runtime. The exported Hub icon was visually inspected; recognition/contrast at the user's RuneLite UI scale still needs manual acceptance. Supplied with the project under BSD 2-Clause to the extent applicable.
