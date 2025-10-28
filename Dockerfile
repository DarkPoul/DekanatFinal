# ---------- build stage ----------
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app
COPY . .

RUN chmod +x mvnw

# Збираємо прод-jar з готовим фронтендом Vaadin
# Використовуємо профіль "production" з pom.xml
RUN ./mvnw clean package -Pproduction -DskipTests

# ---------- runtime stage ----------
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Для pdf/шрифтів
RUN apt-get update && \
    apt-get install -y fontconfig && \
    rm -rf /var/lib/apt/lists/*

# Якщо є кастомні шрифти
COPY src/main/resources/fonts /usr/local/share/fonts
RUN fc-cache -f -v || true

# Копіюємо зібраний jar
COPY --from=build /app/target/Dekanat-0.0.1.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
