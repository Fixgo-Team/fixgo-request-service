# Etapa 1: compilar el JAR con Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Etapa 2: imagen liviana solo con el JRE (RNF-23)
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 fixgo
COPY --from=build /app/target/fixgo-request-service.jar app.jar
USER fixgo
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
