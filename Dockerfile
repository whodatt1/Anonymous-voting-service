# ─── Stage 1: 빌드 ───────────────────────────────────────────────────────────
# FROM: 이 스테이지의 베이스 이미지 지정
# eclipse-temurin:21-jdk-alpine = Alpine Linux(초경량 OS) + JDK 21이 설치된 이미지
# AS builder = 이 스테이지에 "builder"라는 이름을 붙임 (Stage 2에서 참조할 때 사용)
FROM eclipse-temurin:21-jdk-alpine AS builder

# WORKDIR: 컨테이너 안에서 작업할 디렉토리 지정 (없으면 자동 생성)
# 이후 모든 COPY, RUN 명령은 이 경로 기준으로 실행됨
WORKDIR /app

# ── 캐시 레이어 최적화 ─────────────────────────────────────────────────────────
# Docker는 각 줄을 레이어로 캐싱함
# 소스코드(src/)가 바뀌어도 build.gradle이 안 바뀌면 아래 의존성 다운로드 레이어를 재사용
# → 매번 라이브러리를 새로 받지 않아도 돼서 빌드 속도가 빨라짐
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# gradlew(Gradle 실행 스크립트)에 실행 권한 부여
# 리눅스에서는 파일에 실행 권한이 없으면 ./gradlew 명령어 자체가 안 됨
RUN chmod +x gradlew

# 소스코드 없이 의존성만 먼저 다운로드
# --no-daemon: Gradle 백그라운드 프로세스(Daemon)를 띄우지 않음 (컨테이너에서는 불필요)
RUN ./gradlew dependencies --no-daemon

# 소스코드 복사 후 실행 가능한 JAR 파일 생성
# bootJar: Spring Boot 전용 태스크, 모든 의존성을 하나의 JAR에 묶어줌 (fat JAR)
COPY src src
RUN ./gradlew bootJar --no-daemon

# ─── Stage 2: 실행 ───────────────────────────────────────────────────────────
# JRE만 있는 경량 이미지 사용 (JDK보다 훨씬 작음)
# 실행에는 컴파일러(JDK)가 필요 없고 JVM(JRE)만 있으면 됨
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Stage 1(builder)에서 생성된 JAR 파일만 꺼내서 복사
# --from=builder: Stage 1의 파일시스템에서 가져옴
# build/libs/*.jar → app.jar로 이름 변경
COPY --from=builder /app/build/libs/*.jar app.jar

# 컨테이너 시작 시 실행할 명령어
# -Xmx256m: JVM 최대 힙 메모리 256MB로 제한 (t2.micro 1GB RAM 환경 고려)
# -Xms128m: JVM 초기 힙 메모리 128MB (처음부터 128MB 확보, 필요 시 256MB까지 확장)
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-jar", "app.jar"]
