package com.instance.vote.event;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseEmitterManager {

    // SseEmitter -> Host 브라우저 탭 하나와의 지속적인 연결 통로
    // ConcurrentHashMap -> 여러 스레드(Tomcat 스레드)가 동시에 broadcast()를 호출할 수 있으므로 thread-safe Map 필요
    // CopyOnWriterArrayList -> 한 pollId에 Host가 여러 탭으로 연결 가능하고(하나의 창 제약이 없음), broadcast()중에 onError로 인해 제거가 동시에 일어날 수 있어
    //                         순회 중 제거가 안전한 자료구조 (쓰기가 발생할 때 리스트 전체를 복사한 새 배열에 변경 적용)
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitterManager(MeterRegistry meterRegistry) {
        // sse.connections.active -> 메트릭 이름 Prometheus에서 점이 언더스코어로 변환되어 조회
        // emmiters -> 참조할 객체 스크래핑 시점에 함수 호출
        Gauge.builder("sse.connections.active", emitters,
                // 현재 커넥션 수를 계산. Map의 모든 List 크기 합산
                map -> map.values().stream().mapToInt(List::size).sum())
                .description("현재 활성 SSE 커넥션 수")
                .register(meterRegistry);
    }

    private void remove(Long pollId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitterList = emitters.get(pollId);
        if (emitterList != null) {
            emitterList.remove(emitter);
        }
    }

    public SseEmitter register(Long pollId, long timeoutMillis) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);

        emitter.onCompletion(() -> remove(pollId, emitter));
        emitter.onTimeout(() -> remove(pollId, emitter));
        emitter.onError((e) -> remove(pollId, emitter));

        emitters.computeIfAbsent(pollId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        return emitter;
    }

    // 현재는 List<PollResponse.OptionDetail> 이지만 다른 타입 보낼 가능성 열어두기 위해 Object로 선언
    public void broadcast(Long pollId, Object data) {
        CopyOnWriteArrayList<SseEmitter> emitterList = emitters.get(pollId);

        if (emitterList == null || emitterList.isEmpty()) {
            return;
        }

        List<SseEmitter> dead = new ArrayList<>();

        for (SseEmitter emitter : emitterList) {
            try {
                emitter.send(SseEmitter.event().data(data));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }

        // 후속 처리
        dead.forEach(emitter -> remove(pollId, emitter));
    }

    public void completeAll(Long pollId) {
        List<SseEmitter> list = emitters.remove(pollId);

        if (list == null || list.isEmpty()) {
            return;
        }

        list.forEach(SseEmitter::complete);
    }

}
