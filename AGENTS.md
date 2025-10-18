# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java`: Spring Boot + Vaadin app code under `com.esvar.dekanat` (e.g., `*Entity`, `*DTO`, `*View`, `*Generator`).
- `src/main/resources`: configuration (`application.yaml`, `application-*.yaml`), templates, static assets.
- `src/main/frontend`: Vaadin frontend; `src/main/bundles` contains dev bundle.
- `src/test/java`: JUnit tests.
- Root: `pom.xml`, `mvnw(.cmd)`, `Dockerfile`, `docker-compose.yml`, `.env` (database credentials), `uploads/` (generated/uploads).

## Build, Test, and Development Commands
- Run locally (Unix/Windows): `./mvnw spring-boot:run` / `mvnw.cmd spring-boot:run`.
  - With profile: `-Dspring-boot.run.profiles=local`.
- Test: `./mvnw test` (runs JUnit 5 and Spring tests).
- Package: `./mvnw clean package` (jar in `target/`). For optimized Vaadin build: `./mvnw -Pproduction clean package`.
- Run jar: `java -jar target/Dekanat-0.0.1.jar` (add `--spring.profiles.active=local` as needed).
- Docker (optional): `docker compose up -d` to start app + MySQL per `docker-compose.yml`.

## Coding Style & Naming Conventions
- Java 17, 4‑space indentation, UTF‑8. Prefer Lombok for boilerplate (`@Getter/@Setter/@Builder`).
- Naming: classes `PascalCase`; fields/methods `camelCase`; constants `UPPER_SNAKE_CASE`.
- Patterns used: persistence models end with `Entity`, data carriers with `DTO`, Vaadin views with `View`.

## Testing Guidelines
- Frameworks: JUnit 5 (`spring-boot-starter-test`), Spring Security test.
- Location: `src/test/java`; name tests `*Test.java` mirroring package of target class.
- Run: `./mvnw test`. For profile-specific config, activate with `-Dspring.profiles.active=test`.
- Prefer slicing/mocks for DB layers; avoid relying on external MySQL in unit tests.

## Commit & Pull Request Guidelines
- History shows versioned release commits (e.g., “Dekanat v.alfa.x.y”). Use clear, present‑tense messages for changes; group related edits.
- Commits: scope + intent (e.g., `entity: add StudentRatingEntity`) and small, logical diffs.
- PRs: include description, linked issues, screenshots for UI changes, and a brief test plan (commands run, profiles used). Ensure builds/tests pass.

## Security & Configuration Tips
- Secrets live in `.env` (e.g., `DB_URL`, `MYSQL_ROOT_USER`, `MYSQL_ROOT_PASSWORD`). Do not commit real credentials; prefer a local `.env` and add an example file when changing config.
- Profiles: default uses env vars; `local` disables SSL and uses `uploads/`. Rotate any leaked secrets.

