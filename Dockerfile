# ---- Build stage: compile the executable JAR with Maven + JDK 21 ----
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only the pom first so the dependency layer is cached until pom.xml changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests run in the Jenkins Test stage, so skip them here
RUN mvn -B -q package -DskipTests

# ---- Runtime stage: slim JRE image with just the JAR ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build --chown=app:app /app/target/calculator.jar app.jar
USER app

EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
