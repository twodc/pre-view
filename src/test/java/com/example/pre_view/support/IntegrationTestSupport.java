package com.example.pre_view.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 통합 테스트를 위한 기본 설정 클래스 (Spring Boot 4.0 + Java 21)
 *
 * <h2>주요 특징</h2>
 * <ul>
 *   <li>MySQL Testcontainers: 프로덕션 환경과 동일한 DB 사용 (H2 제거)</li>
 *   <li>Redis Testcontainers: 실제 Redis 인스턴스 사용</li>
 *   <li>@ServiceConnection: 자동 연결 설정 (DynamicPropertySource 불필요)</li>
 *   <li>Virtual Thread 지원: Java 21 기본 지원</li>
 * </ul>
 *
 * <h2>사용법</h2>
 * <pre>{@code
 * class MemberServiceIntegrationTest extends IntegrationTestSupport {
 *     @Autowired
 *     private MemberService memberService;
 *
 *     @Test
 *     void 회원_생성_테스트() {
 *         // Given-When-Then
 *     }
 * }
 * }</pre>
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/testing/testcontainers.html">Spring Boot Testcontainers</a>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@Testcontainers
public abstract class IntegrationTestSupport {

    /**
     * MySQL 컨테이너 (프로덕션 환경 미러링)
     *
     * @ServiceConnection이 자동으로:
     * - spring.datasource.url
     * - spring.datasource.username
     * - spring.datasource.password
     * 를 설정합니다.
     */
    @ServiceConnection
    protected static final MySQLContainer<?> MYSQL_CONTAINER;

    /**
     * Redis 컨테이너
     *
     * @ServiceConnection(name = "redis")가 자동으로:
     * - spring.data.redis.host
     * - spring.data.redis.port
     * 를 설정합니다.
     */
    @ServiceConnection(name = "redis")
    protected static final GenericContainer<?> REDIS_CONTAINER;

    static {
        // MySQL 8.0 컨테이너 설정
        MYSQL_CONTAINER = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("preview_test")
                .withUsername("test")
                .withPassword("test")
                .withCommand(
                        "--character-set-server=utf8mb4",
                        "--collation-server=utf8mb4_unicode_ci"
                );

        // Redis 7 Alpine 컨테이너 설정
        REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);

        // 컨테이너 병렬 시작
        MYSQL_CONTAINER.start();
        REDIS_CONTAINER.start();
    }

    /**
     * Virtual Thread에서 실행 중인지 검증하는 유틸리티 메서드
     */
    protected void assertRunningOnVirtualThread() {
        if (!Thread.currentThread().isVirtual()) {
            throw new AssertionError(
                    "Expected to run on Virtual Thread, but running on Platform Thread: "
                            + Thread.currentThread().getName());
        }
    }

    /**
     * 현재 스레드 정보를 로깅용으로 반환
     */
    protected String getCurrentThreadInfo() {
        var current = Thread.currentThread();
        return "Thread[name=%s, virtual=%s, id=%d]".formatted(
                current.getName(),
                current.isVirtual(),
                current.threadId());
    }
}
