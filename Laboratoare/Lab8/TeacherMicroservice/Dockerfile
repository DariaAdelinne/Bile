FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/TeacherMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar
COPY teacher_database.txt teacher_database.txt
ENTRYPOINT ["java", "-jar", "app.jar"]
