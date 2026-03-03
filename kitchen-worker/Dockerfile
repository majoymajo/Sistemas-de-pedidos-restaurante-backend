FROM maven:3.9-eclipse-temurin-17 AS deps
WORKDIR /app

# Layer 1: Copy only POM files (cached unless POMs change)
COPY pom.xml .
COPY kitchen-worker/pom.xml kitchen-worker/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY report-service/pom.xml report-service/pom.xml

# Layer 2: Download dependencies (cached unless POM changes)
RUN mvn -pl kitchen-worker -am dependency:go-offline -q

# Layer 3: Copy source code (invalidated on code changes)
COPY kitchen-worker/src kitchen-worker/src

# Layer 4: Compile only (fast, dependencies already cached)
RUN mvn -pl kitchen-worker -am package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=deps /app/kitchen-worker/target/kitchen-worker-*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
