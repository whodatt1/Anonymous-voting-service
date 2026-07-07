package com.instance.vote.intergration;

import com.instance.vote.dto.PollRequest;
import com.instance.vote.dto.PollResponse;
import com.instance.vote.repository.PollRepository;
import com.instance.vote.repository.VoteOptionRepository;
import com.instance.vote.repository.VoteRecordRepository;
import com.instance.vote.service.PollService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc // MockMvc 자동 구성
@Testcontainers
@ActiveProfiles("test")
public class RateLimitIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Container
    @ServiceConnection
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    VoteRecordRepository voteRecordRepository;

    @Autowired
    VoteOptionRepository voteOptionRepository;

    @Autowired
    PollRepository pollRepository;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    private String shareCode;
    @Autowired
    private PollService pollService;
    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Testcontainer로 격리된 DB 초기화
        voteRecordRepository.deleteAll();
        voteOptionRepository.deleteAll();
        pollRepository.deleteAll();

        // Testcontainer로 격리된 Redis 초기화
        Objects.requireNonNull(stringRedisTemplate.getConnectionFactory())
                .getConnection()
                .serverCommands()
                .flushAll();

        PollResponse.Create created = pollService.createPoll(new PollRequest.Create("테스트 투표",
                List.of("옵션1", "옵션2", "옵션3"),
                LocalDateTime.now().plusDays(1)));
        shareCode = created.shareCode();
    }

    @Test
    void 쿠키_없으면_RateLimiting_적용_안됨() throws Exception {
        mockMvc.perform(get("/votes/{shareCode}", shareCode))
                .andExpect(status().isOk());
    }

    @Test
    void 같은_토큰_30회_성공_후_31번째_429() throws Exception {
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(get("/votes/{shareCode}", shareCode)
                    .cookie(new Cookie("participantToken", "token-abc")))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/votes/{shareCode}", shareCode)
                .cookie(new Cookie("participantToken", "token-abc")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void 다른_토큰은_독립된_버킷() throws Exception {
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(get("/votes/{shareCode}", shareCode)
                    .cookie(new Cookie("participantToken", "token-a")))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/votes/{shareCode}", shareCode)
                .cookie(new Cookie("participantToken", "token-b")))
                .andExpect(status().isOk());
    }
}
