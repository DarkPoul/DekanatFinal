# ---------- Build ----------
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw
# ключове: зібрати фронт у JAR
RUN ./mvnw -Pproduction -DskipTests clean package

# ---------- Runtime ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# (за потреби шрифти)
RUN apt-get update && apt-get install -y fontconfig && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar /app/app.jar
ENV VAADIN_DISABLE_DEV_SERVER=true
EXPOSE 8080
ENTRYPOINT ["sh","-c","java -jar /app/app.jar"]
