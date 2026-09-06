"""Tests for CURSOR-063 external offer collection workflow."""

from __future__ import annotations

from datetime import UTC, datetime, timedelta

import pytest

from aaspas.common.audit import AuditLog
from aaspas.common.exceptions import ValidationAppError
from aaspas.common.source_type import SourceType
from aaspas.config import get_settings
from aaspas.modules.external.import_service import ExternalOfferImportService
from aaspas.modules.external.report import ExternalImportRowOutcome
from aaspas.modules.external.schemas import ExternalOfferImportRecord
from aaspas.modules.external.validation import ExternalValidationOutcome, classify_import_record
from aaspas.modules.notification.models import Notification
from tests.pilot_fixtures import FIXED_NOW, pilot_data
from tests.test_external_offers import import_external, make_external_record


class TestExternalCollectionWorkflow:
    def test_repeated_import_is_idempotent_unchanged(self, db_session, pilot_data):
        record = ExternalOfferImportRecord.model_validate(make_external_record())
        service = ExternalOfferImportService(db_session)
        first = service.import_many([record], now=FIXED_NOW)
        second = service.import_many([record], now=FIXED_NOW)
        assert first.created == 1
        assert second.unchanged == 1
        assert second.updated == 0

    def test_refresh_reports_stale_candidates_without_deactivating(
        self, db_session, pilot_data
    ):
        kept = import_external(
            db_session,
            pilot_data,
            external_source_key="ext-refresh-kept-001",
        )
        missing = import_external(
            db_session,
            pilot_data,
            external_source_key="ext-refresh-missing-001",
            title="Missing From Refresh File",
        )
        incoming = ExternalOfferImportRecord.model_validate(
            make_external_record(
                external_source_key="ext-refresh-kept-001",
                title=kept.title,
            )
        )
        report = ExternalOfferImportService(db_session).import_many(
            [incoming],
            now=FIXED_NOW,
            mode="refresh",
            include_stale_report=True,
        )
        stale_keys = {item.external_source_key for item in report.stale_candidates}
        assert "ext-refresh-missing-001" in stale_keys
        assert "ext-refresh-kept-001" not in stale_keys
        refreshed_missing = db_session.get(type(missing), missing.id)
        assert refreshed_missing is not None
        assert refreshed_missing.status != "expired"

    def test_duplicate_in_file_counted_separately(self, db_session, pilot_data):
        record = ExternalOfferImportRecord.model_validate(make_external_record())
        report = ExternalOfferImportService(db_session).import_many(
            [record, record],
            now=FIXED_NOW,
        )
        assert report.duplicate_in_file == 1
        assert report.created == 1
        assert any(
            row.outcome == ExternalImportRowOutcome.DUPLICATE_IN_FILE for row in report.rows
        )

    def test_updated_source_information(self, db_session, pilot_data):
        import_external(
            db_session,
            pilot_data,
            external_source_key="ext-refresh-update-001",
            source_name="Old Source",
            source_url="https://example.com/old",
        )
        updated = ExternalOfferImportRecord.model_validate(
            make_external_record(
                external_source_key="ext-refresh-update-001",
                source_name="New Source",
                source_url="https://example.com/new",
            )
        )
        report = ExternalOfferImportService(db_session).import_many([updated], now=FIXED_NOW)
        assert report.updated == 1
        from aaspas.modules.offer.models import Offer

        offer = (
            db_session.query(Offer)
            .filter(Offer.external_source_key == "ext-refresh-update-001")
            .one()
        )
        assert offer.source_name == "New Source"
        assert offer.source_url == "https://example.com/new"

    def test_import_run_writes_audit_log(self, db_session, pilot_data):
        record = ExternalOfferImportRecord.model_validate(
            make_external_record(external_source_key="ext-audit-run-001")
        )
        before = db_session.query(AuditLog).count()
        ExternalOfferImportService(db_session).import_many(
            [record],
            now=FIXED_NOW,
            source_file="collected.json",
        )
        after = db_session.query(AuditLog).count()
        assert after == before + 1
        entry = (
            db_session.query(AuditLog)
            .filter(AuditLog.resource_type == "external_offer_import")
            .order_by(AuditLog.created_at.desc())
            .first()
        )
        assert entry is not None
        assert entry.module == "external"
        assert entry.metadata_json["created"] == 1

    def test_rejected_missing_source_name(self, external_categories):
        payload = make_external_record(external_source_key="ext-no-source-name-001")
        payload["source_name"] = "   "
        record = ExternalOfferImportRecord.model_validate(payload)
        outcome, _ = classify_import_record(record, active_categories=external_categories)
        assert outcome == ExternalValidationOutcome.REJECTED

    def test_rejected_invalid_source_url_scheme(self, external_categories):
        record = ExternalOfferImportRecord.model_validate(
            make_external_record(
                external_source_key="ext-no-source-url-001",
                source_url="ftp://invalid.example/offers",
            )
        )
        outcome, _ = classify_import_record(record, active_categories=external_categories)
        assert outcome == ExternalValidationOutcome.REJECTED

    def test_rejected_vague_offer(self, db_session, pilot_data, external_categories):
        record = ExternalOfferImportRecord.model_validate(
            make_external_record(
                external_source_key="ext-vague-workflow-001",
                title="Great deals available",
            )
        )
        report = ExternalOfferImportService(db_session).import_many([record], now=FIXED_NOW)
        assert report.rejected == 1

    def test_null_ends_at_ongoing_offer(self, db_session, pilot_data):
        offer = import_external(
            db_session,
            pilot_data,
            external_source_key="ext-null-ends-workflow-001",
            ends_at=None,
            starts_at=None,
        )
        assert offer.ends_at is None
        assert offer.source_type == SourceType.EXTERNAL.value
        assert offer.is_verified is False

    def test_no_notifications_on_batch_import(self, db_session, pilot_data):
        before = db_session.query(Notification).count()
        record = ExternalOfferImportRecord.model_validate(
            make_external_record(external_source_key="ext-batch-notify-001")
        )
        ExternalOfferImportService(db_session).import_many([record], now=FIXED_NOW)
        after = db_session.query(Notification).count()
        assert before == after

    def test_dev_guard_blocks_development(self, monkeypatch):
        monkeypatch.setenv("DATABASE_URL", "postgresql://user:pass@neon.example/db")
        monkeypatch.setenv("APP_ENV", "development")
        get_settings.cache_clear()
        with pytest.raises(ValidationAppError, match="development"):
            ExternalOfferImportService.assert_beta_import_target()
        get_settings.cache_clear()

    def test_explicit_expired_rejected(self, db_session, pilot_data):
        record = ExternalOfferImportRecord.model_validate(
            make_external_record(
                external_source_key="ext-expired-workflow-001",
                ends_at=(FIXED_NOW - timedelta(days=1)).isoformat(),
            )
        )
        report = ExternalOfferImportService(db_session).import_many([record], now=FIXED_NOW)
        assert report.rejected == 1

    def test_deactivate_external_offer_without_ends_at(self, db_session, pilot_data):
        from aaspas.modules.offer.models import Offer
        from aaspas.modules.offer.status import OfferStatus

        offer = import_external(
            db_session,
            pilot_data,
            external_source_key="ext-deactivate-workflow-001",
            ends_at=None,
        )
        assert offer.ends_at is None
        assert offer.status == OfferStatus.ACTIVE.value

        service = ExternalOfferImportService(db_session)
        deactivated, outcome = service.deactivate_external_offer(
            "ext-deactivate-workflow-001",
            reason="Public source vouchers sold out",
            now=FIXED_NOW,
            source_verification_url="https://example.com/store",
        )
        assert outcome == "deactivated"
        assert deactivated.status == OfferStatus.EXPIRED.value
        assert deactivated.ends_at is None
        assert deactivated.is_verified is False
        assert deactivated.source_type == "EXTERNAL"

        again, second_outcome = service.deactivate_external_offer(
            "ext-deactivate-workflow-001",
            reason="Repeat call",
            now=FIXED_NOW,
        )
        assert second_outcome == "unchanged"
        assert again.status == OfferStatus.EXPIRED.value

    def test_deactivate_rejects_non_external(self, db_session, pilot_data):
        from aaspas.common.exceptions import ValidationAppError
        from aaspas.modules.offer.models import Offer

        offer = pilot_data["offers"][0]
        offer.external_source_key = "ext-not-external-001"
        db_session.commit()

        service = ExternalOfferImportService(db_session)
        with pytest.raises(ValidationAppError, match="EXTERNAL"):
            service.deactivate_external_offer(
                "ext-not-external-001",
                reason="Should fail",
                now=FIXED_NOW,
            )


@pytest.fixture
def external_categories(db_session):
    from aaspas.modules.category.models import Category
    import uuid

    names = ["Clothing / Fashion", "Restaurant"]
    for index, name in enumerate(names, start=1):
        slug = name.lower().replace(" / ", "-").replace(" ", "-")
        if db_session.query(Category).filter(Category.slug == slug).one_or_none() is None:
            db_session.add(
                Category(
                    id=uuid.uuid4(),
                    name=name,
                    slug=slug,
                    is_active=True,
                    display_order=index,
                )
            )
    db_session.commit()
    return {name for name in names}
