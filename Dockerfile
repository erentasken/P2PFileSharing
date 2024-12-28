# FROM maven:3.8.6-openjdk-11-slim as build

# WORKDIR /app

# COPY pom.xml .
# RUN mvn dependency:go-offline

# COPY src /app/src
# RUN mvn clean install


# FROM openjdk:11-jre-slim

# WORKDIR /app

# # COPY target/file-management-1.0-SNAPSHOT-jar-with-dependencies.jar /app/file-management.jar
# COPY --from=build /app/target/file-management-1.0-SNAPSHOT-jar-with-dependencies.jar /app/file-management.jar

# EXPOSE 9876

# ENTRYPOINT ["java", "-jar", "/app/file-management.jar"]


FROM openjdk:11-jre-slim

WORKDIR /app

COPY target/file-management-1.0-SNAPSHOT-jar-with-dependencies.jar /app/file-management.jar

COPY sharedFiles /app/sharedFiles

EXPOSE 9876

ENTRYPOINT ["java", "-jar", "/app/file-management.jar"]
