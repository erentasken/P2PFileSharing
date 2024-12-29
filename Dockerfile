FROM openjdk:11

RUN apt-get update && apt-get install -y \
    x11-apps \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libxext6 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/cse471-1.0-SNAPSHOT-jar-with-dependencies.jar /app/cse471.jar

COPY sharedFiles /app/sharedFiles

EXPOSE 9876

ENV DISPLAY=:0  

ENTRYPOINT ["java", "-jar", "/app/cse471.jar"]
