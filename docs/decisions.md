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

## [2026-06-28] castVote 설계 — 시그니처·내부 로직·Redis 키 구조 확정
- 결정: 파라미터는 VoteRequest.Cast(optionId, participantToken) DTO, 반환은 void (204 No Content)
- 배경: 투표 제출 API의 값이 URL·바디·쿠키 세 소스에서 오므로 Controller가 조립해 Service에 전달하는 역할 분리 필요
- 대안: flat 파라미터 3개 / Service가 HttpServletRequest 직접 처리 / 집계 포함 응답
- 채택 이유 / 트레이드오프: Controller 조립 책임 명확화, Service가 HTTP 레이어를 모르므로 단위 테스트 시 문자열만 전달하면 됨. 집계 응답은 단일 책임 위반으로 제외.
- 내부 로직 확정: Poll 유효성(CLOSED·만료) → VoteOption 소속 확인 → Redis SETNX 1차 방어 → DB INSERT 2차 방어 → Redis INCR
- Redis 키: 중복 체크 vote:dup:{pollId}:{participantToken} (TTL=expiresAt까지 남은 초), 집계 vote:count:{pollId}:{optionId} (TTL 없음)
- Redis SETNX 성공 후 DB 롤백 시 키 잔존 문제는 Phase 0에서 인지만 하고 미처리 — TTL 만료로 자연 정리
- ErrorCode 도메인별 prefix 전략 채택: POLL_NNN / OPTION_NNN / VOTE_NNN — 에러 발생 시 어느 도메인 문제인지 즉시 식별 가능
- DTO 분리 확정(2026-06-30 구현): VoteRequest.Body(optionId만, 클라이언트 바디용) / VoteRequest.Cast(optionId + participantToken, Controller가 조립하여 Service에 전달). participantToken을 바디로 받으면 HttpOnly 쿠키의 보안 의미가 훼손되므로(JS 접근 불가여야 하는데 바디에 담으려면 JS가 값을 알아야 함) 쿠키에서만 읽도록 강제.

## [2026-06-28] participantToken 쿠키 발급 시점
- 결정: 투표 페이지 첫 진입(GET /votes/{shareCode}) 시 발급, 쿠키 이미 있으면 재발급 안 함
- 배경: 투표 제출 시점에 @CookieValue(required=true)로 받으므로 사전 발급이 필수
- 대안: 투표 제출 시점에 없으면 그때 발급
- 채택 이유 / 트레이드오프: 쿠키 발급과 투표 제출 로직을 분리. Controller에서 쿠키 존재 여부를 확인해 없을 때만 UUID 발급 후 Set-Cookie 헤더에 담아 응답.

## [2026-06-30] Controller 응답 방식 — ResponseEntity 전체 통일
- 결정: 모든 Controller 메서드의 반환 타입을 ResponseEntity<T>로 통일
- 배경: GET /votes/{shareCode}에서 participantToken 쿠키를 Set-Cookie 헤더에 동적으로 담아야 하므로 헤더 조작이 필수
- 대안: @ResponseStatus 애노테이션 + HttpServletResponse 파라미터 — 상태 코드는 간단하지만 동적 헤더 추가 시 서블릿 API가 Controller 파라미터에 노출됨
- 채택 이유 / 트레이드오프: 상태 코드·헤더·바디를 한 곳에서 조립 가능하고, 쿠키 포함 응답이 필요한 엔드포인트와의 일관성 확보. 반환 타입이 ResponseEntity<PollResponse.Detail>처럼 길어지는 단점은 수용.

## [2026-06-30] 쿠키 발급 방식 — ResponseCookie 채택
- 결정: participantToken 쿠키 발급에 Spring의 ResponseCookie 빌더 사용
- 배경: GET /votes/{shareCode} 첫 진입 시 HttpOnly 쿠키를 안전하게 발급해야 하며, SameSite 속성까지 제어 필요
- 대안: javax.servlet.http.Cookie — HttpOnly 설정은 가능하나 SameSite 속성 설정에 헤더 문자열 직접 조작이 필요하고, HttpServletResponse 파라미터 의존 발생
- 채택 이유 / 트레이드오프: 빌더 API로 HttpOnly·Path·MaxAge·SameSite를 타입 안전하게 설정 가능. 서블릿 API 직접 의존 없이 ResponseEntity 헤더에 자연스럽게 통합됨.

## [2026-06-30] GlobalExceptionHandler 예외 처리 분리 원칙
- 결정: 도메인 예외(BusinessException)는 ErrorCode로, Spring 프레임워크가 던지는 예외는 별도 @ExceptionHandler로 분리
- 배경: @CookieValue(required=true) 누락 시 MissingRequestCookieException, @Valid 실패 시 MethodArgumentNotValidException을 처리할 방법 결정 필요
- 대안: 프레임워크 예외도 ErrorCode에 추가하고 BusinessException으로 감싸 처리
- 채택 이유 / 트레이드오프: 프레임워크 예외를 BusinessException으로 wrapping하면 Spring이 던진 예외를 catch해서 다시 throw하는 불필요한 변환 레이어가 생김. 예외의 발생 주체(우리 도메인 코드 vs Spring)로 책임을 분리해 ErrorCode는 도메인 의미만 담도록 유지. 추가된 핸들러: MissingRequestCookieException → 400, MethodArgumentNotValidException → 400.

## [2026-07-01] 캐시 쓰기 전략 — Write-Through 채택
- 결정: castVote 쓰기 시 DB INSERT(VoteRecord)와 Redis INCR(vote:count 키)을 동시에 수행하는 Write-Through 패턴 채택
- 배경: Redis를 단순 조회 캐시가 아닌 실시간 원자적 카운터로 활용하므로, 쓰기 시점에 두 저장소를 동기화해야 정합성 유지 가능
- 개념 정리 (세 패턴 비교):
  - Write-Through: 쓰기 시 DB와 캐시를 동시 업데이트. 읽기 캐시 히트율 높음, 불일치 가능성 낮음. 우리 선택.
  - Cache-Aside: 쓰기 시 캐시 무효화(삭제), 다음 읽기 때 DB에서 재적재. 구현 단순하나 삭제 직후 읽기가 몰리면 DB 부하 급증(캐시 스탬피드) 위험.
  - Write-Behind: 캐시에만 먼저 쓰고 DB는 비동기 반영. 응답 속도 최고이나 캐시 장애 시 미반영 데이터 유실 위험 — 투표 정합성 요건에 부적합.
- 대안: Cache-Aside — Redis 키 삭제 후 getPoll 때 DB COUNT로 재적재. INCR의 원자성 이점을 포기해야 하고 고동시성 시 DB 부하 급증 위험.
- 채택 이유 / 트레이드오프: Redis INCR이 집계 업데이트와 캐시 갱신을 한 동작으로 처리해 Write-Through가 자연스럽게 맞아떨어짐. 단, Redis 완전 장애 시 복구 후 재동기화 문제가 남음 — 하단 "write-back 보류" 항목 참조.

## [2026-07-01] getPoll Redis 집계 연동 — null/예외 구분 정책 및 DB 폴백 방식
- 결정: Redis null은 0표 정상 케이스, Redis 예외만 DB 폴백 트리거로 구분. DB 폴백은 GROUP BY 단일 쿼리로 처리
- 배경: getPoll의 count가 0L 하드코딩 상태였고, Redis 장애 시 서빙 가능한 폴백 경로가 필요
- 대안: null도 폴백 트리거로 처리(투표 0개 상태에서 매번 DB 쿼리 발생), 옵션별 COUNT 쿼리 N회(옵션 수만큼 DB 왕복)
- 채택 이유 / 트레이드오프: null과 예외를 구분해야 불필요한 DB 폴백을 막을 수 있음. GROUP BY 단일 쿼리로 DB 부하 최소화. DB 폴백은 비상 경로이므로 성능보다 정확성 우선.
- 구현: resolveVoteCounts(try→Redis, catch→DB) / getCountsFromRedis(null→0L) / getCountsFromDb(GROUP BY→Map) 세 메서드로 분리. VoteRecordRepository에 DTO Projection(record OptionCount) 추가.

## [2026-07-01] Redis 복구 후 write-back — Phase 0 보류
- 결정: Redis 장애 복구 후 DB 집계를 Redis에 재동기화하는 write-back을 Phase 0에서 구현하지 않음
- 배경: DB 폴백 중 새 INCR이 들어오면 "DB 읽기 → SET" 사이 레이스 컨디션으로 최신 값을 덮어쓸 위험이 있음
- 대안: 폴백 직후 SET으로 Redis 복구 시도 — 동시성 보장 불가, 방금 예외 난 Redis에 즉시 쓰기도 불안
- 채택 이유 / 트레이드오프: Phase 0 목표는 "Redis 장애 시 DB로 정상 서빙"까지. 복구 후 재동기화는 운영 레벨 문제(AOF 백업 복원 또는 별도 배치 스크립트)로 분리하는 것이 적합. Phase 0에서는 의도적으로 보류.

## [2026-07-01] SSE 이벤트 전파 메커니즘 — Spring Application Event + @TransactionalEventListener 채택
- 결정: castVote 완료 후 ApplicationEventPublisher로 이벤트를 발행하고, @TransactionalEventListener(AFTER_COMMIT)로 SSE 전송
- 배경: castVote가 완료될 때 Host 화면에 실시간으로 집계를 전달해야 하며, VoteService가 SSE 인프라를 직접 알지 않아야 함
- 개념:
  - ApplicationEventPublisher: 같은 프로세스 내에서 이벤트를 발행하는 Spring 기본 기능. 발행자(VoteService)와 수신자(SseEventHandler)가 서로를 모름.
  - @EventListener (기본): 이벤트 발행자와 같은 스레드·같은 트랜잭션에서 실행됨. SSE send()라는 I/O가 트랜잭션 안에서 실행되어 DB 커넥션 점유 시간이 늘어남.
  - @TransactionalEventListener(AFTER_COMMIT): 트랜잭션이 커밋된 이후에 실행됨. DB INSERT가 완료된 상태에서 SSE 이벤트가 나가므로 Host가 즉시 getPoll을 해도 최신 데이터가 보임.
- 대안: SseEmitter 직접 참조 (VoteService → SseEmitterManager 직접 호출, 가장 단순하나 강결합) / Redis Pub/Sub (Phase 2 프로세스 분리 시 필요, Phase 0에서는 오버엔지니어링)
- 채택 이유 / 트레이드오프: VoteService는 이벤트만 발행하고 SSE를 전혀 모름. Phase 2에서 Redis Pub/Sub으로 교체 시 @EventListener 부분만 바꾸면 됨. 단, 이벤트 발행 후 트랜잭션 롤백 시 AFTER_COMMIT이 실행되지 않으므로 롤백된 투표에 대한 SSE는 자동으로 차단됨(의도한 동작).
- 전체 흐름: castVote() → DB INSERT + Redis INCR → publishEvent(VoteCastEvent) → 커밋 → @TransactionalEventListener → SseEmitterManager.broadcast(pollId, 새 집계)

## [2026-07-01] SSE 엔드포인트 설계 — URL 및 HTTP 동작 방식
- 결정: SSE 스트리밍 엔드포인트를 GET /votes/{shareCode}/stream?hostToken={UUID}로 분리, 별도 SseController 생성
- 배경: Host 모니터링 화면에 실시간 집계를 스트리밍해야 하며, SSE는 일반 HTTP와 동작 방식이 달라 Controller 책임 분리가 필요
- 개념 — 일반 HTTP vs SSE:
  - 일반 HTTP: 요청 → 서버 처리 → 응답 전송 → 연결 종료. 서버가 나중에 먼저 데이터를 보낼 수 없음.
  - SSE: 요청 → 서버가 헤더(Content-Type: text/event-stream)만 먼저 보내고 연결을 열어둠 → 이후 서버가 원할 때마다 data: {...} 형식으로 단방향 전송 → 투표 종료 시 연결 닫음.
  - Tomcat 스레드 점유: 일반 HTTP는 처리 완료(수십ms) 후 스레드 반환. SSE는 연결이 유지되는 동안 스레드를 점유. Tomcat 기본 스레드 풀 200개 기준으로 SSE 200개 연결 시 REST API 처리 불가 — 이것이 Phase 0 관찰 실험의 핵심.
- 대안: PollController에 메서드 추가 — 기존 단일 Controller 결정과 일관성 있으나 SSE의 장기 커넥션 특성이 CRUD 메서드들과 혼재됨
- 채택 이유 / 트레이드오프: SSE는 응답을 끝내지 않는다는 점에서 일반 HTTP 메서드와 책임 성격이 다름. 별도 SseController로 분리해 역할을 명확히 함.

## [2026-07-01] SseEmitter 생명주기 관리 — expiresAt 동적 타임아웃 채택
- 결정: SseEmitter를 ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>로 관리, 타임아웃은 expiresAt까지 남은 시간으로 동적 계산
- 배경: castVote 완료 시 연결 중인 Host emitter를 꺼내 send()해야 하므로 중앙 보관소가 필요. 무한정 타임아웃(0L)은 비정상 종료 시 스레드가 영구 점유될 위험이 있음.
- 개념 — 비정상 종료 시 동작:
  - 브라우저 정상 종료(탭 닫기): TCP FIN 전송 → 서버 즉시 감지 → onCompletion 발화 → emitter 제거, 스레드 해제
  - 브라우저 비정상 종료(네트워크 단절): FIN 없음 → 다음 send() 시 IOException → onError 발화 → 즉시 제거 / send()가 없으면 → expiresAt 도달 → onTimeout 발화 → 제거. expiresAt이 최대 보장 시간.
- 추가 처리:
  - 연결 직후 현재 집계 스냅샷 즉시 전송 (첫 투표 전까지 빈 화면 방지)
  - closePoll 호출 시 해당 pollId의 모든 emitter.complete() 호출 → Map에서 제거
  - 타임아웃 후 클라이언트 재연결 시도 시 투표 만료 여부 확인 → 만료면 POLL_EXPIRED 반환
- 대안: 무한정 타임아웃(0L) — send()가 들어올 때만 비정상 종료를 감지하므로 투표 기간이 길면 좀비 연결·스레드 장시간 점유 위험
- 채택 이유 / 트레이드오프: participantToken 쿠키 MaxAge도 expiresAt 기준 동적 계산 — 동일 패턴으로 일관성 확보. expiresAt이 안전망 역할을 하여 비정상 종료 시에도 스레드 점유 시간에 상한이 생김.

## [2026-07-02] SSE 구현 확정 — validateSseConnection 반환 타입 및 구현 세부 결정
- 결정: validateSseConnection()이 Poll 대신 PollResponse.Detail을 반환해 DB 조회 1회로 검증+스냅샷 데이터를 통합 처리
- 배경: SseController에서 검증(validateSseConnection)과 스냅샷 전송(getPoll)을 분리하면 DB 조회가 2회 발생
- 대안: Poll 반환 후 Controller에서 getPoll() 추가 호출 — 구현 단순하나 같은 Poll을 두 번 조회
- 채택 이유 / 트레이드오프: validateSseConnection이 Detail을 반환하면 pollId(register 키)와 options(스냅샷 데이터)를 한 번에 확보. 메서드 책임이 "검증+초기 데이터 제공"으로 넓어지는 단점은 SSE 연결 진입점 전용 메서드임을 명시해 수용.
- 추가 확정 사항:
  - SseEmitterManager.remove() 내 isEmpty 정리 제외 — completeAll()이 Map 정리를 전담, isEmpty 체크와 emitters.remove() 사이 레이스 컨디션 방지
  - 스냅샷 send() IOException 처리: 에러 응답 대신 emitter.completeWithError() 호출 → onError 콜백 발화 → Map에서 정리
  - 만료 투표 status 일괄 업데이트 스케줄러 미도입 — 마감 조건(status=CLOSED OR expiresAt 경과)을 각 진입점(castVote/validateSseConnection)에서 직접 체크하는 방식 유지. 스케줄러 도입 시 불일치 구간 발생 및 복잡도 증가 우려로 보류.

## [2026-07-02] 테스트 전략 확정 — 단위/통합 분리 및 인프라 격리 방식
- 결정: 단위 테스트는 Mockito, 통합 테스트는 Testcontainers(MySQL 8.0 + Redis 7) 채택
- 배경: PollService·VoteService가 JPA Repository + RedisTemplate을 동시에 의존하는 Phase 0 구조에서 각 레이어의 테스트 방식 결정 필요
- 채택 이유 / 트레이드오프:
  - 단위 테스트: Mockito로 모든 의존성 대체. JPA 없이 실행되므로 @OneToMany 관계가 populate되지 않음 → `ReflectionTestUtils.setField(poll, "options", List.of(option))`로 강제 주입 필요. 이 불편함 자체가 Phase 0 레이어드 아키텍처 강결합 체감 실험의 일부.
  - 통합 테스트: Testcontainers로 MySQL/Redis Docker 컨테이너 기동. @ServiceConnection으로 컨테이너 접속 정보 자동 주입. application-local.yaml은 spring-dotenv 환경 변수를 사용하므로 테스트 전용 application-test.yaml 별도 생성 + @ActiveProfiles("test") 적용. ddl-auto: create-drop으로 테스트 완료 시 스키마 자동 삭제.
- 동시성 테스트 패턴: CountDownLatch(startLatch=1 동시 출발, doneLatch=N 완료 대기) + ExecutorService.newFixedThreadPool(N) + AtomicInteger 성공/실패 카운팅