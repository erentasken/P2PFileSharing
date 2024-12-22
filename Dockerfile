# Stage 1: Build the application
FROM maven:3.8.6-openjdk-11-slim as build

WORKDIR /app

# Copy pom.xml and resolve dependencies (without copying the source code)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code and build the application
COPY src /app/src
RUN mvn clean install

# Stage 2: Runtime environment
FROM openjdk:11-jre-slim

WORKDIR /app

# Copy the built JAR file from the build stage
COPY --from=build /app/target/file-management-1.0-SNAPSHOT-jar-with-dependencies.jar /app/file-management.jar

# Expose the port used by the P2PRecog application (adjust if needed)
EXPOSE 9876

# Run the P2PRecog class explicitly (adjust the entry point if needed)
ENTRYPOINT ["java", "-jar", "/app/file-management.jar"]
