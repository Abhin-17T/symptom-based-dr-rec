# CarePath — Symptom-Based Doctor Recommendation & Appointment System

A full-stack Java project for the S3 CSE Object-Oriented Programming subject. It has a polished responsive web interface, Spring Boot REST backend, **JDBC via Spring JdbcTemplate**, and MySQL persistence. User registration and appointments are saved in MySQL; sessions keep the user signed in.

> **Important:** This is an educational demonstration, not a clinical service. Symptom matching is rule-based and cannot diagnose. Temporary self-care text is general information, not a prescription. It explicitly directs emergencies to urgent care.

## Features

- Patient registration, login, logout, password hashing with BCrypt, and session-based authentication.
- Symptom selection and free-text input, rule-based specialty suggestion, safety messaging, and non-prescriptive self-care guidance.
- Doctor directory with specialties, experience, ratings, bio, availability days and accepting-bookings status.
- Appointment requests, day/time validation, unique doctor/time slot protection, appointment history and patient cancellation.
- Admin dashboard with live counts, request confirmation/cancellation/completion, and doctor booking availability controls.
- MySQL schema and sample doctor data are initialized automatically at startup. An admin account is seeded on first startup.

## Tech stack

- Java 17, Spring Boot 3.3, Spring Web, Spring JDBC, MySQL Connector/J
- MySQL 8+ (or compatible MySQL/MariaDB)
- HTML, CSS and vanilla JavaScript; no frontend build required

## Run locally

1. Install Java 17+, Maven 3.8+, and MySQL 8+.
2. Start the MySQL service and create a database, if you prefer to create it manually:

   ```sql
   CREATE DATABASE medisuggest;
   ```

   The default JDBC URL also has `createDatabaseIfNotExist=true`.
3. From this directory, set your database credentials (defaults: `root` and blank password) and run:

   **Linux/macOS**
   ```bash
   export DB_USER=root
   export DB_PASSWORD='your-mysql-password'
   export ADMIN_EMAIL='admin@medisuggest.local'
   export ADMIN_PASSWORD='ChangeMe123!'
   mvn spring-boot:run
   ```

   **Windows PowerShell**
   ```powershell
   $env:DB_USER='root'
   $env:DB_PASSWORD='your-mysql-password'
   $env:ADMIN_EMAIL='admin@medisuggest.local'
   $env:ADMIN_PASSWORD='ChangeMe123!'
   mvn spring-boot:run
   ```
4. Open **http://localhost:8080**.

`DB_URL` can override the JDBC URL. Example:
`jdbc:mysql://localhost:3306/medisuggest?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`

For a demo project, default environment values are provided in `application.properties`. **Change the admin password and database credentials for any deployment**; do not deploy with demo defaults. A production version should add CSRF protection, email verification, rate limiting, audit logs, HTTPS, more robust scheduling/slot handling, and clinician-reviewed medical content.

## Demo accounts

- Admin: configured by `ADMIN_EMAIL` and `ADMIN_PASSWORD`; default is `admin@medisuggest.local` / `Admin123!`.
- Patient: create one using the registration form.

The initial admin is created only if that email is not already present. If you change the configured email after first run, the new admin is created at startup. Avoid using the demo admin password outside a local demo.

## Main project structure

```
src/main/java/edu/project/medisuggest/
  MediSuggestApplication.java   Spring Boot entry point and admin seeding
  ApiController.java            REST API, validation, recommendations and JDBC operations
src/main/resources/
  application.properties        DB and application configuration
  schema.sql                    MySQL tables and sample doctor records
  static/index.html             Web page
  static/styles.css             Responsive visual styling
  static/app.js                 Frontend flows and API integration
```

## API overview

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Create patient account |
| POST | `/api/auth/login` | Start session |
| POST | `/api/auth/logout` | End session |
| GET | `/api/auth/me` | Current session |
| GET | `/api/doctors` | Doctor directory |
| POST | `/api/recommend` | Rule-based symptom-to-specialty match |
| POST | `/api/appointments` | Request appointment (signed-in patient) |
| GET | `/api/appointments/mine` | Patient's appointments |
| DELETE | `/api/appointments/{id}` | Cancel own pending/confirmed appointment |
| GET | `/api/admin/stats` | Admin summary |
| GET | `/api/admin/appointments` | All appointment requests (admin) |
| PATCH | `/api/admin/appointments/{id}` | Update request status (admin) |
| PATCH | `/api/admin/doctors/{id}` | Toggle doctor availability (admin) |

## OOP / project discussion points

The controller separates HTTP workflows from the UI; records model request data; helper methods centralize identity/role checks; and the recommendation rules are isolated in a single method that can be replaced by a service/rules engine. BCrypt hashes passwords before persistence. JDBC uses parameterized SQL, and foreign keys plus a unique doctor/date/time constraint protect appointment data. A further refactor could introduce service and repository interfaces for a larger deployment.
