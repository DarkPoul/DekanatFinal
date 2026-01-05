# Dekanat

Spring Boot + Vaadin application for managing deanery workflows.

## Configuration

- Java 17, Maven Wrapper available (`./mvnw`).
- Database connection is taken from environment variables (`DB_URL`, `MYSQL_ROOT_USER`, `MYSQL_ROOT_PASSWORD`). See `docker-compose.yml` for a MySQL example.
- Default profile (`application.yaml`) disables automatic schema initialization and enforces `spring.jpa.hibernate.ddl-auto=validate`.

## Database migrations (Flyway)

Flyway runs automatically at application startup (classpath `db/migration`). Current versioned scripts:

- `V1__initial_schema.sql` — adapts `users.id` to auto-increment and provisions mail module tables.
- `V2__group_plan_bridge.sql` — introduces `group_plans` bridge and backfills data.

### Deploying to production

1. Build the artifact: `./mvnw -Pproduction clean package`.
2. Ensure database credentials are configured for the target environment.
3. Start the app with the `prod` profile (Flyway runs on boot and JPA validates the schema):  
   `SPRING_PROFILES_ACTIVE=prod java -jar target/Dekanat-0.0.1.jar`
4. Watch the startup log for `Flyway` entries before Vaadin initialization to confirm migrations executed.

### Running migrations separately

- Trigger migrations without starting the UI:  
  `SPRING_PROFILES_ACTIVE=prod ./mvnw -DskipTests spring-boot:run -Dspring-boot.run.arguments=\"--spring.main.web-application-type=none\"`
- For existing databases created before Flyway, run the first deployment with `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` to baseline the current schema, then revert the flag for subsequent runs. Migration `V2` will still apply after baselining.
- Flyway clean is disabled in configuration; drop/cleanup must be done manually if ever required.

### Local development

- Start with `./mvnw spring-boot:run` (or add `-Dspring-boot.run.profiles=local`).
- Use `./mvnw test` to run the JUnit suite.
