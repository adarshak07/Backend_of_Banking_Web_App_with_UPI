# ---- Build stage ----
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only pom.xml first to cache dependencies
COPY pom.xml .
RUN mvn -B -e -DskipTests dependency:go-offline

# Now copy the source code
COPY src ./src

# Build the application JAR file
RUN mvn -q -DskipTests clean package

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy only the built JAR file from the 'build' stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java","-jar","app.jar"]
