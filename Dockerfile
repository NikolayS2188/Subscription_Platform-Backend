# Многоступенчатая сборка для оптимизации размера:
FROM maven:3.9.5-eclipse-temurin-21 AS build

# Устанавливаем рабочую директорию:
WORKDIR /app

# Копируем pom.xml и зависимости для кэширования:
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем исходный код:
COPY src ./src

# Собираем приложение:
RUN mvn clean package

# Второй этап: создаем легковесный образ для запуска:
FROM eclipse-temurin:21-jre-alpine

# Устанавливаем рабочую директорию:
WORKDIR /app

# Создаем пользователя для безопасности:
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Копируем JAR файл из первого этапа с правами пользователя:
COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar

# Переключаемся на пользователя appuser:
USER appuser

# Открываем порт:
EXPOSE 8080

# Точка входа - запуск приложения:
ENTRYPOINT ["java", "-jar", "app.jar"]