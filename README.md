# PetTracker

PetTracker is a Spring Boot web application for searching adoptable pets by type, gender, age, and
keywords. It uses a server-rendered UI (Thymeleaf) and is designed to eventually integrate with the
RescueGroups API, but relies on database seed data as a fallback as of 2026-05-04.

## Current Status

- API integration: designed for RescueGroups API v5; not yet wired (relies on seed data fallback)
- Primary retry strategy: exponential backoff with jitter (in place for when API is connected)
- Alternative strategy: token bucket rate limiting (available for comparison/interview discussion)
- Search: type, gender, age band, and keyword filters supported
- Test status: full suite green

## Features

- Server-rendered UI with Thymeleaf
- Search by pet type, gender, age band, and keywords (up to 5 comma-separated terms)
- Unmatched keyword reporting on results page
- API-first search with local seed data fallback
- MVC architecture (controller/service/repository/dto)
- TDD workflow with unit and integration tests

## Tech Stack

- Java 21
- Spring Boot 4.1.0-SNAPSHOT
- Spring Web + RestClient
- Spring Security 6
- Thymeleaf
- Jackson
- JUnit 5 + MockMvc + Mockito
- Maven

## Architecture

Pet search path:

```text
Browser
    |
    v
SearchController
    |
    v
PetService
    |
    +--> RescueGroupsClient (primary API path, not yet connected)
    |       |
    |       +--> success: return API pets
    |       |
    |       +--> failure/empty: return empty list
    |
    +--> PetRepository (fallback: returns local seed data)
                    |
                    +--> return local pets
```

Retry logic used by `RescueGroupsClient`:

```text
Attempt 1
    |
    +-- 2xx --> success
    |
    +-- 4xx --> no retry, return empty list
    |
    +-- 5xx --> wait exponential backoff + jitter, retry

Attempt 2
    |
    +-- same rules

Attempt 3
    |
    +-- if still 5xx, stop and return empty list
```

## Retry Strategies In Repository

Two approaches are implemented:

1. Exponential backoff with jitter (wired into the app now)
2. Token bucket rate limiting (available for comparison and interview discussion)

Decision guide:

```text
Unknown API limits or general transient outages?
    -> Prefer exponential backoff + jitter

Known strict request-per-second quotas and fairness goals?
    -> Prefer token bucket
```

## Configuration

Main configuration is in `src/main/resources/application.yaml`.

Environment variables:

- `RESCUEGROUPS_API_KEY` (required for live API calls)
- `RESCUEGROUPS_BASE_URL` (optional, defaults to RescueGroups v5 URL)
- `RESCUEGROUPS_RETRY_BACKOFF_MS` (optional, default backoff base is `1000` ms)

Example local run configuration:

```bash
export RESCUEGROUPS_API_KEY="your-api-key"
./mvnw spring-boot:run
```

## Running

Run tests:

```bash
./mvnw test
```

Run application:

```bash
./mvnw spring-boot:run
```

Then open:

```text
http://localhost:8080
```

## Local MySQL with Docker

This project supports running MySQL locally via Docker and applying schema changes with Flyway
migrations.

1. Copy the local MySQL env template:

```bash
cp .env.mysql.example .env.mysql
```

2. Start MySQL:

```bash
docker compose --env-file .env.mysql up -d mysql
```

3. Run the app with the MySQL profile:

```bash
export SPRING_PROFILES_ACTIVE=mysql
./mvnw spring-boot:run
```

4. Stop MySQL when finished:

```bash
docker compose --env-file .env.mysql down
```

Schema migrations live in:

```text
src/main/resources/db/migration/mysql/
```

Current baseline schema:

- `user_accounts`
- `user_preferences`

## How Schema Design Works in Spring Boot

In Java/Spring Boot, the professional equivalent of JS ORM migrations is usually Flyway or
Liquibase.

Recommended workflow:

1. Design tables/relations as SQL migration files (`V1__...sql`, `V2__...sql`, etc.)
2. Commit each migration with code changes that depend on it
3. Keep `ddl-auto=validate` in MySQL environments so entities are checked against schema
4. Let Flyway apply migrations automatically on app startup

This gives predictable, reviewable schema history and is interview-friendly for senior reviewers.

## Routes

```text
Route            Description
------------------------------------------------------------
/                Home page (index)

/search          Search form page
                                 - Displays search UI
                                 - Shows validation error if present

/pets/results    Search results page
                                 - Query params: type, gender, ageBand, keywords
                                 - Valid input: renders results page
                                 - Empty results: shows no-results state
                                 - Missing/blank type: shows error page
```

## Project Structure

```text
pettracker/
|-- pom.xml
|-- src/
|   |-- main/
|   |   |-- java/com/ac/pettracker/
|   |   |   |-- controller/
|   |   |   |-- service/
|   |   |   |-- repository/
|   |   |   |-- model/
|   |   |   |-- dto/
|   |   |   `-- client/
|   |   |       |-- RescueGroupsClient.java
|   |   |       |-- RescueGroupsClientTokenBucket.java
|   |   |       `-- TokenBucket.java
|   |   `-- resources/
|   |       |-- templates/
|   |       |-- static/
|   |       `-- application.yaml
|   `-- test/
|       `-- java/com/ac/pettracker/
|           |-- controller/
|           |-- service/
|           |-- repository/
|           `-- client/
|               |-- RescueGroupsClientTest.java
|               |-- RescueGroupsClientRetryTest.java
|               |-- RescueGroupsClientTokenBucketTest.java
|               `-- TokenBucketTest.java
`-- mvnw / mvnw.cmd
```

## Development Approach

This project follows a strict TDD loop:

1. Write failing test
2. Implement minimal code to pass
3. Refactor while keeping tests green

## Author

AC Roselee

- GitHub: https://github.com/acres42
- Portfolio: https://acres42.github.io/portfolio
