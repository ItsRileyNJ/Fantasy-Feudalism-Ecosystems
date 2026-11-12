# November 11, 2026 — Version 0.1.46 — Changelog

- **Enclosure bats:** Removed the side-eye highlights while preserving their front-face details.

The enclosure uses a dedicated native BatRenderer with a corrected texture. Exactly two opaque side-head pixels differ from vanilla: (1,10) and (6,10). Front eyes, nose, ears, fur, wings, feet and transparency remain unchanged. The renderer is private to enclosure displays; ordinary world bats retain their normal renderer and texture.

Validation: release build passed. A real client connected to the dedicated server rendered one, two and three captured bats, retaining their fixed animation pose, roof contact and staggered positions. Native foot measurements stayed within 0.00001 blocks of the roof. Resource reload rebuilt the enclosure renderer successfully, with no rendering errors. This cosmetic change did not rerun the full gameplay suite. Evidence: [render checks](bats-0.1.46/render-results.json), [close view](bats-0.1.46/close.png).

Pixel comparison confirmed only the two intended pixels changed, and the release JAR contains that exact texture without test classes. Whitespace checks passed.

Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.46.jar`

SHA-256: `423b6b451fac623567623208dbb88cbcd7b714ef35a4e821108eb89d7f2f9036`
