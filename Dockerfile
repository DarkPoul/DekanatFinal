# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw
# Прискорить збірку, тести можна ввімкнути за потреби
RUN ./mvnw -DskipTests clean package

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# (опційно) шрифти, якщо справді потрібні для рендерів/PDF
RUN apt-get update && apt-get install -y fontconfig && rm -rf /var/lib/apt/lists/*
COPY src/main/resources/fonts /usr/local/share/fonts
RUN fc-cache -f -v || true

# Копіюємо готовий fat JAR
# Заміни назву jar, якщо інша (див. target/)
COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]