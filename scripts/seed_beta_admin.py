"""Create exactly one Beta platform admin (staging only).

Admin self-registration is blocked by AuthService.register(); this script is the
supported way to bootstrap the first Beta admin after migrations.

Usage (on Beta host or with Beta DATABASE_URL):
    set BETA_ADMIN_EMAIL=you@example.com
    set BETA_ADMIN_PASSWORD=your-strong-password
    PYTHONPATH=src python scripts/seed_beta_admin.py

Credentials are read from environment variables only — never commit passwords.
"""

from __future__ import annotations

import os
import sys

from aaspas.common.security.auth import hash_password
from aaspas.config import get_settings
from aaspas.database import SessionLocal
from aaspas.modules.auth.models import User


def main() -> int:
    settings = get_settings()
    if settings.app_env not in {"staging", "testing"}:
        print(
            f"Refusing to seed admin: APP_ENV={settings.app_env!r} "
            "(expected staging or testing).",
            file=sys.stderr,
        )
        return 1

    email = os.environ.get("BETA_ADMIN_EMAIL", "").strip().lower()
    password = os.environ.get("BETA_ADMIN_PASSWORD", "")
    if not email or not password:
        print(
            "Set BETA_ADMIN_EMAIL and BETA_ADMIN_PASSWORD in the environment.",
            file=sys.stderr,
        )
        return 1
    if len(password) < 12:
        print("BETA_ADMIN_PASSWORD must be at least 12 characters.", file=sys.stderr)
        return 1

    db = SessionLocal()
    try:
        existing = db.query(User).filter(User.email == email).one_or_none()
        if existing is not None:
            if existing.role not in {"admin", "super_admin"}:
                print(f"User {email} exists but is not an admin (role={existing.role}).", file=sys.stderr)
                return 1
            print(f"Beta admin already exists: {email}")
            return 0

        other_admins = (
            db.query(User)
            .filter(User.role.in_(["admin", "super_admin"]))
            .count()
        )
        if other_admins > 0:
            print("A Beta admin already exists. Refusing to create a second admin.", file=sys.stderr)
            return 1

        user = User(
            email=email,
            password_hash=hash_password(password),
            full_name=os.environ.get("BETA_ADMIN_FULL_NAME", "Beta Platform Admin"),
            role="admin",
            is_active=True,
        )
        db.add(user)
        db.commit()
        print(f"Beta admin created: {email}")
        return 0
    finally:
        db.close()


if __name__ == "__main__":
    raise SystemExit(main())
