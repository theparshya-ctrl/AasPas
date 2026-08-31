import uuid

from sqlalchemy.orm import Session

from aaspas.modules.category.models import Category


class CategoryRepository:
    def __init__(self, db: Session) -> None:
        self.db = db

    def get_by_id(self, category_id: uuid.UUID) -> Category | None:
        return self.db.get(Category, category_id)

    def get_by_slug(self, slug: str) -> Category | None:
        return self.db.query(Category).filter(Category.slug == slug).one_or_none()

    def list_active(self) -> list[Category]:
        return (
            self.db.query(Category)
            .filter(Category.is_active.is_(True))
            .order_by(Category.display_order.asc(), Category.name.asc())
            .all()
        )

    def create(self, category: Category) -> Category:
        self.db.add(category)
        self.db.flush()
        return category
