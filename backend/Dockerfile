# syntax=docker/dockerfile:1
# oneBite 백엔드(Spring Boot/Kotlin) — 멀티스테이지: gradle 빌드 → JRE 런타임

# ── build: 프로젝트 Gradle 래퍼로 bootJar 생성(테스트는 CI에서 별도 실행하므로 -x test) ──
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew \
 && ./gradlew bootJar --no-daemon -x test \
 && cp "$(ls build/libs/*.jar | grep -v -- '-plain.jar' | head -n1)" /app/app.jar

# ── runtime: 슬림 JRE + 비루트 실행 ──
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd -r -u 1001 appuser
COPY --from=build /app/app.jar app.jar
USER appuser
EXPOSE 8080
# 컨테이너 메모리 한도에 맞춰 힙 자동 산정
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
