# Batbox source model

`Batbox Model.json` is the user-supplied Blockbench Java model, preserved byte-for-byte. SHA-256: `a8fdf00d731d06c7b27ea0199e860a54d67e49e5b53f01272d7cc7cd78ff4769`.

The export has seventeen elements and no texture definitions; every face references `#missing`. The runtime model preserves all element bounds, rotations, and UV coordinates, replaces unresolved material references with custom spruce-colored wood/interior textures and the vanilla cauldron-side texture for the four front nails, normalizes the parent resource ID, and omits Blockbench editor-only colors. No glass or additional cuboids are added.
