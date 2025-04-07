# Вказуємо базовий образ для зборки
FROM eclipse-temurin:17-jdk-jammy AS build

RUN apt-get update && \
    apt-get install -y \
    software-properties-common \
    apt-transport-https \
    && add-apt-repository universe && \
    apt-get update && \
    apt-get install -y \
    ttf-mscorefonts-installer \
    fontconfig \
    --no-install-recommends && \
    fc-cache -f -v

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

# Встановлюємо Maven Wrapper як виконуваний файл
RUN chmod +x mvnw

# Вказуємо порти для додатку
EXPOSE 8080

# Команда для запуску додатку
CMD ["./mvnw", "spring-boot:run"]