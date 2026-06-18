FROM gradle:8.7-jdk17 AS build
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .
RUN gradle clean installDist --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /home/gradle/project/build/install/Lab11 /app
COPY numbers.txt /app/numbers.txt
EXPOSE 8080
ENTRYPOINT ["/app/bin/Lab11"]