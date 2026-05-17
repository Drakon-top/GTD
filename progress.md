# Progress Log — GTD Personal Management App

Этот файл используется агентами для логирования прогресса по задачам из `tasks.json`.

---

## Формат записи

```
### TASK-XXX — [Краткое описание]
- **Дата:** YYYY-MM-DD
- **Статус:** done
- **Что сделано:** ...
- **Коммиты:** ...
- **Заметки:** ...
```

---

## Записи

### TASK-001 — Инициализация Spring Boot проекта
- **Дата:** 2026-05-17
- **Статус:** done
- **Что сделано:**
  - Создан Spring Boot 3.5.0 проект (Java 21) через Spring Initializr
  - Подключены зависимости: Spring Web, Spring Data JPA, Spring Security, Flyway, PostgreSQL driver, SpringDoc OpenAPI, Spring AMQP, Lombok, Bean Validation, H2 (test)
  - Настроен `application.yml` с профилями dev и prod
  - Dev-профиль: PostgreSQL, debug logging, Flyway baseline-on-migrate
  - Prod-профиль: увеличенный connection pool, отключённый Swagger UI, INFO logging
  - Тестовый профиль: H2 in-memory, Flyway отключён, RabbitMQ auto-config исключён
  - Создан `.gitignore` (корень проекта + backend)
  - Создан `.dev-rules.md` — свод правил разработки и PR checklist
- **Коммиты:** initial commit (pending)
- **Заметки:**
  - Spring Boot 3.4.x недоступен на start.spring.io (min 3.5.0)
  - SpringDoc 2.8.6 добавлен вручную (не генерируется через Initializr)
  - Следующий шаг: TASK-002 (Docker Compose) — настроить PostgreSQL и RabbitMQ для локальной разработки

### TASK-002 — Docker Compose для локальной разработки
- **Дата:** 2026-05-17
- **Статус:** done
- **Что сделано:**
  - Создан `docker-compose.yml` с 3 сервисами: app (Spring Boot), postgres (PostgreSQL 16), rabbitmq (RabbitMQ 3.13 + Management UI)
  - PostgreSQL 16-alpine на порту 5432 с named volume `postgres_data`
  - RabbitMQ 3.13-management-alpine на портах 5672 (AMQP) и 15672 (Management UI) с named volume `rabbitmq_data`
  - Healthchecks для postgres (`pg_isready`) и rabbitmq (`rabbitmq-diagnostics ping`)
  - Сервис app зависит от postgres и rabbitmq через `condition: service_healthy`
  - Создан multi-stage `backend/Dockerfile` (build на eclipse-temurin:21-jdk-alpine, run на 21-jre-alpine)
  - Создан `backend/.dockerignore` для оптимизации Docker-контекста
  - Env-переменные в docker-compose передают DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD и RABBITMQ_* — совпадают с application-dev.yml
  - Добавлен `-XX:+EnableDynamicAgentLoading` в maven-surefire-plugin для Java 21 совместимости с Mockito
  - Проект собирается (`./mvnw clean package`) и тесты проходят
- **Коммиты:** feat: add Docker Compose for local development
- **Заметки:**
  - application-dev.yml уже использовал env-переменные с дефолтами (localhost:5432), поэтому конфиг не потребовал изменений
  - Для запуска всей инфраструктуры: `docker-compose up -d`
  - Для локальной разработки без Docker: запустить PostgreSQL и RabbitMQ отдельно, использовать дефолтные env-значения
  - Следующий шаг: TASK-003 (Flyway миграции + таблица users) или TASK-008 (таблица contexts) — оба разблокированы
