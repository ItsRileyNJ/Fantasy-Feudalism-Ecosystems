# November 8, 2026 — Version 0.1.44 — Changelog

- **Sitting spiders:** Pet spiders and cave spiders now tilt their abdomen slightly farther downward while sitting.

The sitting-only pitch changes from -0.18 to -0.25 radians, roughly four additional degrees. Both species use the same SpiderModel hook. The per-render reset keeps standing, walking and untamed poses unchanged. The connected pivot correction is included in 0.1.45: the front attachment remains fixed while the abdomen angles downward.

Validation: release build and whitespace checks passed. No full behavioral suite was rerun for this small visual adjustment, following the requested testing scope.

Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.44.jar`

SHA-256: `a80901adf9f72661b92c2e8d843d1a2b9c01d0c9aedb3b94ec31843edf5ad9f0`
