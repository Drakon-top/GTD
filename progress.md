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

### TASK-005 — JWT авторизация: login, access token, refresh token
- **Дата:** 2026-05-17
- **Статус:** done
- **Что сделано:**
  - Добавлены зависимости JJWT 0.12.6 (jjwt-api, jjwt-impl, jjwt-jackson) в pom.xml
  - Создан `JwtProperties` (@ConfigurationProperties) для конфига JWT (secret, access/refresh expiration)
  - JWT-конфиг добавлен в application.yml (15 мин access, 30 дней refresh) и test application.yml
  - Создан `JwtService` — генерация access/refresh токенов (HMAC-SHA), валидация, парсинг claims (userId, type)
  - Создана Flyway-миграция `V2__create_refresh_tokens_table.sql` (id UUID PK, user_id FK, token_hash UNIQUE, expires_at, revoked, created_at)
  - Создана JPA-сущность `RefreshToken` с маппингом и `RefreshTokenRepository` (findByTokenHashAndRevokedFalse, revokeAllByUserId)
  - Refresh-токен хранится в БД как SHA-256 хэш — оригинал токена отправляется в HttpOnly cookie
  - Создан `LoginRequest` DTO (email + password с валидацией)
  - Создан `AuthResponse` DTO (accessToken + tokenType)
  - Расширен `AuthService`: login() — аутентификация + генерация пары токенов, refresh() — ротация refresh-токена (old revoked, new issued), logout() — revoke refresh-токена
  - Создан `JwtAuthenticationFilter` (OncePerRequestFilter) — извлекает Bearer token из Authorization header, валидирует, устанавливает Authentication в SecurityContext
  - Обновлён `SecurityConfig` — JWT-фильтр добавлен перед UsernamePasswordAuthenticationFilter, custom AuthenticationEntryPoint возвращает 401
  - Созданы `InvalidCredentialsException` и `InvalidRefreshTokenException` с обработкой в GlobalExceptionHandler (401 Unauthorized)
  - POST /api/v1/auth/login — access token в теле, refresh token в HttpOnly secure cookie
  - POST /api/v1/auth/refresh — ротация обоих токенов, refresh из cookie
  - POST /api/v1/auth/logout — revoke refresh-токена, очистка cookie
  - Обновлены AuthControllerTest (14 тестов), AuthServiceTest (12 тестов), JwtServiceTest (6 тестов)
  - Создан JwtAuthIntegrationTest (8 тестов: login, wrong password, protected endpoint, refresh, refresh revoked, logout, etc.)
  - Обновлён RegistrationIntegrationTest — cleanup refresh_tokens перед users для FK constraint
  - Все 54 теста проходят, проект собирается: `./mvnw clean package`
- **Коммиты:** feat: add JWT authentication with login, refresh, and logout endpoints
- **Заметки:**
  - Refresh token rotation: при каждом refresh старый токен revoke, выдаётся новый — защита от replay attacks
  - Токены хранятся как SHA-256 хэш в БД, оригинал только в cookie — даже при утечке БД токены бесполезны
  - `@WebMvcTest` требует мокать JwtAuthenticationFilter и JwtService, иначе SecurityConfig не загружается
  - В H2 (тесты) Hibernate create-drop автоматически создаёт refresh_tokens, Flyway миграция не нужна
  - Разблокирован: TASK-006 (Spring Security конфигурация + rate limiting)
  - Следующий приоритет: TASK-006 (security, critical) — rate limiting и CORS, или TASK-008 (functional, critical) — таблица contexts

### TASK-008 — Flyway-миграция и JPA-сущность для таблицы Context
- **Дата:** 2026-05-17
- **Статус:** done
- **Что сделано:**
  - Создана Flyway-миграция `V3__create_contexts_table.sql` с PostgreSQL enum type `context_theme`
  - Таблица `contexts`: id (UUID PK), user_id (FK → users, ON DELETE RESTRICT), name (VARCHAR 100, NOT NULL), theme (context_theme ENUM, NOT NULL), icon (VARCHAR 50), sort_order (INTEGER, NOT NULL, default 0), is_deleted (BOOLEAN, NOT NULL, default false), created_at (TIMESTAMPTZ), updated_at (TIMESTAMPTZ)
  - Индексы: `idx_contexts_user_id` и частичный `idx_contexts_user_id_not_deleted` (WHERE is_deleted = FALSE)
  - Создан enum `ContextTheme` в пакете `com.gtd.backend.context.model` с 5 значениями: MINIMALIST, DESIGN, FORMAL, NATURE, DARK
  - Создана JPA-сущность `Context` с полным маппингом: @ManyToOne(LAZY) → User, @Enumerated(STRING), @Builder.Default для sortOrder и isDeleted, @PrePersist/@PreUpdate для timestamps
  - Создан `ContextRepository` (JpaRepository) с методами: findByUserIdAndIsDeletedFalseOrderBySortOrderAsc(), findByIdAndIsDeletedFalse(), countByUserIdAndIsDeletedFalse()
  - Написаны 12 тестов для ContextRepository: save, find active, exclude deleted, find by id, count active, sort order, user isolation, timestamps, all theme values, FK integrity
  - Все 66 тестов проходят (54 старых + 12 новых), проект собирается: `./mvnw clean package`
- **Коммиты:** feat: add Flyway V3 migration for contexts table and Context JPA entity
- **Заметки:**
  - Миграция использует PostgreSQL CREATE TYPE для enum — в H2 (тесты) это не выполняется (Flyway disabled), Hibernate create-drop создаёт таблицу автоматически с EnumType.STRING
  - Partial index `idx_contexts_user_id_not_deleted` оптимизирует самый частый запрос — получение активных контекстов пользователя
  - Разблокированы: TASK-009 (CRUD API для контекстов, нужен также TASK-007), TASK-010 (миграция и сущность Task)
  - Следующий приоритет: TASK-006 (security, critical) — единственная critical-задача с выполненными dependencies, или TASK-010 (functional, critical) — таблица tasks (зависит только от TASK-008, теперь done)

### TASK-006 — Spring Security конфигурация + rate limiting
- **Дата:** 2026-05-17
- **Статус:** done
- **Что сделано:**
  - Обновлён `SecurityConfig`: добавлена CORS-конфигурация через `CorsConfigurationSource` bean, rate limiting filter добавлен в цепочку фильтров после JWT-фильтра
  - Создан `CorsProperties` (@ConfigurationProperties prefix=cors): allowed-origins, allowed-methods, allowed-headers, allow-credentials, max-age — всё конфигурируется через application.yml/env
  - Создан `RateLimitProperties` (@ConfigurationProperties prefix=rate-limit): max-requests (default 100), window-ms (default 60000)
  - Создан `RateLimitingFilter` (OncePerRequestFilter): sliding window rate limiter на ConcurrentHashMap + ConcurrentLinkedDeque, ключ = userId (если авторизован) или IP (X-Forwarded-For / remoteAddr), возвращает 429 + Retry-After header при превышении лимита
  - CORS: по умолчанию разрешены http://localhost:3000 и http://localhost:5173, конфигурируется через `cors.allowed-origins` в application.yml
  - Rate limit: по умолчанию 100 запросов в минуту, конфигурируется через `rate-limit.max-requests` и `rate-limit.window-ms`
  - Добавлены секции cors и rate-limit в application.yml и test application.yml
  - Входные данные уже валидируются на всех эндпоинтах через Jakarta Validation (@Valid, @NotBlank, @Email, @Size) — добавлено в TASK-004
  - Написаны unit-тесты RateLimitingFilterTest (6 тестов: allow within limit, block on exceed, userId key, X-Forwarded-For key, remoteAddr key, separate tracking per client)
  - Написаны интеграционные тесты SecurityConfigIntegrationTest (5 тестов: 401 без токена, auth open, swagger open, CORS allowed origin, CORS rejected origin)
  - Написаны интеграционные тесты RateLimitingIntegrationTest (2 теста: 429 при превышении лимита, работа после сброса)
  - Обновлён AuthControllerTest — добавлены mock beans для новых зависимостей (RateLimitingFilter, RateLimitProperties, CorsProperties)
  - Все 79 тестов проходят, проект собирается: `./mvnw clean package`
- **Коммиты:** feat: add CORS configuration and rate limiting (100 req/min)
- **Заметки:**
  - Rate limiter in-memory — подходит для single-instance deployment. Для multi-instance нужен Redis-based (Bucket4j + Redis)
  - CORS origins конфигурируются через env: `CORS_ALLOWED_ORIGINS=https://app.example.com`
  - `RateLimitingFilter.clearBuckets()` используется в тестах для изоляции — package-private
  - Разблокирован: TASK-007 (Swagger/OpenAPI документация)
  - Следующий приоритет: TASK-007 (infrastructure, critical) — Swagger документация, или TASK-010 (functional, critical) — таблица tasks

### TASK-010 — Flyway-миграция и JPA-сущность для таблицы Task
- **Дата:** 2026-05-17
- **Статус:** done
- **Что сделано:**
  - Создана Flyway-миграция `V4__create_tasks_table.sql` с PostgreSQL enum type `gtd_list`
  - Таблица `tasks`: id (UUID PK), context_id (FK → contexts), parent_task_id (FK self-reference → tasks), gtd_list (ENUM, default INBOX), category_id (UUID, будет FK позже), title (VARCHAR 500, NOT NULL), notes (TEXT), due_date (TIMESTAMPTZ), reminder_settings (JSONB), recurrence_rule (JSONB), nesting_level (INTEGER, CHECK 1-4), sort_order (INTEGER), is_completed (BOOLEAN), completed_at (TIMESTAMPTZ), is_deleted (BOOLEAN), created_at (TIMESTAMPTZ), updated_at (TIMESTAMPTZ), version (INTEGER, optimistic locking)
  - Индексы: idx_tasks_context_id, idx_tasks_parent_task_id (partial, WHERE NOT NULL), idx_tasks_context_gtd_list (partial, WHERE NOT deleted), idx_tasks_context_not_deleted, idx_tasks_due_date (partial, WHERE NOT NULL/deleted/completed)
  - Создан enum `GtdList` в пакете `com.gtd.backend.task.model` с 8 значениями: INBOX, NEXT_ACTIONS, PROJECTS, WAITING_FOR, SOMEDAY_MAYBE, REFERENCE, CALENDAR, DONE
  - Создана JPA-сущность `Task` с полным маппингом: @ManyToOne(LAZY) → Context, self-referencing @ManyToOne → parentTask, @OneToMany → subtasks, @Enumerated(STRING), @JdbcTypeCode(JSON) для JSONB-полей, @Version для optimistic locking, @Builder.Default для дефолтов
  - Создан `TaskRepository` (JpaRepository) с методами: findByContextIdAndIsDeletedFalseOrderBySortOrderAsc(), findByContextIdAndGtdListAndIsDeletedFalseOrderBySortOrderAsc(), findByParentTaskIdAndIsDeletedFalseOrderBySortOrderAsc(), findByIdAndIsDeletedFalse(), countByContextIdAndIsDeletedFalse(), countByContextIdAndGtdListAndIsDeletedFalse()
  - Написаны 16 тестов для TaskRepository: save, find by context, filter by GTD list, find subtasks, exclude deleted subtasks, find by id, return empty when deleted, count active, count by GTD list, sort order, all GTD list values, self-reference (3 levels), optional fields (JSONB), timestamps, context isolation, FK to context
  - Все 95 тестов проходят (79 старых + 16 новых), проект собирается: `./mvnw clean package`
- **Коммиты:** feat: add Flyway V4 migration for tasks table and Task JPA entity
- **Заметки:**
  - JSONB-поля (reminder_settings, recurrence_rule) используют @JdbcTypeCode(SqlTypes.JSON) без columnDefinition — это обеспечивает совместимость с H2 (тесты) и PostgreSQL (prod/dev)
  - Self-reference (parent_task_id) с ON DELETE RESTRICT — нельзя удалить родителя пока есть подзадачи в БД
  - category_id пока UUID без FK — FK будет добавлен в TASK-014 когда появится таблица categories
  - Partial indices оптимизируют самые частые запросы: активные задачи контекста, фильтр по GTD-списку, задачи с приближающимся дедлайном
  - Разблокированы: TASK-011 (CRUD API для задач, нужен также TASK-009), TASK-014 (категории, нужен также TASK-009)
  - Следующий приоритет: TASK-007 (infrastructure, critical) — Swagger документация (разблокирует TASK-009, который разблокирует TASK-011 и TASK-014)

### TASK-007 — Swagger/OpenAPI документация для всех существующих эндпоинтов
- **Дата:** 2026-05-18
- **Статус:** done
- **Что сделано:**
  - Создан `OpenApiConfig` — конфигурация OpenAPI с информацией о проекте и JWT security scheme (bearerAuth)
  - JWT-авторизация настроена в Swagger: кнопка Authorize → ввод Bearer JWT token
  - Все auth-эндпоинты задокументированы OpenAPI аннотациями: @Operation, @ApiResponses, @Tag
  - Эндпоинты register, login, refresh, logout — описания, response codes (201, 200, 400, 401, 409), response schemas
  - DTO-классы аннотированы @Schema с описаниями и примерами: RegisterRequest, LoginRequest, AuthResponse, RegisterResponse, ErrorResponse
  - Auth-эндпоинты помечены `@SecurityRequirement(name = "")` — не требуют токен в Swagger UI
  - SecurityConfig уже разрешал /swagger-ui/**, /api-docs/**, /swagger-ui.html — проверено
  - application.yml уже имел springdoc секцию (api-docs.path=/api-docs, swagger-ui.path=/swagger-ui.html)
  - Все 95 тестов проходят, проект собирается без ошибок
- **Коммиты:** feat: add Swagger/OpenAPI documentation with JWT authorization
- **Заметки:**
  - springdoc-openapi-starter-webmvc-ui 2.8.6 уже был в pom.xml — дополнительных зависимостей не требовалось
  - Swagger UI доступен по http://localhost:8080/swagger-ui.html, OpenAPI spec по http://localhost:8080/api-docs
  - Global security scheme bearerAuth применяется ко всем эндпоинтам, auth-эндпоинты переопределяют через пустой @SecurityRequirement
  - Разблокирован: TASK-009 (CRUD API для контекстов с лимитом 5) — зависел от TASK-007 + TASK-008 (оба done)
  - Следующий приоритет: TASK-009 (functional, critical) — CRUD API для контекстов

### TASK-009 — CRUD API для контекстов с лимитом 5 контекстов
- **Дата:** 2026-05-18
- **Статус:** done
- **Что сделано:**
  - Создан `ContextService` в пакете `com.gtd.backend.context.service` — бизнес-логика CRUD контекстов с проверкой лимита (max 5) и прав доступа (user isolation)
  - Создан `ContextController` (`/api/v1/contexts`) с 5 эндпоинтами: GET (list), GET /{id}, POST, PUT /{id}, DELETE /{id}
  - Созданы DTO: `CreateContextRequest` (name, theme, icon обязательны), `UpdateContextRequest` (partial update), `ContextResponse`
  - Созданы исключения: `ContextNotFoundException` (404), `ContextAccessDeniedException` (403), `ContextLimitExceededException` (400)
  - Обновлён `GlobalExceptionHandler` — добавлены обработчики для 3 новых исключений
  - Все эндпоинты задокументированы OpenAPI аннотациями (@Operation, @ApiResponses, @Tag)
  - DELETE выполняет soft delete (is_deleted=true), контекст пропадает из листинга
  - При создании 6-го контекста возвращается 400 с сообщением о лимите
  - Удалённые контексты не учитываются в лимите (можно создать новый после удаления)
  - Контексты изолированы: пользователь видит/изменяет только свои (403 при попытке доступа к чужому)
  - Написаны unit-тесты ContextServiceTest (13 тестов: CRUD, trim, limit, ownership, not found)
  - Написаны controller-тесты ContextControllerTest (14 тестов: все endpoints + validation + error cases)
  - Написаны интеграционные тесты ContextIntegrationTest (10 тестов: full CRUD flow, limit enforcement, isolation between users, soft delete behaviour)
  - Исправлена проблема FK constraint в существующих интеграционных тестах — добавлен `contextRepository.deleteAll()` перед `userRepository.deleteAll()` в setUp() всех @SpringBootTest классов
  - Все 132 теста проходят (95 старых + 37 новых), проект собирается: `./mvnw clean package`
- **Коммиты:** feat: add CRUD API for contexts with 5-context limit
- **Заметки:**
  - SecurityContext propagation в @WebMvcTest: при addFilters=false нужен custom RequestPostProcessor для установки Authentication (SecurityContextHolder.getContext() не пробрасывается в MockMvc request thread)
  - `userRepository.getReferenceById()` используется вместо `findById()` при создании контекста — не делает лишний SELECT для проверки существования пользователя (JWT уже гарантирует)
  - sort_order при создании автоматически = текущему количеству контекстов (append to end)
  - Разблокированы: TASK-011 (CRUD API для задач, зависит от TASK-009 + TASK-010, оба done), TASK-014 (категории, зависит от TASK-009 + TASK-010, оба done)
  - Следующий приоритет: TASK-011 (functional, critical) — CRUD API для задач внутри контекста

### TASK-011 — CRUD API для задач внутри контекста
- **Дата:** 2026-05-18
- **Статус:** done
- **Что сделано:**
  - Создан `TaskService` в пакете `com.gtd.backend.task.service` — бизнес-логика CRUD задач с проверкой прав доступа (user isolation через context ownership)
  - Создан `TaskController` с 5 эндпоинтами на двух базовых путях:
    - GET /api/v1/contexts/{contextId}/tasks — список задач контекста (top-level, без подзадач)
    - GET /api/v1/contexts/{contextId}/tasks?gtd_list=INBOX — фильтр по GTD-списку
    - POST /api/v1/contexts/{contextId}/tasks — создать задачу (минимум: title)
    - GET /api/v1/tasks/{id} — получить задачу с прямыми подзадачами
    - PUT /api/v1/tasks/{id} — обновить задачу (partial update, только предоставленные поля)
    - DELETE /api/v1/tasks/{id} — soft delete
  - Созданы DTO: `CreateTaskRequest` (title обязателен, @NotBlank, @Size(max=500)), `UpdateTaskRequest` (partial update), `TaskResponse` (с subtasks для single GET)
  - Созданы исключения: `TaskNotFoundException` (404), `TaskAccessDeniedException` (403)
  - Обновлён `GlobalExceptionHandler` — добавлены обработчики для 2 новых task-исключений
  - Обновлён `TaskRepository` — добавлены 2 новых метода для фильтрации top-level задач (parentTask IS NULL): `findByContextIdAndParentTaskIsNullAndIsDeletedFalseOrderBySortOrderAsc()` и `findByContextIdAndGtdListAndParentTaskIsNullAndIsDeletedFalseOrderBySortOrderAsc()`
  - Новая задача по умолчанию: gtd_list=INBOX, nesting_level=1, sort_order=count of existing tasks
  - Все эндпоинты задокументированы OpenAPI аннотациями (@Operation, @ApiResponses, @Tag)
  - Написаны unit-тесты TaskServiceTest (20 тестов: CRUD, trim title, defaults, ownership, context not found, access denied)
  - Написаны controller-тесты TaskControllerTest (15 тестов: все endpoints + validation + error cases + GTD filter)
  - Написаны интеграционные тесты TaskIntegrationTest (11 тестов: full CRUD flow, GTD filter, isolation between contexts, isolation between users, soft delete, validation)
  - Все 178 тестов проходят (132 старых + 46 новых), проект собирается: `./mvnw clean package`
- **Коммиты:** feat: add CRUD API for tasks within context
- **Заметки:**
  - GET /contexts/{contextId}/tasks возвращает только top-level задачи (parentTask IS NULL) — подзадачи доступны через GET /tasks/{id}
  - TaskController использует два базовых пути: /api/v1/contexts/{contextId}/tasks для контекстных операций и /api/v1/tasks/{id} для операций с конкретной задачей
  - Ownership проверяется через context.user — задача принадлежит пользователю, если он владелец контекста
  - sort_order при создании = текущему количеству задач в контексте (append to end)
  - Разблокированы: TASK-012 (перемещение между GTD-списками), TASK-013 (вложенные подзадачи), TASK-014 (категории, если TASK-009 done), TASK-016 (напоминания)
  - Следующий приоритет: TASK-012 (functional, critical) — перемещение задач между GTD-списками и выполнение задач, или TASK-013 (functional, critical) — вложенные подзадачи до 4 уровней

### TASK-012 — Перемещение задач между GTD-списками и выполнение задач
- **Дата:** 2026-05-18
- **Статус:** done
- **Что сделано:**
  - Создан `MoveTaskRequest` DTO с валидацией (@NotNull gtdList)
  - Добавлены методы `moveTask()` и `completeTask()` в `TaskService`
  - `moveTask()` — меняет gtd_list задачи на указанный, использует `saveAndFlush()` для корректного отслеживания @Version
  - `completeTask()` — устанавливает is_completed=true, completed_at=now(), gtd_list=DONE
  - Нельзя переместить/выполнить удалённую задачу — `findByIdAndIsDeletedFalse()` вернёт 404
  - @Version (optimistic locking) инкрементируется при каждом изменении — проверено в интеграционных тестах
  - Добавлены 2 PATCH эндпоинта в `TaskController`:
    - PATCH /api/v1/tasks/{id}/move — принимает `{gtdList: "NEXT_ACTIONS"}`
    - PATCH /api/v1/tasks/{id}/complete — без тела запроса
  - Оба эндпоинта задокументированы OpenAPI аннотациями (@Operation, @ApiResponses)
  - Написаны unit-тесты TaskServiceTest (+7 тестов: moveTask success, not found, access denied; completeTask success, not found, access denied; version increment)
  - Написаны controller-тесты TaskControllerTest (+6 тестов: move success, 400 null gtdList, 404 deleted, complete success, 404, 403)
  - Написаны интеграционные тесты TaskIntegrationTest (+6 тестов: move to another list, complete, 404 on move deleted, version increment on move, version increment on complete, 403 on other user's task)
  - Все 197 тестов проходят (178 старых + 19 новых), проект собирается: `./mvnw clean package`
- **Коммиты:** feat: add move and complete task endpoints with optimistic locking
- **Заметки:**
  - Использован `saveAndFlush()` вместо `save()` для move/complete — гарантирует, что @Version инкрементируется и возвращается в ответе (важно для optimistic locking на клиенте)
  - Deleted задачи (soft delete) не видны через `findByIdAndIsDeletedFalse()` — поэтому move/complete на удалённую задачу автоматически возвращает 404
  - Разблокированы: TASK-021 (повторяющиеся задачи, зависит от TASK-012), TASK-023 (Sync API, зависит от TASK-012)
  - Следующий приоритет: TASK-013 (functional, critical) — вложенные подзадачи до 4 уровней с валидацией (зависит только от TASK-011, уже done)
