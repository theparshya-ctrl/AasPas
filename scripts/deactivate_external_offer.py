"""Deactivate a stale EXTERNAL offer on Beta Neon (status=expired, ends_at unchanged).

Usage:
    PYTHONPATH=src python scripts/deactivate_external_offer.py <external_source_key> \\
        --reason "Magicpin vouchers sold out on public store page" \\
        --source-url "https://magicpin.in/..."

Requires .env.beta pointing at Beta PostgreSQL. Does not modify DEV.
"""

from __future__ import annotations

import argparse
import os
import sys
from datetime import UTC, datetime
from pathlib import Path

from aaspas.config import get_settings
from aaspas.database import SessionLocal
from aaspas.modules.external.import_service import ExternalOfferImportService
from aaspas.modules.offer.models import Offer


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
    parser = argparse.ArgumentParser(description="Deactivate an EXTERNAL offer on Beta PostgreSQL")
    parser.add_argument("external_source_key", help="Stable external_source_key of the offer")
    parser.add_argument("--reason", required=True, help="Human-readable reason for deactivation")
    parser.add_argument(
        "--source-url",
        help="Public source URL checked before deactivation (audit metadata only)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Validate target and offer existence without writing",
    )
    args = parser.parse_args()

    env_file = _resolve_env_file()
    if env_file is not None:
        _load_env_file(env_file)
        print(f"Loaded environment from {env_file}")

    get_settings.cache_clear()
    try:
        ExternalOfferImportService.assert_beta_import_target()
    except Exception as exc:
        print(f"Deactivation refused: {exc}", file=sys.stderr)
        return 1

    db = SessionLocal()
    try:
        service = ExternalOfferImportService(db)
        if args.dry_run:
            offer = (
                db.query(Offer)
                .filter(Offer.external_source_key == args.external_source_key)
                .one_or_none()
            )
            if offer is None:
                print(f"Dry run failed: offer not found for key {args.external_source_key!r}")
                return 1
            print(
                f"Dry run OK: would deactivate offer {offer.id} "
                f"(status={offer.status}, ends_at={offer.ends_at})"
            )
            return 0

        offer, outcome = service.deactivate_external_offer(
            args.external_source_key,
            reason=args.reason,
            now=datetime.now(UTC),
            source_verification_url=args.source_url,
        )
        print(
            f"Deactivation {outcome}: offer_id={offer.id} "
            f"status={offer.status} ends_at={offer.ends_at} "
            f"external_source_key={offer.external_source_key}"
        )
        return 0
    finally:
        db.close()


if __name__ == "__main__":
    raise SystemExit(main())
