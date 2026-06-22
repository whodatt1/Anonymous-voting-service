# 의사결정 로그 (Decisions Log)

> 이 파일은 `project.md`의 [2단계] ADR 도출 시점마다 압축 요약을 append하는 용도입니다.
> 형식: 결정 / 배경 / 대안 / 채택 이유·트레이드오프 / (필요시) 상세 ADR 링크

<!-- 예시 (실제 결정 시 아래 형식으로 추가하고 이 예시는 삭제하세요)

## [2026-06-17] SSE vs WebSocket 선택
- 결정: 호스트 모니터링 화면에는 SSE를 채택
- 배경: 참여자는 1회성 제출만 하므로 양방향 통신이 필요한 호스트 화면만 실시간성이 필요
- 대안: WebSocket (양방향, 더 무거움), Long Polling (구현 단순하지만 지연 큼)
- 채택 이유 / 트레이드오프: 단방향 브로드캐스트로 충분하고 Tomcat 스레드 부담이 적어 SSE 채택. 양방향 기능 확장 시 WebSocket 재검토 필요
- 상세: docs/adr/001-sse-vs-websocket.md

-->

## [2026-06-17] Java 버전 확정
- 결정: Java 21 (LTS) 채택
- 배경: build.gradle이 이미 21로 세팅되어 있었고, project.md의 Java 17 명시는 오기였음
- 대안: Java 17 (LTS, Spring Boot 3.x 완전 호환)
- 채택 이유 / 트레이드오프: Virtual Thread 기본 지원으로 Phase 2에서 비교 카드로 활용 가능. Virtual Thread 개념은 구현 시점에 학습 예정.

## [2026-06-17] QueryDSL 의존성 설정 방식
- 결정: querydsl-jpa jakarta classifier 방식 채택 (Spring Boot 3.x 표준)
- 배경: Spring Boot 3.x는 Jakarta EE 기반(jakarta.* 네임스페이스)으로, 기존 javax.* 기반 APT와 네임스페이스 충돌 발생 빈번
- 대안: 전통 방식 (querydsl-apt + JPAAnnotationProcessor 직접 설정) — QClass 생성 가능하나 jakarta 충돌 위험
- 채택 이유 / 트레이드오프: jakarta 네임스페이스에 정확히 대응하여 QClass 자동 생성 안정적. 단, QueryDSL 버전이 Spring Boot BOM 미포함이므로 버전을 직접 명시해야 함.

## [2026-06-17] application.yaml 프로파일 전략
- 결정: 공통(application.yaml) + 환경별 파일 분리(application-local.yaml, application-prod.yaml)
- 배경: Public 레포지토리 운영 예정으로 prod 민감정보(DB 접속 정보 등)를 소스코드에서 분리 필요
- 대안: 단일 파일 내 --- 구분자 방식 (환경변수 외부화 자체는 두 방식 모두 가능)
- 채택 이유 / 트레이드오프: 환경별 설정 가시성 확보, CI/CD 시 프로파일 이름만 외부 주입하면 자동 매핑. spring.profiles.active는 jar에 포함되지 않도록 IntelliJ Run Config 또는 JVM 아규먼트로만 주입.

## [2026-06-17] Phase 0 패키지 구조
- 결정: 레이어 우선 패키지 구조 채택 (controller / service / repository / domain / dto / config / exception)
- 배경: Phase 0 목표는 레이어드 아키텍처의 불편함(강결합, 테스트 어려움)을 직접 체감하여 Phase 1 전환의 근거를 마련하는 것
- 대안: 도메인 우선 구조 (향후 헥사고날 전환에 유리하나 Phase 0 체감 목적에 부합하지 않음)
- 채택 이유 / 트레이드오프: Service가 JPA Repository / RedisTemplate을 직접 의존하는 가장 단순한 형태로 시작. 이 구조의 한계를 실측한 뒤 Phase 1에서 헥사고날로 전환하는 포트폴리오 서사 확보.

## [2026-06-18] 브랜치 전략 선택
- 결정: GitHub Flow 채택 (main ← feature/*)
- 배경: 1인 포트폴리오 프로젝트로 develop 브랜치의 실익이 없고, PR 기반 CI 설계와 정합성이 필요
- 대안: Git Flow (main → develop → feature/*) — 실무 유사하나 혼자 하는 프로젝트에서 develop이 형식적 절차에 불과
- 채택 이유 / 트레이드오프: feature/* → main PR 흐름이 GitHub Actions CI(PR 트리거)와 자연스럽게 맞아떨어짐. PR 히스토리가 기능 단위로 쌓여 포트폴리오 서사로도 활용 가능. 단, main 직접 push 시 CI가 우회될 수 있으므로 브랜치 보호 규칙 설정 권장.

## [2026-06-20] domain 패키지 범위 전략
- 결정: Phase 0은 A안(Entity Only)으로 시작, 테스트 체감 후 B안(Rich Domain Model)으로 전환
- 배경: 테스트 코드 작성 경험이 없는 상태에서 "Service 강결합이 불편하다"는 감각을 추상적 설명만으로 확보하기 어려움
- 대안: B안 선적용 (Entity에 도메인 메서드 포함) — Phase 1 전환이 쉬워지나 불편함 체감 기회를 잃음
- 채택 이유 / 트레이드오프: "투표 참여 + 중복 투표 방지" 기능을 A안으로 구현 후 단위 테스트까지 직접 작성해, Service가 JPA Repository·RedisTemplate을 동시에 의존할 때의 테스트 복잡도를 실측한다. 불편함이 확인되면 B안으로 전환 — 이 과정 자체가 Phase 1 헥사고날 비교 실험의 근거 서사가 된다.

## [2026-06-20] DTO 패키지 구조
- 결정: 도메인별 outer class + inner record 방식 채택
- 배경: Flat 파일 방식(VoteCreateRequest.java 등)은 파일 수가 늘어나고, request/response 폴더 분리는 이 프로젝트 규모에서 과한 구조
- 대안: Flat 파일 (기능마다 파일 1개) / request·response 하위 패키지 분리
- 채택 이유 / 트레이드오프: `VoteRequest.Create`, `VoteResponse.Detail` 형태로 도메인 관련 DTO를 파일 2개로 집약. Java 21 record 활용으로 간결하고, outer class는 `private 생성자`로 인스턴스화를 방지한다. DTO가 20개 이상으로 늘어나거나 도메인이 다수가 되면 폴더 분리 재검토.

## [2026-06-20] exception 패키지 설계
- 결정: ErrorCode enum + 단일 BusinessException + GlobalExceptionHandler 구조 채택
- 배경: 기능별 예외 클래스를 개별 생성하면 에러 추가 시 파일이 늘고, HTTP 상태 코드와 에러 메시지 관리가 핸들러에 분산됨
- 대안: 기능별 개별 예외 클래스 (VoteNotFoundException 등) / 추상 기반 클래스 + 하위 예외
- 채택 이유 / 트레이드오프: 모든 비즈니스 에러를 `ErrorCode` enum 한 곳에서 정의(HTTP 상태·코드·메시지 일괄 관리), `throw new BusinessException(ErrorCode.VOTE_NOT_FOUND)` 한 줄로 통일. 특정 예외 타입만 골라 catch해야 하는 상황에서는 ErrorCode 분기가 필요하나, 이 프로젝트에서 해당 케이스는 드물다고 판단.

## [2026-06-21] 비회원 식별 전략
- 결정: participantToken은 HttpOnly 쿠키, hostToken은 관리 URL 쿼리 파라미터로 관리
- 배경: 로그인 없는 서비스에서 중복 투표 방지(참여자)와 관리 화면 접근 제어(생성자)를 위한 식별 수단이 필요
- 대안: IP 기반 식별(NAT 오탐 위험), 로컬스토리지(XSS 취약 + SSE 커스텀 헤더 미지원)
- 채택 이유 / 트레이드오프: HttpOnly 쿠키는 XSS 안전하고 SSE 연결 시 자동 포함됨. hostToken은 URL에 포함해 쿠키 만료 문제를 회피(`/votes/{shareCode}/manage?hostToken={UUID}`). hostToken이 URL에 노출되나 탈취 시 리스크(투표 종료 권한)가 낮아 수용 가능한 수준으로 판단.

## [2026-06-21] 공유 URL 방식
- 결정: DB PK 대신 랜덤 shareCode 컬럼을 별도로 두어 `/votes/{shareCode}` 형태로 공유
- 배경: DB PK 직접 노출 시 투표 수가 외부에 드러나고 순차 접근 시도가 가능해짐
- 대안: DB PK 직접 노출(`/votes/1`) — 구현 단순하지만 보안·신뢰도 낮음
- 채택 이유 / 트레이드오프: 추측이 어렵고 투표 수가 노출되지 않아 서비스 신뢰도 향상. Poll 테이블에 shareCode 컬럼 추가 및 UNIQUE 인덱스가 필요하나 비용 미미.

## [2026-06-21] 집계 구조 및 결과 공개 정책
- 결정: Redis INCR(실시간 집계) + VoteRecord DB INSERT(영속), 결과는 항상 공개, 조회 시 Redis 우선 / 장애 시 DB COUNT(*) 폴백
- 배경: 고동시성 환경에서 집계 정합성과 Redis 장애 시 복구 가능성을 동시에 확보해야 하며, 결과 공개 범위도 명확화 필요
- 대안: Redis만 사용(장애 시 집계 유실), VoteOption.count 컬럼 방식(동시 UPDATE 경합 발생 — Redis 도입 이유를 DB에서 재현)
- 채택 이유 / 트레이드오프: Redis는 빠른 실시간 집계, DB VoteRecord는 영속 원본으로 역할 분리. 결과 항상 공개로 UX 향상, Rate Limiting으로 무한 새로고침 방지. 매 투표마다 DB INSERT가 발생하나 INSERT는 행 경합이 없어 허용 가능. SSE는 Host 전용으로 유지해 커넥션 고갈 방지.

## [2026-06-21] 엔티티 설계 확정 (Poll / VoteOption / VoteRecord)
- 결정: 3개 엔티티 필드 확정. Poll에 status(OPEN/CLOSED) 추가, 투표는 단일 선택만 지원
- 배경: 비회원 식별·집계 구조 결정을 바탕으로 실제 DB 스키마 설계 필요
- 대안: expiresAt만으로 마감 판단(수동 종료 불가), 복수 선택(UNIQUE 제약 복잡 + Poll에 maxSelectCount 필요)
- 채택 이유 / 트레이드오프: status로 수동 종료 지원하되 마감 조건이 두 가지(expiresAt 도래 OR status=CLOSED)가 됨. 단일 선택으로 UNIQUE(poll_id, participantToken) 단순 유지.

## [2026-06-21] 패키지 구조 확정
- 결정: 단일 PollController, Service는 PollService + VoteService 두 개로 분리
- 배경: 레이어 우선 구조(decisions.md 2026-06-20)가 확정된 상태에서 Controller·Service 분리 단위 결정 필요
- 대안: Controller 2개 분리(과한 분리), Service 1개 통합(Redis 의존성이 DB 로직과 섞임)
- 채택 이유 / 트레이드오프: Controller는 URL이 모두 /votes 하위라 통합이 자연스러움. VoteService가 Redis 의존성을 집중해서 갖도록 분리해 헥사고날 비교 실험 시 테스트 복잡도 체감 대상을 명확히 함.

## [2026-06-21] 엔티티 인스턴스 생성 방식 — 정적 팩토리 메서드 채택
- 결정: 엔티티 생성에 @Builder 대신 정적 팩토리 메서드(Poll.create(...)) 사용
- 배경: JPA의 protected no-arg 생성자 요구와 엔티티 불변 규칙(status는 생성 시 항상 OPEN) 을 동시에 만족시킬 생성 수단이 필요
- 대안: @Builder — @AllArgsConstructor(PRIVATE) 추가로 JPA 호환은 가능하나 status·id·createdAt 등 외부에서 건드리면 안 되는 필드까지 Builder에 노출됨
- 채택 이유 / 트레이드오프: 팩토리 메서드 내부에서 status=OPEN을 강제해 호출부의 실수를 원천 차단. 필드 수가 4개 내외라 Builder의 가독성 이점도 크지 않음. 단, 파라미터가 많아지거나 선택적 조합이 생기면 Builder 재검토 가능.

## [2026-06-22] getPoll 조회 전략 — @OneToMany + @EntityGraph 채택
- 결정: Poll에 @OneToMany(VoteOption) 관계 추가, getPoll은 JOIN FETCH(@EntityGraph)로 단일 쿼리 조회
- 배경: Poll/VoteOption 분리 조회(2쿼리) vs JOIN FETCH(1쿼리) 비교. Poll→VoteOption은 명확한 부모-자식 관계이므로 엔티티에 표현하는 것이 적절
- 대안: VoteOptionRepository.findByPollOrderByDisplayOrder로 분리 조회 — 구현 단순하나 DB 왕복 2회
- 채택 이유 / 트레이드오프: options 필요 여부가 용도마다 다르므로 메서드를 분리(findWithOptionsByShareCode / findByShareCode)해야 함. @EntityGraph는 JPQL fetch join과 동일한 SQL을 생성하나 복잡한 조건 조합 시 JPQL이 더 적합