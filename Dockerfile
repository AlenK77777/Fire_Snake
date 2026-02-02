# ==========================================
# Dockerfile для Fire Snake Game
# Multi-stage build: сборка + запуск
# ==========================================

# ==========================================
# STAGE 1: Сборка приложения
# ==========================================
FROM maven:3.9-eclipse-temurin-21 AS builder

# Рабочая директория для сборки
WORKDIR /app

# Копируем файл зависимостей отдельно для кэширования слоёв Docker
COPY pom.xml .

# Загружаем зависимости (кэшируется, если pom.xml не изменился)
RUN mvn dependency:go-offline -B

# Копируем исходный код
COPY src ./src

# Собираем JAR-файл
# -DskipTests - пропускаем тесты (они запускаются в Jenkins)
# -B - batch mode (без интерактивного вывода)
RUN mvn clean package -DskipTests -B

# ==========================================
# STAGE 2: Финальный образ для запуска
# ==========================================
FROM eclipse-temurin:21-jre

# Метаданные образа
LABEL maintainer="Fire Snake Project"
LABEL version="1.0.0"
LABEL description="Fire Snake - Classic Snake game with shooting mechanics"

# Рабочая директория
WORKDIR /app

# Копируем собранный JAR из builder stage
COPY --from=builder /app/target/fire-snake-game-1.0.0.jar ./fire-snake-game.jar

# Создаём непривилегированного пользователя для безопасности
RUN groupadd -r firesnake && useradd -r -g firesnake firesnake
RUN chown -R firesnake:firesnake /app
USER firesnake

# Точка входа - запуск игры
ENTRYPOINT ["java", "-jar", "fire-snake-game.jar"]
