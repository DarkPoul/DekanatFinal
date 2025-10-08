# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw
# Ключове: профіль production (Vaadin збере фронт), тести пропускаємо
RUN ./mvnw -Pproduction -DskipTests clean package

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# (опційно) шрифти
RUN apt-get update && apt-get install -y fontconfig && rm -rf /var/lib/apt/lists/*
COPY src/main/resources/fonts /usr/local/share/fonts
RUN fc-cache -f -v || true

# Кладемо зібраний fat JAR
COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080
ENV JAVA_OPTS=""
# ВАЖЛИВО: вимикаємо Dev Server на всяк випадок
ENV VAADIN_DISABLE_DEV_SERVER=true
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
