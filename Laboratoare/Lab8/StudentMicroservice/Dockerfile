FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/StudentMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar
COPY questions_database.txt questions_database.txt
ENTRYPOINT ["java", "-jar", "app.jar"]
