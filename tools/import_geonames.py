#!/usr/bin/env python3
"""Build the deterministic offline city asset from pinned GeoNames exports.

Usage:
  tools/import_geonames.py --cities /path/cities15000.zip --alternates /path/alternateNamesV2.zip \
    --admin1 /path/admin1CodesASCII.txt --cities-sha256 SHA256 --alternates-sha256 SHA256 --admin1-sha256 SHA256

The caller supplies exact checksums, so an accidental or changed download cannot
silently become the committed asset. The generated JSON has sorted keys and rows.
"""

import argparse
import csv
import hashlib
import json
import sys
import unicodedata
import zipfile
from collections import defaultdict
from pathlib import Path


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def source_member(archive: zipfile.ZipFile, suffix: str) -> str:
    return next(member for member in archive.namelist() if member.endswith(suffix))


def is_arabic(value: str) -> bool:
    return any("ARABIC" in unicodedata.name(character, "") for character in value)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cities", type=Path, required=True)
    parser.add_argument("--alternates", type=Path, required=True)
    parser.add_argument("--admin1", type=Path, required=True)
    parser.add_argument("--cities-sha256", required=True)
    parser.add_argument("--alternates-sha256", required=True)
    parser.add_argument("--admin1-sha256", required=True)
    parser.add_argument("--source-date", required=True, help="Pinned GeoNames export date (YYYY-MM-DD)")
    parser.add_argument("--output", type=Path, default=Path("app/src/main/assets/cities.v1.json"))
    arguments = parser.parse_args()

    for path, expected in (
        (arguments.cities, arguments.cities_sha256),
        (arguments.alternates, arguments.alternates_sha256),
        (arguments.admin1, arguments.admin1_sha256),
    ):
        actual = sha256(path)
        if actual != expected.lower():
            raise SystemExit(f"SHA-256 mismatch for {path}: expected {expected}, received {actual}")

    administration_names: dict[str, str] = {}
    with arguments.admin1.open(encoding="utf-8") as source:
        for row in csv.reader(source, delimiter="\t"):
            if len(row) >= 2:
                administration_names[row[0]] = row[1]

    arabic_names: dict[str, str] = {}
    with zipfile.ZipFile(arguments.alternates) as archive:
        with archive.open(source_member(archive, "alternateNamesV2.txt"), "r") as binary:
            for row in csv.reader((line.decode("utf-8") for line in binary), delimiter="\t"):
                # alternateNameId, geonameid, isolanguage, alternate name, preferred, short, colloquial, historic, from, to
                if len(row) >= 4 and row[2] == "ar" and is_arabic(row[3]) and row[1] not in arabic_names:
                    arabic_names[row[1]] = row[3]

    records = []
    with zipfile.ZipFile(arguments.cities) as archive:
        with archive.open(source_member(archive, "cities15000.txt"), "r") as binary:
            for row in csv.reader((line.decode("utf-8") for line in binary), delimiter="\t"):
                # GeoNames dump documentation: id, name, ascii, alternates, lat, lon, class, code,
                # country, cc2, admin1, admin2, admin3, admin4, population, elevation, dem, timezone, modified.
                if len(row) < 19 or not row[17]:
                    continue
                records.append({
                    "id": row[0], "name": row[1], "asciiName": row[2],
                    **({"arabicName": arabic_names[row[0]]} if row[0] in arabic_names else {}),
                    "countryCode": row[8],
                    **({"administrationName": administration_names[f"{row[8]}.{row[10]}"]} if f"{row[8]}.{row[10]}" in administration_names else {}),
                    "latitude": float(row[4]), "longitude": float(row[5]),
                    "population": int(row[14] or 0), "zoneId": row[17],
                })

    records.sort(key=lambda record: (record["name"].casefold(), record["countryCode"], record["id"]))
    payload = {"version": 1, "sourceDate": arguments.source_date, "cities": records}
    arguments.output.parent.mkdir(parents=True, exist_ok=True)
    arguments.output.write_text(json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"Wrote {len(records)} cities to {arguments.output}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
