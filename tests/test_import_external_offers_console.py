"""Tests for import_external_offers.py console helpers."""

from __future__ import annotations

import sys


def test_console_safe_replaces_unencodable_characters():
    from scripts.import_external_offers import _console_safe

    rendered = _console_safe(
        "Current bill vouchers; e.g. \u20b9500 voucher for \u20b9425",
        encoding="cp1252",
    )
    rendered.encode("cp1252")


def test_configure_console_encoding_is_idempotent():
    from scripts.import_external_offers import _configure_console_encoding

    _configure_console_encoding()
    _configure_console_encoding()
