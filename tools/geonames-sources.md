# GeoNames city-data input

`cities.v1.json` is generated from GeoNames `cities15000.zip`,
`alternateNamesV2.zip`, and `admin1CodesASCII.txt`. They are CC BY 4.0 data exports from
[GeoNames](https://www.geonames.org/). The committed source date and SHA-256
values are recorded in this document when the asset is refreshed.

The generator accepts local, pinned inputs and verifies their SHA-256 hashes
before emitting a sorted, compact JSON asset. This keeps app builds offline and
makes an asset update reviewable and reproducible.

## Current asset

The initial asset was generated from GeoNames snapshots downloaded on
`2026-08-31`:

| Input | SHA-256 |
| --- | --- |
| `cities15000.zip` | `e2f2e93c59bfb2820d2821909f706452eeef9cb7aef36c9407c4fcbe3cb0056a` |
| `alternateNamesV2.zip` | `d64c39d6b4d0d1014e5f2b3fdd54f42d7bb063b526ac2ffa89d5cb5337411212` |
| `admin1CodesASCII.txt` | `590651498043f674accda2b7f46d21286cda0e290b02f8561c5005eee9a5448c` |

Run `tools/import_geonames.py --help` for the reproducible command.
