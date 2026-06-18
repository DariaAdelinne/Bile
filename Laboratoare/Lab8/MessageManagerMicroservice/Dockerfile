FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/MessageManagerMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
