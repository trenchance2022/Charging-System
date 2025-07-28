FROM maven:3.9.9-amazoncorretto-17-alpine AS build
WORKDIR /app

VOLUME /root/.m2
COPY maven-settings.xml /root/.m2/settings.xml
COPY pom.xml .

RUN mvn -s /root/.m2/settings.xml dependency:go-offline

COPY src ./src
RUN mvn -s /root/.m2/settings.xml clean package -DskipTests

FROM amazoncorretto:17-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]