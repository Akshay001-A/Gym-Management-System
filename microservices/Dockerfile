
# Build stage: use Maven and Java 25
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Pass the service directory when building the image
ARG SERVICE_DIR

COPY ${SERVICE_DIR}/pom.xml ./pom.xml
COPY ${SERVICE_DIR}/src ./src

RUN mvn -B -DskipTests package

# Runtime stage: use a smaller Java 25 image
FROM eclipse-temurin:25-jre

WORKDIR /app

ARG SERVICE_DIR
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
