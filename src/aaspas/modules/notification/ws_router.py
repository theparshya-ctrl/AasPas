"""Authenticated WebSocket endpoint for real-time in-app notifications."""

from __future__ import annotations

import uuid

from fastapi import APIRouter, Query, WebSocket, WebSocketDisconnect
from jose import JWTError
from sqlalchemy.orm import Session

from aaspas.common.security.auth import decode_access_token
from aaspas.common.security.rbac import UserRole
from aaspas.database import SessionLocal
from aaspas.modules.auth.repository import AuthRepository
from aaspas.modules.notification.ws_manager import notification_ws_manager

router = APIRouter(tags=["Notification WebSocket"])


def _authenticate_ws_token(token: str, db: Session) -> uuid.UUID | None:
    try:
        payload = decode_access_token(token)
        user_id = uuid.UUID(payload.sub)
    except (JWTError, ValueError, KeyError):
        return None

    user = AuthRepository(db).get_by_id(user_id)
    if user is None or not user.is_active:
        return None
    if UserRole(user.role) not in {
        UserRole.CUSTOMER,
        UserRole.SHOP_OWNER,
        UserRole.ADMIN,
        UserRole.SUPER_ADMIN,
    }:
        return None
    return user_id


@router.websocket("/notifications")
async def notifications_websocket(
    websocket: WebSocket,
    token: str | None = Query(default=None),
) -> None:
    if not token:
        await websocket.close(code=4401, reason="Missing token")
        return

    db = SessionLocal()
    try:
        user_id = _authenticate_ws_token(token, db)
    finally:
        db.close()

    if user_id is None:
        await websocket.close(code=4401, reason="Unauthorized")
        return

    await notification_ws_manager.connect(user_id, websocket)
    try:
        while True:
            # Keep connection alive; ignore client pings/text.
            await websocket.receive_text()
    except WebSocketDisconnect:
        pass
    finally:
        await notification_ws_manager.disconnect(user_id, websocket)
