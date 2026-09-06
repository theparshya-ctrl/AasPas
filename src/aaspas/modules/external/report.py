"""Structured audit/report models for external offer collection runs."""

from __future__ import annotations

from datetime import datetime
from enum import StrEnum
from typing import Any

from pydantic import BaseModel, Field


class ExternalImportRowOutcome(StrEnum):
    CREATED = "created"
    UPDATED = "updated"
    UNCHANGED = "unchanged"
    REJECTED = "rejected"
    DUPLICATE_IN_FILE = "duplicate_in_file"
    ERROR = "error"


class ExternalImportRowResult(BaseModel):
    row: int
    external_source_key: str | None = None
    outcome: ExternalImportRowOutcome
    message: str | None = None
    offer_id: str | None = None


class ExternalStaleCandidate(BaseModel):
    external_source_key: str
    offer_id: str
    title: str
    shop_name: str | None = None
    last_collected_at: datetime | None = None
    status: str


class ExternalOfferImportReport(BaseModel):
    run_at: datetime
    source_file: str | None = None
    mode: str = "import"
    candidates: int = 0
    created: int = 0
    updated: int = 0
    unchanged: int = 0
    rejected: int = 0
    duplicate_in_file: int = 0
    errors: int = 0
    stale_candidates: list[ExternalStaleCandidate] = Field(default_factory=list)
    rows: list[ExternalImportRowResult] = Field(default_factory=list)

    def to_summary_dict(self) -> dict[str, Any]:
        return {
            "run_at": self.run_at.isoformat(),
            "source_file": self.source_file,
            "mode": self.mode,
            "candidates": self.candidates,
            "created": self.created,
            "updated": self.updated,
            "unchanged": self.unchanged,
            "rejected": self.rejected,
            "duplicate_in_file": self.duplicate_in_file,
            "errors": self.errors,
            "stale_candidates": len(self.stale_candidates),
        }

    def apply_row(self, row: ExternalImportRowResult) -> None:
        self.rows.append(row)
        if row.outcome == ExternalImportRowOutcome.CREATED:
            self.created += 1
        elif row.outcome == ExternalImportRowOutcome.UPDATED:
            self.updated += 1
        elif row.outcome == ExternalImportRowOutcome.UNCHANGED:
            self.unchanged += 1
        elif row.outcome == ExternalImportRowOutcome.REJECTED:
            self.rejected += 1
        elif row.outcome == ExternalImportRowOutcome.DUPLICATE_IN_FILE:
            self.duplicate_in_file += 1
        elif row.outcome == ExternalImportRowOutcome.ERROR:
            self.errors += 1
