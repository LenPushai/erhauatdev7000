# Stage 1: Build the application using Maven
FROM maven:3.9.6-amazoncorretto-17 AS build
WORKDIR /app
COPY . .
# Use Maven wrapper for consistency; ensure it's executable
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

# Stage 2: Run the application
FROM amazoncorretto:17-alpine-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Expose the port (Render will override with $PORT)
EXPOSE ${PORT:-8080}
# Run the JAR with dynamic port from Render's $PORT env var
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]