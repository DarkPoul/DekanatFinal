# ---------- build stage ----------
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app
COPY . .

# Забезпечуємо можливість виконання wrapper
RUN chmod +x mvnw

# Збірка jar без тестів (швидше), з підготовкою Vaadin
RUN ./mvnw clean package -DskipTests

# Знайдемо згенерований jar
# Припустимо, що після збірки ми маємо target/Dekanat-0.0.1.jar
# (якщо назва інша — підкоригуй нижче)
# ---------- runtime stage ----------
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Шрифти для iText (як ти робив)
RUN apt-get update && \
    apt-get install -y fontconfig && \
    rm -rf /var/lib/apt/lists/*

# Копіюємо шрифти (якщо є)
COPY src/main/resources/fonts /usr/local/share/fonts
RUN fc-cache -f -v || true

# Копіюємо готовий jar з білд-стейджа
COPY --from=build /app/target/Dekanat-0.0.1.jar /app/app.jar

# Експонуємо порт, на якому працює Spring у контейнері
EXPOSE 8080

# Запуск із активним профілем test
ENV SPRING_PROFILES_ACTIVE=test

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
