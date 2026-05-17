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

### TASK-003 — Настройка Flyway миграций и создание таблицы User
- **Дата:** 2026-05-17
- **Статус:** done
- **Что сделано:**
  - Создана директория `backend/src/main/resources/db/migration/`
  - Создана миграция `V1__create_users_table.sql` (PostgreSQL): таблица `users` с полями id (UUID PK, gen_random_uuid()), email (UNIQUE, NOT NULL), password_hash (NOT NULL), created_at (TIMESTAMPTZ), updated_at (TIMESTAMPTZ)
  - Индекс `idx_users_email` для быстрого поиска по email
  - Создана JPA-сущность `User` в пакете `com.gtd.backend.auth.model` с Lombok (@Builder, @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor)
  - @PrePersist / @PreUpdate автоматически устанавливают created_at и updated_at
  - Создан `UserRepository` (JpaRepository) в `com.gtd.backend.auth.repository` с методами findByEmail() и existsByEmail()
  - Написаны 7 тестов для UserRepository (save, findByEmail, existsByEmail, duplicate email rejection, timestamps)
  - Убрана устаревшая настройка `database-platform: org.hibernate.dialect.H2Dialect` из тестового application.yml
- **Коммиты:** feat: add Flyway V1 migration for users table and User JPA entity
- **Заметки:**
  - Тестовый профиль использует H2 с create-drop и Flyway disabled — PostgreSQL-специфичный SQL миграции не мешает тестам
  - Production/dev: Flyway применяет миграцию, Hibernate validate проверяет соответствие entity и таблицы
  - Mockito/ByteBuddy agent attachment может не работать в sandboxed-среде (нужен `-XX:+EnableDynamicAgentLoading`, уже настроен в surefire)
  - Разблокированы: TASK-004 (User entity + регистрация с BCrypt), TASK-008 (таблица contexts)
  - Следующий приоритет: TASK-004 (security, critical) — регистрация с BCrypt, или TASK-008 (functional, critical) — миграция contexts

### TASK-004 — Модель User (JPA Entity) + регистрация с BCrypt хэшированием
- **Дата:** 2026-05-17
- **Статус:** done
- **Что сделано:**
  - Создан `RegisterRequest` DTO с Jakarta Validation (@Email, @NotBlank, @Size(min=8))
  - Создан `RegisterResponse` DTO (id, email, createdAt)
  - Создан `ErrorResponse` DTO (status, error, message, details, timestamp)
  - Создан `AuthService` с методом register(): нормализация email (trim + lowercase), проверка дубликата, BCrypt хэширование, сохранение в БД
  - Создан `AuthController` с эндпоинтом `POST /api/v1/auth/register` (возвращает 201 Created)
  - Создан `EmailAlreadyExistsException` → 409 Conflict
  - Создан `GlobalExceptionHandler` (@RestControllerAdvice): обработка EmailAlreadyExists (409) и MethodArgumentNotValid (400 с деталями по полям)
  - Создан `SecurityConfig`: BCryptPasswordEncoder bean, SecurityFilterChain (auth/** permitAll, остальное authenticated, CSRF off, stateless session)
  - Написаны unit-тесты AuthService (4 теста: регистрация, хэширование, нормализация email, дубликат)
  - Написаны controller-тесты AuthController (7 тестов: 201, 409, 400 invalid email/blank email/short password/blank password/empty body)
  - Написаны интеграционные тесты (5 тестов: полный цикл регистрации с проверкой BCrypt в БД, дубликат email, нормализация регистра, невалидный email, короткий пароль)
  - Добавлен mockito-extensions/org.mockito.plugins.MockMaker для совместимости с sandbox-средой
- **Коммиты:** feat: add user registration endpoint with BCrypt password hashing
- **Заметки:**
  - Spring Boot 3.5.0 перенёс @MockBean в `org.springframework.test.context.bean.override.mockito.MockitoBean`
  - ByteBuddy agent attachment не работает в sandbox (нужны all permissions для тестов или использовать subclass mock maker)
  - SecurityConfig пока простой — auth/** открыт, остальное закрыто. JWT-фильтр будет добавлен в TASK-005
  - Разблокирован: TASK-005 (JWT авторизация: login, access/refresh tokens)
  - Следующий приоритет: TASK-005 (security, critical) — JWT auth, или TASK-008 (functional, critical) — таблица contexts
