FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY checkstyle.xml .
COPY src ./src
RUN mvn -B -ntp package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app

ARG APP_VERSION=dev
ENV APP_VERSION=${APP_VERSION}

RUN groupadd --system app && useradd --system --gid app --no-create-home app

COPY --from=build /app/target/*.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
