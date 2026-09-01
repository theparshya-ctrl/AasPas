"""Import external unverified offers into Beta Neon PostgreSQL only.

Usage (Beta host or local with .env.beta pointing at Neon):
    PYTHONPATH=src python scripts/import_external_offers.py data/external_offers/template.json

The JSON file must follow data/external_offers/template.json structure.
Real public offers are collected separately; do not commit offer payloads.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from datetime import UTC, datetime
from pathlib import Path

from aaspas.config import get_settings
from aaspas.database import SessionLocal
from aaspas.modules.external.import_service import ExternalOfferImportService
from aaspas.modules.external.schemas import ExternalOfferImportFile


def _load_env_file(path: Path) -> None:
    if not path.is_file():
        return
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        os.environ.setdefault(key, value)


def _resolve_env_file() -> Path | None:
    explicit = os.environ.get("AASPAS_ENV_FILE", "").strip()
    if explicit:
        return Path(explicit)
    root = Path(__file__).resolve().parents[1]
    beta_env = root / ".env.beta"
    return beta_env if beta_env.is_file() else None


def main() -> int:
    parser = argparse.ArgumentParser(description="Import external offers into Beta PostgreSQL")
    parser.add_argument("json_file", type=Path, help="Path to external offers JSON file")
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Validate file and target database without writing",
    )
    args = parser.parse_args()

    env_file = _resolve_env_file()
    if env_file is not None:
        _load_env_file(env_file)
        print(f"Loaded environment from {env_file}")

    get_settings.cache_clear()
    settings = get_settings()

    try:
        ExternalOfferImportService.assert_beta_import_target()
    except Exception as exc:
        print(f"Import refused: {exc}", file=sys.stderr)
        return 1

    if not args.json_file.is_file():
        print(f"File not found: {args.json_file}", file=sys.stderr)
        return 1

    payload = json.loads(args.json_file.read_text(encoding="utf-8"))
    import_file = ExternalOfferImportFile.model_validate(payload)
    if not import_file.offers:
        print("No offers found in import file (offers array is empty).")
        return 0

    if args.dry_run:
        print(
            f"Dry run OK: {len(import_file.offers)} record(s) validated for "
            f"APP_ENV={settings.app_env!r} target."
        )
        return 0

    db = SessionLocal()
    try:
        service = ExternalOfferImportService(db)
        result = service.import_many(import_file.offers, now=datetime.now(UTC))
        print(
            "Import complete: "
            f"created={result['created']} updated={result['updated']} skipped={result['skipped']}"
        )
        errors = result.get("errors") or []
        if errors:
            print("Errors:", file=sys.stderr)
            for message in errors:
                print(f"  - {message}", file=sys.stderr)
            return 1 if result["created"] == 0 and result["updated"] == 0 else 0
        return 0
    finally:
        db.close()


if __name__ == "__main__":
    raise SystemExit(main())
