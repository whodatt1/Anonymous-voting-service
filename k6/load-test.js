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
        // ramping-vus: VU 수를 단계적으로 증가시켜 SSE 연결이 서서히 쌓이는 과정을 관찰
        // 3단계로 나눠 50개씩 증가 → Tomcat 스레드 점유가 단계적으로 쌓이는 양상을 Grafana에서 관찰
        sse_connections: {
            executor: 'ramping-vus',
            stages: [
                { duration: '1m', target: 50 },         // 0 → 50 (Tomcat 스레드 여유 구간)
                { duration: '1m', target: 100 },        // 50 → 100 (스레드 점유 중간 구간)
                { duration: '1m', target: POLL_COUNT }, // 100 → 150 (스레드 포화 임박 구간)
                { duration: '2m', target: POLL_COUNT }, // 150 유지 — REST 응답 지연 최대치 관찰
            ],
            exec: 'sseScenario',
        },

        // 시나리오 2: 참여자 역할 — 투표 조회 후 투표를 제출한다
        // constant-arrival-rate: "초당 30회"를 보장
        //   → VU 방식이면 응답이 느려질수록 실제 RPS가 줄어들어 부하 측정이 왜곡됨
        //   → rate 고정 방식이어야 "SSE 점유가 늘어날수록 응답 시간이 어떻게 변하는가"를 정확히 볼 수 있다
        rest_requests: {
            executor: 'constant-arrival-rate',
            rate: 30,
            timeUnit: '1s',
            duration: '5m', // SSE 전체 구간(5분)을 커버 — SSE 0개 ~ 150개 구간 전체 응답 시간 기록
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

    // KST 기준 7분 후 만료시간 — LocalDateTime 포맷 (T 구분, 초까지)
    // 전체 시나리오(5분)보다 2분 여유를 두어 테스트 도중 만료되지 않도록 설정
    // 7분으로 짧게 잡는 이유: 서버 SseEmitter 타임아웃 = expiresAt - now
    //   → 테스트 종료 후 emitter가 자연 만료되어 2차 테스트 시 누적 방지
    //
    // KST 오프셋(+9h) 보정 이유:
    //   - toISOString()은 UTC 기준 문자열 반환 (예: "2026-07-12T05:30:00")
    //   - 서버는 LocalDateTime.now()를 KST로 읽으므로 UTC 문자열을 그대로 보내면
    //     이미 9시간 전 시각으로 해석되어 @Future 검증 실패(400) 발생
    //   - UTC + 9h + 7min 으로 보내면 서버가 "KST 현재 + 7분"으로 인식
    const expiresAt = new Date(Date.now() + (9 * 60 + 7) * 60 * 1000)
        .toISOString()
        .slice(0, 19);

    for (let i = 0; i < POLL_COUNT; i++) {
        // 1. 투표 생성 (POST /votes)
        const createRes = http.post(
            `${BASE_URL}/votes`,
            JSON.stringify({
                title: `load-test-poll-${i + 1}`,
                options: ['option-A', 'option-B'],
                expiresAt: expiresAt,
            }),
            { headers: { 'Content-Type': 'application/json' } }
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
            headers: { Cookie: `hostToken=${hostToken}` },
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
// → "투표 50개에 호스트 50명이 각각 SSE 연결 유지" 시뮬레이션
//
// [Tomcat 스레드 점유 원리]
// http.get()은 응답이 완전히 끝날 때까지 블로킹한다.
// SSE는 서버(SseEmitter)가 complete()를 호출하기 전까지 응답이 끝나지 않으므로
// timeout 시간 동안 Tomcat 스레드 1개를 점유한 채로 대기한다.
export function sseScenario(data) {
    const poll = data.polls[(__VU - 1) % data.polls.length];

    http.get(`${BASE_URL}/votes/${poll.shareCode}/stream`, {
        headers: { Cookie: `hostToken=${poll.hostToken}` },
        timeout: '400s', // 전체 duration(5분=300s)보다 길게 — 중도 재연결 방지
    });
    // 연결이 끊어지면(서버 timeout or 에러) sseScenario가 재실행되어 즉시 재연결
}

// ── 시나리오 2: 참여자 투표 제출 (참여자 역할) ────────────────────
// 실제 사용 패턴: 참여자가 URL로 접속 → 투표 조회 → 투표 제출
//
// 목적: SSE로 스레드가 점유되어 가는 동안 일반 요청의 응답 시간이 어떻게 변하는지 측정
//       SSE 0개(초반) vs SSE 50개(후반)의 응답 시간 차이가 핵심 지표
//
// [participantToken 처리]
// GET /votes/{shareCode} 최초 접속 시 Set-Cookie로 participantToken이 발급된다.
// 매 iteration마다 새 토큰을 발급받아 사용 → 각 반복이 새로운 참여자 1명을 시뮬레이션
export function restScenario(data) {
    const poll = data.polls[__VU % data.polls.length];

    // 1. 투표 조회 — GET /votes/{shareCode}
    //    주의: @RateLimit(limit=30, windowSeconds=60) 적용 엔드포인트
    //    k6가 단일 IP에서 요청을 보내므로 Rate Limit에 걸릴 수 있다
    const getRes = http.get(`${BASE_URL}/votes/${poll.shareCode}`);
    check(getRes, { '투표 조회 200': (r) => r.status === 200 });

    // 2. Set-Cookie에서 participantToken 추출
    const setCookie = getRes.headers['Set-Cookie'];
    if (!setCookie) return; // 쿠키가 없으면 (예외 상황) 조기 종료
    const participantToken = setCookie.match(/participantToken=([^;]+)/)[1];

    // 3. 투표 제출 — POST /votes/{shareCode}/vote
    //    optionIds 중 무작위로 선택하여 제출
    const optionId = poll.optionIds[Math.floor(Math.random() * poll.optionIds.length)];
    const voteRes = http.post(
        `${BASE_URL}/votes/${poll.shareCode}/vote`,
        JSON.stringify({ optionId }),
        {
            headers: {
                'Content-Type': 'application/json',
                Cookie: `participantToken=${participantToken}`,
            },
        }
    );
    check(voteRes, { '투표 제출 204': (r) => r.status === 204 });
}
