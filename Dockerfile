FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S app -G app
COPY target/distributed-order-fulfillment-platform-1.0.0.jar /app.jar
RUN chown app:app /app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-XX:+ExitOnOutOfMemoryError","-Djava.security.egd=file:/dev/urandom","-jar","/app.jar"]
