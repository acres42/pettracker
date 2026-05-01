# PetTracker

PetTracker is a Spring Boot web application for searching adoptable pets by type and location. It serves as a clean, extensible foundation for integrating real APIs and evolving into a production system.

## Features

- Server-rendered UI with Thymeleaf
- Search form (pet type and location)
- MVC architecture (Spring Boot)
- Test-driven development (JUnit + MockMvc)

## Tech Stack

- Java 21
- Spring Boot 4
- Thymeleaf
- JUnit 5
- MockMvc
- Maven

## Project Structure
```
pettracker/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── ac/
│   │   │           └── pettracker/
│   │   │               ├── controller/
│   │   │               │   └── PageController.java
│   │   │               ├── service/
│   │   │               │   └── PetService.java
│   │   │               ├── model/
│   │   │               │   └── Pet.java
│   │   │               └── PettrackerApplication.java
│   │   └── resources/
│   │       ├── templates/
│   │       │   ├── index.html
│   │       │   ├── search.html
│   │       │   └── results.html
│   │       └── static/
│   │           └── css/
│   │               └── styles.css
│   └── test/
│       └── java/
│           └── com/
│               └── ac/
│                   └── pettracker/
│                       ├── controller/
│                       │   └── PageControllerTest.java
│                       └── PettrackerApplicationTests.java
├── .gitignore
└── mvnw / mvnw.cmd
```
---

## Running Tests

```bash
./mvnw test
```
Tests use MockMvc to validate HTTP routes and ensure pages render correctly.

## Running the Application

```bash
./mvnw spring-boot:run
```
Then visit http://localhost:8080 in your browser.

## Example Routes
```
Route        Description
-----------------------------------------------------------
/                 Home page (index)

/search           Search form page
                  - Displays search UI
                  - Shows validation error (flash "error") if present

/pets/results     Search results page
                  - Query params:
                      type (String)
                      location (String)
                  - Behavior:
                      • Valid input → renders results.html with pets list
                      • Empty results → shows "No pets found" state
                      • Missing/blank input → redirects to /search with flash error
```
## Development Approach

This project follows a strict TDD workflow:

1. Write failing test (RED)
2. Implement minimal code (GREEN)
3. Refactor safely

Each milestone is committed only when stable.

## Design Goals

* Keep architecture boringly reliable
* Favor readability over cleverness
* Maintain strict layering
* Build production-ready foundations early

⸻

### Author

AC Roselee

* GitHub: https://github.com/acres42
* Portfolio: https://acres42.github.io/portfolio
