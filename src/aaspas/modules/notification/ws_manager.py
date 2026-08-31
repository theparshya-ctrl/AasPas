"""In-process WebSocket connection manager for real-time notification delivery."""

from __future__ import annotations

import asyncio
import json
import logging
import uuid
from typing import Any

from starlette.websockets import WebSocket

logger = logging.getLogger(__name__)


class NotificationWsManager:
    def __init__(self) -> None:
        self._connections: dict[uuid.UUID, set[WebSocket]] = {}
        self._lock = asyncio.Lock()
        self._loop: asyncio.AbstractEventLoop | None = None

    def set_loop(self, loop: asyncio.AbstractEventLoop) -> None:
        self._loop = loop

    async def connect(self, user_id: uuid.UUID, websocket: WebSocket) -> None:
        await websocket.accept()
        async with self._lock:
            self._connections.setdefault(user_id, set()).add(websocket)

    async def disconnect(self, user_id: uuid.UUID, websocket: WebSocket) -> None:
        async with self._lock:
            sockets = self._connections.get(user_id)
            if sockets is None:
                return
            sockets.discard(websocket)
            if not sockets:
                self._connections.pop(user_id, None)

    async def push_to_user(self, user_id: uuid.UUID, payload: dict[str, Any]) -> None:
        message = json.dumps(payload, default=str)
        async with self._lock:
            sockets = list(self._connections.get(user_id, set()))
        dead: list[WebSocket] = []
        for websocket in sockets:
            try:
                await websocket.send_text(message)
            except Exception:
                dead.append(websocket)
        for websocket in dead:
            await self.disconnect(user_id, websocket)

    def schedule_push(self, user_id: uuid.UUID, payload: dict[str, Any]) -> None:
        loop = self._loop
        if loop is None or not loop.is_running():
            return
        asyncio.run_coroutine_threadsafe(self.push_to_user(user_id, payload), loop)


notification_ws_manager = NotificationWsManager()
