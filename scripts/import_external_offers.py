"""Import external unverified offers into Beta Neon PostgreSQL only.

Usage (Beta host or local with .env.beta pointing at Neon):
    PYTHONPATH=src python scripts/import_external_offers.py data/external_offers/template.json

Refresh workflow (import + stale-candidate report, no auto-deletion):
    PYTHONPATH=src python scripts/import_external_offers.py path\\to\\collected.json --refresh --report reports/external_refresh.json

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


def _configure_console_encoding() -> None:
    """Best-effort UTF-8 console output on Windows (avoids cp1252 failures on ₹ etc.)."""
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if callable(reconfigure):
            try:
                reconfigure(encoding="utf-8", errors="replace")
            except Exception:
                pass


def _console_safe(text: str, *, encoding: str | None = None) -> str:
    target = encoding or getattr(sys.stdout, "encoding", None) or "utf-8"
    return text.encode(target, errors="replace").decode(target)


def _print_report_summary(report) -> None:
    summary = report.to_summary_dict()
    print(
        "Run complete: "
        f"mode={summary['mode']} "
        f"candidates={summary['candidates']} "
        f"created={summary['created']} "
        f"updated={summary['updated']} "
        f"unchanged={summary['unchanged']} "
        f"rejected={summary['rejected']} "
        f"duplicate_in_file={summary['duplicate_in_file']} "
        f"errors={summary['errors']} "
        f"stale_candidates={summary['stale_candidates']}"
    )
    rejected_rows = [
        row for row in report.rows if row.message and row.outcome.value in {"rejected", "error"}
    ]
    if rejected_rows:
        print("Rejected/errors:", file=sys.stderr)
        for row in rejected_rows:
            key = row.external_source_key or "?"
            print(f"  - row {row.row} [{key}]: {row.message}", file=sys.stderr)


def main() -> int:
    parser = argparse.ArgumentParser(description="Import external offers into Beta PostgreSQL")
    parser.add_argument("json_file", type=Path, help="Path to external offers JSON file")
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Validate file and target database without writing",
    )
    parser.add_argument(
        "--refresh",
        action="store_true",
        help="Refresh mode: import valid records and report stale active external offers missing from file",
    )
    parser.add_argument(
        "--report",
        type=Path,
        help="Write machine-readable JSON audit report to this path",
    )
    parser.add_argument(
        "--allow-needs-review",
        action="store_true",
        help="Import records flagged NEEDS_REVIEW (e.g. area-only address)",
    )
    args = parser.parse_args()

    _configure_console_encoding()

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
        if args.refresh:
            print("Refresh mode would also report stale active external offers after import.")
        return 0

    db = SessionLocal()
    try:
        service = ExternalOfferImportService(db)
        report = service.import_many(
            import_file.offers,
            now=datetime.now(UTC),
            allow_needs_review=args.allow_needs_review,
            source_file=str(args.json_file),
            mode="refresh" if args.refresh else "import",
            include_stale_report=args.refresh,
        )
        _print_report_summary(report)

        if args.report is not None:
            args.report.parent.mkdir(parents=True, exist_ok=True)
            args.report.write_text(
                json.dumps(report.model_dump(mode="json"), indent=2),
                encoding="utf-8",
            )
            print(f"Report written to {args.report}")

        if report.stale_candidates:
            print("Stale candidates (missing from this collection file; not auto-deactivated):")
            for candidate in report.stale_candidates:
                title = _console_safe(candidate.title)
                print(
                    f"  - {candidate.external_source_key}: {title} "
                    f"(status={candidate.status})"
                )

        if report.errors and report.created == 0 and report.updated == 0 and report.unchanged == 0:
            return 1
        return 0
    finally:
        db.close()


if __name__ == "__main__":
    raise SystemExit(main())
