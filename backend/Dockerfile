FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --chown=app:app build/docker/app.jar app.jar

USER app
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
