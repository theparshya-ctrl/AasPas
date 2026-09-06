#!/usr/bin/env python3
"""Upload Beta APK to Neon Object Storage (public_read bucket) at a stable key."""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "src"))


def main() -> int:
    parser = argparse.ArgumentParser(description="Upload Beta APK to S3-compatible storage")
    parser.add_argument(
        "--apk",
        type=Path,
        default=Path("releases/current/latest.apk"),
        help="Local APK file to upload",
    )
    parser.add_argument(
        "--key",
        default="beta/android/latest.apk",
        help="Object key in the public bucket",
    )
    args = parser.parse_args()

    if not args.apk.is_file():
        print(f"APK not found: {args.apk}", file=sys.stderr)
        return 1

    try:
        import boto3
    except ImportError:
        print("boto3 is required: pip install boto3", file=sys.stderr)
        return 1

    from aaspas.config import get_settings

    settings = get_settings()
    if not all(
        [
            settings.s3_endpoint_url,
            settings.s3_access_key_id,
            settings.s3_secret_access_key,
            settings.s3_bucket_name,
        ]
    ):
        print(
            "S3 credentials are not configured. Set AWS_ENDPOINT_URL_S3, "
            "AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, and AWS_S3_BUCKET.",
            file=sys.stderr,
        )
        return 1

    client = boto3.client(
        "s3",
        region_name=settings.s3_region,
        endpoint_url=settings.s3_endpoint_url,
        aws_access_key_id=settings.s3_access_key_id,
        aws_secret_access_key=settings.s3_secret_access_key,
    )
    body = args.apk.read_bytes()
    safe_key = args.key.replace("\\", "/").lstrip("/")
    client.put_object(
        Bucket=settings.s3_bucket_name,
        Key=safe_key,
        Body=body,
        ContentType="application/vnd.android.package-archive",
    )

    base = settings.media_public_base_url
    if base:
        print(f"Uploaded: {base.rstrip('/')}/{safe_key}")
    else:
        print(f"Uploaded to s3://{settings.s3_bucket_name}/{safe_key}")
    print(f"Bytes: {len(body)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
