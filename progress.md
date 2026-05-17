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
