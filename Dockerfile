FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew build.gradle settings.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q
COPY src src
RUN ./gradlew bootJar --no-daemon -x test -x nativeCompile

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
