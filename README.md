# Task Management Service

REST API for task management with filtering, pagination, sorting, audit fields (creation, modification, assignment), OpenAPI documentation, rate limiting, and Docker support.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Setup and Run](#setup-and-run)
- [Data Model](#data-model)
- [API Overview](#api-overview)
- [OpenAPI Documentation](#openapi-documentation)
- [Architecture](#architecture)
- [Performance and Scalability](#performance-and-scalability)
- [Security Considerations](#security-considerations)
- [Deployment Strategies](#deployment-strategies)
- [Configuration and Profiles](#configuration-and-profiles)
- [Tests](#tests)

---

## Project

- **Name:** Task Management Service  
- **Folder:** `task-management-service` (e.g. under `~/Downloads/task-management-service`)  
- **Package:** `com.taskmanagement`  
- **Main class:** `com.taskmanagement.TaskManagementApplication`  
- **Maven:** `groupId` `com.taskmanagement`, `artifactId` `task-management-service`

---

## Tech Stack

| Technology | Purpose |
|------------|---------|
| Java 21 | Runtime |
| Spring Boot 3.3.x | Web, Data JPA, Validation |
| Hibernate / JPA | Persistence (parameterized queries) |
| H2 | In-memory DB (default); replaceable via config |
| Lombok | Boilerplate reduction |
| Springdoc OpenAPI 2.x | OpenAPI 3 + Swagger UI |
| Bucket4j | In-memory rate limiting |
| JUnit 5 & Mockito | Unit tests |

---

## Setup and Run

### Prerequisites

- **JDK 21+**
- **Maven 3.6+** (or use included `./mvnw`)
- **Docker & Docker Compose** (optional, for containerized run)

### Option A: Manual (local)

1. **Enter the project**
   ```bash
   cd /path/to/task-management-service
   ```
   (e.g. `cd ~/Downloads/task-management-service` if you keep it in Downloads.)

2. **Build**
   ```bash
   ./mvnw clean package
   ```

3. **Run**
   ```bash
   ./mvnw spring-boot:run
   ```
   Or with a profile:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=qa
   ```

4. **Verify**
   - API base: **http://localhost:8080/api/tasks**
   - Swagger UI: **http://localhost:8080/swagger-ui.html**
   - OpenAPI JSON: **http://localhost:8080/v3/api-docs**
   - H2 console (default profile): **http://localhost:8080/h2-console**

### Option B: Docker

1. **Build image**
   ```bash
   docker build -t task-management-service .
   ```

2. **Run container**
   ```bash
   docker run -p 8080:8080 task-management-service
   ```

3. **Or use Docker Compose**
   ```bash
   docker-compose up --build
   ```
   Service is available at **http://localhost:8080**.

---

## Data Model

The **Task** entity and API track:

| Area | Fields | Notes |
|------|--------|--------|
| **Creation** | `createdAt`, `createdBy` | `createdAt` set automatically on insert; `createdBy` optional (e.g. from auth context). |
| **Modification** | `updatedAt`, `updatedBy` | `updatedAt` set on insert and update; `updatedBy` optional. |
| **Assignment** | `assignedTo`, `assignedAt` | Optional; `assignedAt` set when `assignedTo` is set (create or update). |

Core fields: `id`, `title` (required, max 100), `description` (optional, max 2000), `isCompleted`, `dueDate`.  
Request/response DTOs expose these plus the audit/assignment fields where applicable.

---

## API Overview

Base path: **`/api/tasks`**

| Method | Path | Description | Status codes |
|--------|------|-------------|--------------|
| `GET` | `/api/tasks` | List with **filtering**, **pagination**, **sorting** | 200 |
| `GET` | `/api/tasks/{id}` | Get by ID | 200, 404 |
| `POST` | `/api/tasks` | Create | 201, 400 |
| `PUT` | `/api/tasks/{id}` | Update | 200, 400, 404 |
| `DELETE` | `/api/tasks/{id}` | Delete | 204, 404 |

### GET /api/tasks – Filtering, pagination, sorting

- **Query parameters**
  - `completed` (boolean, optional): filter by completion status.
  - `assignedTo` (string, optional): filter by assignee (case-insensitive).
  - `page` (int, default 0): page index.
  - `size` (int, default 20, max 100): page size.
  - `sort` (string, optional): e.g. `createdAt,desc`, `title,asc`, multiple allowed.

- **Response**: `PagedTaskResponse`
  - `content`: array of `TaskResponse`
  - `page`, `size`, `totalElements`, `totalPages`, `first`, `last`

Example:
```http
GET /api/tasks?completed=false&assignedTo=john&page=0&size=10&sort=createdAt,desc&sort=id,asc
```

---

## OpenAPI Documentation

API is described with **OpenAPI 3** and served by **Springdoc**.

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
  Interactive UI to try endpoints.
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)  
  Machine-readable spec for codegen or tooling.

Controller methods are annotated with `@Operation`, `@ApiResponse`, and `@Parameter` where useful.  
Configuration: `OpenApiConfig` and `springdoc.*` in `application.yaml`.

---

## Architecture

### Layered design (LLD-style)

- **Controller** – HTTP only; delegates to `TaskService` (interface).
- **Service** – `TaskService` interface, `TaskServiceImpl` implementation; business rules and transaction boundaries.
- **Repository** – `TaskRepository` (JPA + `JpaSpecificationExecutor`); data access only.
- **Mapper** – `TaskMapper`; entity ↔ DTO mapping (functional style).
- **DTOs** – Request/response models separate from entity; validation on request DTOs.
- **Exception** – Custom exceptions and `GlobalExceptionHandler` for consistent API error bodies.

### Loose coupling

- **Controller depends on `TaskService` interface**, not the impl. Swapping or testing with a stub is straightforward.
- **Repository is an interface**; persistence can be replaced (e.g. different store) without changing service API.
- **Mapper is a component**; mapping logic is centralized and testable.
- **Specification pattern** (`TaskSpecification`) keeps query logic out of the repository interface and supports dynamic filters.

### Contrast with alternatives

| Aspect | Used here | Alternative |
|--------|-----------|-------------|
| Service | Interface + single impl | Concrete class only (tighter coupling to impl). |
| List API | Paginated + filters + sort | Unbounded list (risk of large responses). |
| Data access | JPA + Specification | Raw SQL or multiple fixed repository methods. |
| Errors | Global handler + `ApiError` + codes | Ad-hoc messages and status codes. |
| API docs | OpenAPI + Springdoc | Manual docs or no docs. |
| Rate limiting | Bucket4j per client (in-memory) | None or gateway-level only. |

---

## Performance and Scalability

### Relevant metrics

- **Latency**: p50/p95/p99 for list and get-by-id (influenced by DB and page size).
- **Throughput**: Requests per second under load; watch for rate limiting (429).
- **DB**: Connection pool usage (HikariCP), query time, N+1 (avoided by single query with Specification + Pageable).
- **Memory**: JVM heap; in-memory rate-limit buckets and H2 (dev) vs external DB (prod).

### Scalability approaches

- **Stateless app**: No session state; horizontal scaling by adding instances behind a load balancer.
- **Pagination**: List API is paginated (default 20, max 100) to avoid large payloads and heavy queries.
- **DB scaling**: Use a proper RDBMS in prod; read replicas and connection pooling as needed.
- **Caching**: Can add response or entity caching (e.g. Spring Cache) for read-heavy workloads.
- **Rate limiting**: Per-client limits (Bucket4j) protect a single instance; for multi-instance, use a shared store (e.g. Redis) or API gateway limits.

---

## Security Considerations

Documented and applied where applicable in this service:

| Topic | Mitigation |
|-------|------------|
| **SQL injection** | JPA/Hibernate and `Specification` use **parameterized queries** only; no string-concatenated SQL. |
| **XSS** | JSON API returns `Content-Type: application/json`; clients should not render request/response as HTML. For future HTML views, encode output and consider CSP. |
| **Rate limiting** | **Bucket4j** filter on `/api/*`; default 60 requests/minute per client IP (configurable via `app.rate-limit.requests-per-minute`). Returns 429 with a JSON body when exceeded. |
| **Secure password hashing** | No user passwords in this service. When adding auth, use **bcrypt** (e.g. `BCryptPasswordEncoder`) or Argon2; never store plaintext or weak hashes. |
| **Sensitive data** | Do not log request/response bodies in prod. H2 console and `show-sql` disabled in prod profile. |
| **HTTPS** | Use TLS in production (terminated at load balancer or in-app). |
| **Authn/Authz** | Not implemented; add Spring Security (e.g. JWT or session) and set `createdBy`/`updatedBy` from principal. |

---

## Deployment Strategies

For **production** deployment:

1. **Rolling update** – Deploy new version instance-by-instance; no downtime if app is stateless and backward-compatible.
2. **Blue-green** – Two environments (blue/green); switch traffic after validation; quick rollback by switching back.
3. **Canary** – Route a small share of traffic to new version; increase gradually; roll back on errors.
4. **Configuration** – Use `application-prod.yaml` and env-specific `spring.datasource.*`; avoid embedding secrets in code or config files; use a secret manager or env vars.

Containers: use the provided **Dockerfile** (multi-stage build, non-root user). Orchestration (e.g. Kubernetes) can use the same image with appropriate probes and resource limits.

---

## Configuration and Profiles

| Profile | Port | Use case |
|---------|------|----------|
| default | 8080 | Local dev; H2 in-memory; H2 console on. |
| qa | 8081 | QA; `show-sql` true; DEBUG for `com.taskmanagement`. |
| stg | 8082 | Staging; H2 console off; `ddl-auto: update`. |
| prod | 8080 | Production; H2 console off; `ddl-auto: validate`; set DB via env. |

Key settings:

- **Rate limit**: `app.rate-limit.requests-per-minute` (default 60).
- **Springdoc**: `springdoc.api-docs.path`, `springdoc.swagger-ui.path`.
- **JPA**: `spring.jpa.hibernate.ddl-auto`, `spring.jpa.show-sql` (off in prod).

Run with profile:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## Tests

```bash
./mvnw test
```

- **TaskServiceTest** – `TaskServiceImpl`: findAll (paged + filters), findById, create, update, delete; not-found and validation.
- **TaskControllerTest** – `TaskController`: status codes, paged response shape, validation (400), not-found (404), create (201 + Location), delete (204).

---

## License

Educational / reference use.
