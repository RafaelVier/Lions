FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app app \
    && mkdir -p /app/data/storage \
    && chown -R app:app /app/data
COPY --chown=app:app build/docker/app.jar app.jar

USER app
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=12 \
    CMD curl --fail --silent --show-error --max-time 4 http://127.0.0.1:8080/actuator/health > /dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
