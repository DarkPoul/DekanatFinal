# Вказуємо базовий образ для зборки
FROM eclipse-temurin:17-jdk-jammy AS build

# Копіюємо всі файли проекту в контейнер
WORKDIR /app
COPY . .

# Виконуємо збірку проекту, включаючи Maven Wrapper
RUN chmod +x mvnw
RUN ./mvnw clean package

# Вказуємо базовий образ для виконання
FROM eclipse-temurin:17-jdk-jammy

# Копіюємо файли проекту з попереднього образу
WORKDIR /app
COPY --from=build /app .

# Install fontconfig so the converter can find the bundled fonts
RUN apt-get update && \
    apt-get install -y fontconfig && \
    rm -rf /var/lib/apt/lists/*

# Copy bundled fonts if provided
COPY src/main/resources/fonts /usr/local/share/fonts
RUN fc-cache -f -v

# Встановлюємо Maven Wrapper як виконуваний файл
RUN chmod +x mvnw

# Вказуємо порти для додатку
EXPOSE 8080

# Команда для запуску додатку
CMD ["./mvnw", "spring-boot:run"]