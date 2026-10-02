FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

ARG SERVICE

COPY gradle ./gradle
COPY gradlew ./gradlew
COPY gradlew.bat ./gradlew.bat
COPY settings.gradle ./settings.gradle
COPY build.gradle ./build.gradle

COPY contracts ./contracts
COPY api-gateway ./api-gateway
COPY solicitud-service ./solicitud-service
COPY evaluacion-service ./evaluacion-service
COPY auth-service ./auth-service

RUN chmod +x ./gradlew

RUN echo "Servicio recibido: [$SERVICE]"

RUN test -n "$SERVICE" || \
    (echo "ERROR: no se proporcionó el argumento SERVICE" && exit 1)

RUN ./gradlew ":${SERVICE}:bootJar" \
    --no-daemon \
    --stacktrace

RUN JAR_FILE=$(find "/workspace/${SERVICE}/build/libs" \
    -type f \
    -name "*.jar" \
    ! -name "*-plain.jar" \
    | head -n 1) \
    && test -n "$JAR_FILE" \
    && echo "JAR seleccionado: $JAR_FILE" \
    && cp "$JAR_FILE" /workspace/app.jar

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/app.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
