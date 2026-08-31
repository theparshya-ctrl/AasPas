FROM python:3.12-slim

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    libpq-dev gcc \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY pyproject.toml .
COPY alembic.ini .
COPY alembic/ alembic/
COPY src/ src/
COPY deploy/beta/ deploy/beta/
COPY scripts/seed_beta_admin.py scripts/seed_beta_admin.py

ENV PYTHONPATH=/app/src
ENV HOST=0.0.0.0
ENV PORT=8000

EXPOSE 8000

CMD ["uvicorn", "aaspas.main:app", "--host", "0.0.0.0", "--port", "8000"]
