# Этап 1: Сборка зависимостей
FROM maven:3.9.6-eclipse-temurin-21 AS deps

WORKDIR /app

# Копируем только pom.xml для кэширования зависимостей
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Этап 2: Сборка приложения
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Копируем зависимости из предыдущего этапа
COPY --from=deps /root/.m2 /root/.m2

# Копируем исходный код
COPY pom.xml .
COPY src ./src

# Собираем приложение
RUN mvn clean package -DskipTests

# Этап 3: Запуск
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Создаем пользователя без root прав
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Копируем jar файл
COPY --from=builder /app/target/*.jar app.jar

# Копируем статические файлы (изображения)
COPY --from=builder /app/src/main/resources/static /app/static

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/ || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]