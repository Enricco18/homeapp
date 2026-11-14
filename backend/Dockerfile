FROM eclipse-temurin:22-jdk AS builder
WORKDIR /app
COPY gradle ./gradle
COPY gradlew build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon -q
COPY src ./src
RUN ./gradlew bootJar --no-daemon -q

FROM eclipse-temurin:22-jre AS runner
WORKDIR /app
RUN addgroup --system --gid 1001 spring && adduser --system --uid 1001 --ingroup spring spring
COPY --from=builder /app/build/libs/*.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
