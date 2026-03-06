# -- build stage --
FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace

# Copy parent pom + module poms first for dependency caching
COPY pom.xml ./
COPY shared/pom.xml shared/
COPY pipeline-service/pom.xml pipeline-service/

# Download dependencies (cached unless poms change)
RUN mvn dependency:go-offline -pl shared,pipeline-service -am -B -q || true

# Copy source
COPY shared/src shared/src
COPY pipeline-service/src pipeline-service/src

# Build only shared + pipeline-service
RUN mvn package -pl shared,pipeline-service -am -B -q -DskipTests \
    && mv pipeline-service/target/pipeline-service-*.jar /app.jar

# -- runtime stage --
FROM --platform=linux/amd64 eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

RUN addgroup --system --gid 1001 spring \
    && adduser --system --uid 1001 --ingroup spring spring

COPY --from=build /app.jar app.jar
RUN chown spring:spring app.jar

USER spring

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
