"""In-process event bus for loose coupling between modules.

Modules should publish domain events instead of calling each other's internals.
Future extraction to microservices: replace InProcessEventBus with message broker adapter.
"""

from collections import defaultdict
from collections.abc import Callable
from dataclasses import dataclass, field
from datetime import UTC, datetime
from typing import Any
from uuid import uuid4


@dataclass(frozen=True)
class DomainEvent:
    event_id: str = field(default_factory=lambda: str(uuid4()))
    event_type: str = ""
    payload: dict[str, Any] = field(default_factory=dict)
    occurred_at: datetime = field(default_factory=lambda: datetime.now(UTC))
    source_module: str = ""


EventHandler = Callable[[DomainEvent], None]


class InProcessEventBus:
    def __init__(self) -> None:
        self._handlers: dict[str, list[EventHandler]] = defaultdict(list)

    def subscribe(self, event_type: str, handler: EventHandler) -> None:
        self._handlers[event_type].append(handler)

    def publish(self, event: DomainEvent) -> None:
        for handler in self._handlers.get(event.event_type, []):
            handler(event)


event_bus = InProcessEventBus()
