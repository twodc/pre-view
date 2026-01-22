# Deployment Dockerfile (Uses pre-built JAR from GitHub Actions)
FROM eclipse-temurin:21-jre

WORKDIR /app

# GitHub Actions에서 빌드해서 넘겨준 JAR 파일 복사
# 주의: 로컬에서 빌드하려면 먼저 './gradlew bootJar'를 실행해서 
#       build/libs/pre-view-0.0.1-SNAPSHOT.jar를 pre-view.jar로 복사해야 함.
COPY pre-view.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
