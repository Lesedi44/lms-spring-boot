FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy Maven wrapper and source
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src ./src

# Make wrapper executable
RUN chmod +x mvnw

# Build the application
RUN ./mvnw clean package -DskipTests

# Run the application
EXPOSE 3000
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "target/lms-1.0.0.jar"]