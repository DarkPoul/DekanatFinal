# ---------- build stage ----------
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app
COPY . .

RUN chmod +x mvnw

# важливо: викликаємо профіль production,
# він тепер робить prepare-frontend + build-frontend
RUN ./mvnw clean package -Pproduction -DskipTests

# ---------- runtime stage ----------
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN apt-get update && \
    apt-get install -y fontconfig && \
    rm -rf /var/lib/apt/lists/*

COPY src/main/resources/fonts /usr/local/share/fonts
RUN fc-cache -f -v || true

COPY --from=build /app/target/Dekanat-0.0.1.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
