package com.instance.vote.intergration;

import com.instance.vote.domain.Poll;
import com.instance.vote.dto.PollRequest;
import com.instance.vote.dto.PollResponse;
import com.instance.vote.dto.VoteRequest;
import com.instance.vote.exception.BusinessException;
import com.instance.vote.repository.PollRepository;
import com.instance.vote.repository.VoteOptionRepository;
import com.instance.vote.repository.VoteRecordRepository;
import com.instance.vote.service.PollService;
import com.instance.vote.service.VoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest // 전체 Spring Context 로딩
@Testcontainers // 컨테이너 생명주기 관리
@ActiveProfiles("test") // application-test.yaml 사용
public class CastVoteIntegrationTest {

    // 컨테이너 선언
    @Container
    @ServiceConnection // 컨테이너 접속 정보를 Spring에 자동으로 주입. application-local.yaml의 DB/Redis 설정을 덮어씀
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Container
    @ServiceConnection
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    // 주입받을 빈
    @Autowired
    VoteService voteService;

    @Autowired
    PollRepository pollRepository;

    @Autowired
    VoteOptionRepository voteOptionRepository;

    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    private VoteRecordRepository voteRecordRepository;

    private String shareCode;
    private Long optionId;
    private Long pollId;

    @Autowired
    private PollService pollService;

    @BeforeEach
    void setUp() {
        // Testcontainers로 격리된 DB 초기화
        voteRecordRepository.deleteAll();
        voteOptionRepository.deleteAll();
        pollRepository.deleteAll();

        // Testcontainers로 격리된 Redis 초기화
        Objects.requireNonNull(stringRedisTemplate.getConnectionFactory())
                .getConnection()
                .serverCommands()
                .flushAll();

        PollResponse.Create created = pollService.createPoll(new PollRequest.Create("테스트 투표",
                List.of("옵션1", "옵션2", "옵션3"),
                LocalDateTime.now().plusDays(1)));
        shareCode = created.shareCode();

        Poll poll = pollRepository.findWithOptionByShareCode(shareCode).orElseThrow();
        optionId = poll.getOptions().get(0).getId();
        pollId = poll.getId();
    }

    @Test
    void castVote_같은사람이_동시에_두번투표_하나만성공() throws InterruptedException {

        // 멀티스레드 환경 구성
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1); // 동시 출발
        CountDownLatch doneLatch = new CountDownLatch(threadCount); // 완료 대기

        // 예외 수집
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 출발 신호 대기
                    voteService.castVote(shareCode, new VoteRequest.Cast(optionId, "sameToken"));
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 10개 쓰레드 동시 실행
        doneLatch.await(); // 10개의 쓰레드가 모두 끝날 때까지 메인 스레드 대기
        executor.shutdown(); // 테스트 끝난 후 스레드풀 죽이기

        // 검증
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(threadCount - 1);
        assertThat(voteRecordRepository.count()).isEqualTo(1L);

        String redisCount = stringRedisTemplate.opsForValue()
                .get("vote:count:" + pollId + ":" + optionId);
        assertThat(Long.parseLong(redisCount)).isEqualTo(1L);
    }

    @Test
    void castVote_N명이_동시에_투표_N개집계() {

    }
}
