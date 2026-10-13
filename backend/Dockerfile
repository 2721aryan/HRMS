# ─── Stage 1: Build the JAR ────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first (layer cache: only re-download deps if pom changes)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Make mvnw executable and pre-download dependencies
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN ./mvnw package -DskipTests -q

# ─── Stage 2: Run the JAR ───────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Create non-root user for security
RUN addgroup -S hrms && adduser -S hrms -G hrms

# Copy only the fat JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Give ownership to the non-root user
RUN chown hrms:hrms app.jar

USER hrms

EXPOSE 8085

# JVM tuning: container-aware memory limits, faster startup, smaller footprint
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=70.0", \
  "-XX:+UseG1GC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
