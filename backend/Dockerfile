# syntax=docker/dockerfile:1

# ---- Stage 1: build ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

# Dependency layer cached separately from source — only re-runs when pom.xml
# (or the wrapper itself) changes, not on every source edit.
COPY mvnw .
COPY .mvn/ .mvn/
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ---- Stage 2: runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -g 1001 mycellis && adduser -D -u 1001 -G mycellis mycellis
USER mycellis

COPY --from=build /build/target/*.jar /app/app.jar

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
