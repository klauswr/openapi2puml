# docker build -t klauswr/openapi2puml .
# docker run -d -p 7000:7000 --name openapi2puml klauswr/openapi2puml:latest

FROM openjdk:11-jre-slim

RUN apt-get update && apt-get install -y plantuml graphviz && rm -rf /var/cache/apt/archives/*

VOLUME /tmp
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]