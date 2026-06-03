.PHONY: up down clean logs test verify format db help

help:
	@echo "Available targets:"
	@echo "  up      - Build and start all services (docker compose up -d --build)"
	@echo "  down    - Stop services, keep data volumes"
	@echo "  clean   - Stop services and remove data volumes"
	@echo "  logs    - Tail app service logs"
	@echo "  test    - Run unit tests (no Docker required)"
	@echo "  verify  - Run unit + integration tests (requires Docker)"
	@echo "  format  - Apply Spotless formatter to all Java files"
	@echo "  db      - Open psql shell in the running postgres container"

up:
	docker compose up -d --build

down:
	docker compose down

clean:
	docker compose down -v

logs:
	docker compose logs -f app

test:
	./mvnw test

verify:
	./mvnw verify

format:
	./mvnw spotless:apply

db:
	@source .env 2>/dev/null || true; \
	docker compose exec postgres psql -U "$${POSTGRES_USER}" "$${POSTGRES_DB}"
