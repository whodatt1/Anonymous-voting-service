import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = 'http://localhost:8080';

// ── 상수 ──────────────────────────────────────────────────────────
// SSE VU 수와 동일하게 맞춘다.
// "투표 1개 = 호스트 1명 = SSE 연결 1개" 이므로
// POLL_COUNT개의 투표를 만들고 VU마다 서로 다른 투표에 SSE 연결한다.
const POLL_COUNT = 150;

// ── 시나리오 설정 ──────────────────────────────────────────────────
export const options = {
    scenarios: {
        // 시나리오 1: 호스트 역할 — 각 투표에 SSE 연결을 유지한다
        // ramping-vus: VU 수를 단계적으로 증가·유지·감소시켜 SSE 연결 수 변화에 따른 Tomcat 스레드 영향을 관찰
        //
        // [대조 실험 포함 이유]
        // 1차 부하 테스트(ramp-down 없음)에서 테스트 종료 시 스레드 수 스파이크가 관찰됨.
        // 원인 가설: SSE 연결 150개가 테스트 종료와 함께 동시에 끊히면서 onCompletion/onError
        //            콜백 150개가 한꺼번에 실행 → 순간적 스레드 점유.
        // 검증 방법: ramp-down 구간을 추가해 연결이 점진적으로 끊히도록 유도.
        //            → ramp-down 중 스파이크가 없으면 "동시 종료 아티팩트" 확정.
        //            → ramp-down 중에도 스파이크가 있으면 실제 문제.
        sse_connections: {
            executor: 'ramping-vus',
            stages: [
                { duration: '1m', target: 50 },         // 0 → 50   [ramp-up 1단계]
                { duration: '1m', target: 100 },        // 50 → 100 [ramp-up 2단계]
                { duration: '1m', target: POLL_COUNT }, // 100 → 150 [ramp-up 3단계]
                { duration: '2m', target: POLL_COUNT }, // 150 유지  [peak: REST 응답 지연 최대치 관찰]
                { duration: '2m', target: 0 },          // 150 → 0  [ramp-down: 대조 실험 핵심 구간]
            ],
            // gracefulRampDown 기본값: 30s
            // → ramp-down 시 k6가 VU 종료를 0.8초(120s÷150개)마다 1개씩 예약하고,
            //   각 VU는 30초 유예 후 강제 종료됨 (SSE는 서버가 먼저 닫지 않으므로 유예 후 끊김).
            // → 결과: 연결이 1~2개씩 순차적으로 끊히는 점진적 패턴 → 동시 종료 아티팩트 검증 가능.
            exec: 'sseScenario',
        },

        // 시나리오 2: 참여자 역할 — 투표 조회 후 투표를 제출한다
        // constant-arrival-rate: "초당 30회"를 보장
        //   → VU 방식이면 응답이 느려질수록 실제 RPS가 줄어들어 부하 측정이 왜곡됨
        //   → rate 고정 방식이어야 "SSE 연결 수 변화가 REST 응답 시간에 영향을 주는가"를 정확히 볼 수 있다
        rest_requests: {
            executor: 'constant-arrival-rate',
            rate: 30,
            timeUnit: '1s',
            duration: '7m', // SSE 전체 구간(ramp-up 3m + peak 2m + ramp-down 2m)을 커버
            preAllocatedVUs: 10,
            maxVUs: 50,
            exec: 'restScenario',
        },
    },
};

// ── setup: 테스트 전 투표 N개를 미리 생성 ─────────────────────────
// k6의 setup()은 모든 VU가 시작하기 전 단 1회 실행된다.
// 반환값은 sseScenario, restScenario의 data 인자로 모든 VU에 전달된다.
export function setup() {
    const polls = [];

    // KST 기준 10분 후 만료시간 — LocalDateTime 포맷 (T 구분, 초까지)
    // 전체 시나리오(ramp-up 3m + peak 2m + ramp-down 2m = 7분)보다 3분 여유를 두어 테스트 도중 만료되지 않도록 설정
    // 10분으로 짧게 잡는 이유: 서버 SseEmitter 타임아웃 = expiresAt - now
    //   → 테스트 종료 후 emitter가 자연 만료되어 2차 테스트 시 누적 방지
    //
    // KST 오프셋(+9h) 보정 이유:
    //   - toISOString()은 UTC 기준 문자열 반환 (예: "2026-07-12T05:30:00")
    //   - 서버는 LocalDateTime.now()를 KST로 읽으므로 UTC 문자열을 그대로 보내면
    //     이미 9시간 전 시각으로 해석되어 @Future 검증 실패(400) 발생
    //   - UTC + 9h + 10min 으로 보내면 서버가 "KST 현재 + 10분"으로 인식
    const expiresAt = new Date(Date.now() + (9 * 60 + 10) * 60 * 1000)
        .toISOString()
        .slice(0, 19);

    // [setup Rate Limiting 우회]
    // POST /votes도 IP당 분당 5회 제한이 걸려 있다.
    // setup()은 VU 컨텍스트 밖에서 단일 스레드로 실행되므로 __VU/__ITER 변수를 사용할 수 없다.
    // 대신 루프 인덱스 i를 활용해 요청마다 고유한 가상 IP(10.0.0.0 ~ 10.0.0.149)를 주입한다.
    for (let i = 0; i < POLL_COUNT; i++) {
        const setupIp = `10.0.0.${i}`;

        // 1. 투표 생성 (POST /votes)
        //    X-Forwarded-For로 가상 IP 주입 → 투표마다 독립 Rate Limiting 버킷 사용
        const createRes = http.post(
            `${BASE_URL}/votes`,
            JSON.stringify({
                title: `load-test-poll-${i + 1}`,
                options: ['option-A', 'option-B'],
                expiresAt: expiresAt,
            }),
            { headers: { 'Content-Type': 'application/json', 'X-Forwarded-For': setupIp } }
        );

        if (createRes.status !== 201) {
            console.error(`[setup] 투표 생성 실패 (${i + 1}번): status=${createRes.status}`);
            continue;
        }

        // 2. body에서 shareCode 추출
        //    hostToken은 @JsonIgnore로 body에서 제외되므로 Set-Cookie 헤더에서 가져와야 한다
        const body = JSON.parse(createRes.body);
        const shareCode = body.shareCode;

        // 3. Set-Cookie 헤더에서 hostToken 추출
        //    형식 예: "hostToken=abc123; Path=/votes/xxx/; HttpOnly; SameSite=Strict"
        const setCookie = createRes.headers['Set-Cookie'];
        const hostToken = setCookie.match(/hostToken=([^;]+)/)[1];

        // 4. GET /votes/{shareCode}/host로 optionId 목록 조회
        //    투표 제출(POST /votes/{shareCode}/vote) 시 optionId가 필요하므로 미리 수집
        const detailRes = http.get(`${BASE_URL}/votes/${shareCode}/host`, {
            headers: { Cookie: `hostToken=${hostToken}`, 'X-Forwarded-For': setupIp },
        });
        const detail = JSON.parse(detailRes.body);

        // PollResponse.OptionDetail 필드: optionId, content, count
        const optionIds = detail.options.map(o => o.optionId);

        polls.push({ shareCode, hostToken, optionIds });
    }

    console.log(`[setup] 투표 ${polls.length}개 생성 완료`);
    return { polls };
}

// ── 시나리오 1: SSE 연결 유지 (호스트 역할) ──────────────────────
// 실제 사용 패턴: 호스트가 투표 생성 후 결과 페이지에서 SSE로 실시간 집계를 본다
//
// __VU: k6가 각 VU에 부여하는 고유 번호 (1부터 시작)
// (__VU - 1) % POLL_COUNT 로 VU마다 서로 다른 투표에 분산 배정한다
// → "투표 150개에 호스트 150명이 각각 SSE 연결 유지" 시뮬레이션
//
// [k6에서 SSE가 동작하는 방식]
// http.get()은 서버 응답이 완전히 끝날 때까지 블로킹한다.
// SSE는 서버(SseEmitter)가 complete()를 호출하기 전까지 응답이 끝나지 않으므로,
// k6 VU는 timeout에 도달할 때까지 http.get()에서 멈춰 SSE 연결을 유지한다.
//
// [1차 실험 결과 — Tomcat 스레드 점유 가설 폐기]
// 예상: SSE 연결이 쌓일수록 Tomcat 스레드 점유 → REST 응답 지연
// 실측: SseEmitter는 서블릿 3.1 AsyncContext(NIO)로 처리되어 스레드를 즉시 반환.
//       SSE 150개 연결 상태에서도 Tomcat 사용 중 스레드 ≈ 0 유지.
//       REST 응답 시간(p90=19.65ms / p95=20.96ms)도 전 구간 안정적.
//
// [이번 실험 목적 — 테스트 종료 시 스파이크 원인 규명]
// 1차 실험에서 테스트 종료 직전 스레드 수가 순간 ~75까지 상승.
// 가설: SSE 연결 150개가 테스트 종료와 동시에 끊히면서 onCompletion 콜백이 몰린 아티팩트.
// 검증: ramp-down 구간에서 연결이 점진적으로 끊힐 때 스파이크가 없으면 아티팩트 확정.
export function sseScenario(data) {
    const poll = data.polls[(__VU - 1) % data.polls.length];

    http.get(`${BASE_URL}/votes/${poll.shareCode}/stream`, {
        headers: { Cookie: `hostToken=${poll.hostToken}` },
        timeout: '600s', // 전체 duration(7분=420s)보다 길게 — 중도 재연결 방지
    });
    // 연결이 끊어지면(서버 timeout or 에러) sseScenario가 재실행되어 즉시 재연결
}

// ── 시나리오 2: 참여자 투표 제출 (참여자 역할) ────────────────────
// 실제 사용 패턴: 참여자가 URL로 접속 → 투표 조회 → 투표 제출
//
// 목적: SSE 연결 수 변화(0개 → 150개 → 0개)에 따라 REST 응답 시간이 영향을 받는지 측정
//       1차 실험 결과: 영향 없음 확인 → 이번 실험에서 ramp-down 구간도 동일한지 재확인
//
// [participantToken 처리]
// GET /votes/{shareCode} 최초 접속 시 Set-Cookie로 participantToken이 발급된다.
// 매 iteration마다 새 토큰을 발급받아 사용 → 각 반복이 새로운 참여자 1명을 시뮬레이션
//
// [Rate Limiting 우회 — X-Forwarded-For IP 스푸핑]
// 부하 테스트 이후 Rate Limiting이 IP 기반으로 전환되었다.
//   - GET /votes/{shareCode}  : IP당 분당 30회
//   - POST /votes/{shareCode}/vote : IP당 분당 5회
// k6는 단일 로컬 IP(127.0.0.1)에서 요청을 보내므로 모든 VU가 버킷을 공유해 즉시 429가 발생한다.
//
// 해결: 각 요청에 X-Forwarded-For 헤더로 가상 IP를 주입한다.
//   - `10.{VU번호}.{이터레이션번호 % 256}.1` 형태로 VU + 이터레이션 조합마다 다른 IP 부여
//   - 로컬 환경(Nginx 없음)에서 앱은 이 헤더의 첫 번째 값을 클라이언트 IP로 읽으므로
//     각 요청이 독립된 Rate Limiting 버킷을 사용하게 된다.
//   - maxVUs=50, __ITER 256단계 → 최대 12,800개의 고유 IP 조합 → 실질적으로 제한 미도달
//
// ※ 프로덕션(Nginx 있음)에서는 Nginx가 $remote_addr로 헤더를 교체하므로 이 우회법은 동작하지 않는다.
//   부하 테스트 전용 로컬 편법이다.
export function restScenario(data) {
    const poll = data.polls[__VU % data.polls.length];

    // VU 번호(__VU)와 이터레이션 번호(__ITER)를 조합해 요청마다 고유한 가상 IP 생성
    // __VU  : 이 VU에 부여된 고유 번호 (1~maxVUs, 시나리오 전체에서 고정)
    // __ITER: 이 VU가 현재까지 완료한 이터레이션 수 (0부터 증가)
    const clientIp = `10.${__VU}.${__ITER % 256}.1`;

    // 1. 투표 조회 — GET /votes/{shareCode}
    //    X-Forwarded-For로 가상 IP 주입 → Rate Limiting(30회/분) 버킷 분리
    const getRes = http.get(`${BASE_URL}/votes/${poll.shareCode}`, {
        headers: { 'X-Forwarded-For': clientIp },
    });
    check(getRes, { '투표 조회 200': (r) => r.status === 200 });

    // 2. Set-Cookie에서 participantToken 추출
    const setCookie = getRes.headers['Set-Cookie'];
    if (!setCookie) return; // 쿠키가 없으면 (예외 상황) 조기 종료
    const participantToken = setCookie.match(/participantToken=([^;]+)/)[1];

    // 3. 투표 제출 — POST /votes/{shareCode}/vote
    //    optionIds 중 무작위로 선택하여 제출
    //    GET과 동일한 가상 IP 사용 → Rate Limiting(5회/분) 버킷도 분리됨
    const optionId = poll.optionIds[Math.floor(Math.random() * poll.optionIds.length)];
    const voteRes = http.post(
        `${BASE_URL}/votes/${poll.shareCode}/vote`,
        JSON.stringify({ optionId }),
        {
            headers: {
                'Content-Type': 'application/json',
                Cookie: `participantToken=${participantToken}`,
                'X-Forwarded-For': clientIp,
            },
        }
    );
    check(voteRes, { '투표 제출 204': (r) => r.status === 204 });
}
