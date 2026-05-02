# PetTracker

PetTracker is a Spring Boot web application for searching adoptable pets by type and location.
It uses a server-rendered UI (Thymeleaf), integrates with the RescueGroups API, and falls back to
local repository data when API results are unavailable.

## Current Status

- API provider: RescueGroups API v5
- Primary retry strategy in production path: exponential backoff with jitter
- Alternative strategy implemented for comparison/interview work: token bucket rate limiting
- Test status: full suite green

## Features

- Server-rendered UI with Thymeleaf
- Search form (pet type and location)
- API-first search with local data fallback
- MVC architecture (controller/service/repository)
- TDD workflow with unit and integration tests

## Tech Stack

- Java 21
- Spring Boot 4.1.0-SNAPSHOT
- Spring Web + RestClient
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
PageController
    |
    v
PetService
    |
    +--> RescueGroupsClient (primary API path)
    |       |
    |       +--> success: return API pets
    |       |
    |       +--> failure/empty: return empty list
    |
    +--> PetRepository (fallback path when API returns empty)
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

## Routes

```text
Route            Description
------------------------------------------------------------
/                Home page (index)

/search          Search form page
                                 - Displays search UI
                                 - Shows validation error if present

/pets/results    Search results page
                                 - Query params: type, location
                                 - Valid input: renders results page
                                 - Empty results: shows no-results state
                                 - Missing/blank input: redirects to /search
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
|   |   |   `-- integration/
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
|           `-- integration/
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
