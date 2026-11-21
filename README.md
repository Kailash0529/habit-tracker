# Habit Tracker

A modern habit tracker web application for monitoring daily routines, streaks, and consistency using a GitHub-style activity contribution graph.

## Architecture

- **Backend**: Spring Boot 3.3.5 (Java 21), Spring Security, Spring Data JPA, Lombok, Flyway, springdoc-openapi (Swagger UI)
- **Database**: PostgreSQL 16 (production/docker), H2 (in-memory for development and testing)
- **Frontend**: React 18, Vite, TypeScript, Tailwind CSS, Recharts, Lucide Icons
- **Testing**: JUnit 5, Mockito, RestAssured, Playwright
- **Containerization**: Docker & Docker Compose

## Project Layout

```
habit-tracker/
├── backend/
│   ├── src/main/java/com/habittracker/
│   │   ├── common/       # Shared controllers (health), exceptions, DTOs
│   │   └── config/       # Security, OpenAPI, CORS configuration
│   ├── src/main/resources/
│   │   ├── db/migration/ # Flyway SQL migrations
│   │   └── application*.yml
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/              # React components and styling
│   ├── Dockerfile
│   └── package.json
├── docker-compose.yml
└── .env.example
```

## Running Locally

### 1. Backend
Ensure Java 21+ is available, then start the application:
```bash
cd backend
mvn spring-boot:run
```
Swagger UI will be available at: `http://localhost:8080/swagger-ui.html`

### 2. Frontend
```bash
cd frontend
npm install
npm run dev
```
Open `http://localhost:5173` in your browser.

### 3. Docker Compose
```bash
docker-compose up --build
```
