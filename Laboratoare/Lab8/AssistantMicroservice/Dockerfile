FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/AssistantMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar
COPY students_codes.txt students_codes.txt
ENTRYPOINT ["java", "-jar", "app.jar"]
