# Artwork provenance

Original artwork generated for From Scratch using the built-in **imagegen** tool. The M4 UI pass explicitly replaces the earlier M1 coin artwork. The earlier locally bundled icon was the input reference; no OSRS sprite, screenshot, remote asset pack, credential or production generation dependency is included. Runtime dashboard/history icons separately use RuneLite's supported item sprite API.

Final edit prompt (use case: precise-object-edit):

> Use case: precise-object-edit. Asset type: From Scratch RuneLite sidebar navigation icon, final exports 24x24 and 48x48 pixels. Input image: current original purple coin stack, replace its noisy detail with a much simpler crisp recognizable OSRS-style stack of coins recolored purple. Three overlapping short cylindrical stacks of round coins viewed isometrically, clear elliptical top faces, dark purple edge outlines and stepped lavender highlights. Entire stack fits a square with a little transparent padding. Deliberate low-resolution pixel art, limited purple palette, solid clean pixel clusters, no blur or glow. Transparent background. No quantity numbers, letters, labels, watermark, UI, frame or panel. Maintain purple branding. Favor the familiar simple old-school coin-stack silhouette, readable when reduced to 24 pixels.

The generated RGBA artwork was mechanically fitted to its alpha bounds, resized with nearest-neighbor resampling and centered with transparent padding. Only small PNG exports are kept in the repository:

| Path | Dimensions | Encoded bytes | Use |
| --- | --- | ---: | --- |
| `icon.png` | 48x48 | 2,246 | Hub root icon; within 48x72 maximum |
| `src/main/resources/com/fromscratch/icon.png` | 24x24 | 706 | Navigation and missing-sprite fallback; 2,304 decoded RGBA bytes |

Both are genuine PNGs with visible purple pixels and transparent padding, tested through the actual classpath resource. The sidebar export was visually inspected at its output size. The 225-pixel synthetic sidebar previews use this fallback for every item icon; a live client supplies the distinct genuine OSRS sprites instead. Recognition at the user's display scale still requires manual acceptance.

Supplied with the project under BSD 2-Clause to the extent applicable. There is no external artwork retrieval or image generation in the production plugin.
