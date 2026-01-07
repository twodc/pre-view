package com.example.pre_view.support;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.example.pre_view.config.JpaConfig;

/**
 * Repository 슬라이스 테스트를 위한 공통 지원 클래스
 *
 * <h2>설계 의도</h2>
 * <ul>
 *   <li>MySQL Testcontainers: 모든 Repository 테스트에서 컨테이너 공유</li>
 *   <li>@ServiceConnection: 자동 DataSource 설정</li>
 *   <li>@Import(JpaConfig.class): JPA Auditing 활성화</li>
 *   <li>static 블록: 컨테이너를 한 번만 시작하여 테스트 속도 향상</li>
 * </ul>
 *
 * <h2>사용법</h2>
 * <pre>{@code
 * @DisplayName("MemberRepository 슬라이스 테스트")
 * class MemberRepositoryTest extends RepositoryTestSupport {
 *
 *     @Autowired
 *     private MemberRepository memberRepository;
 *
 *     @Test
 *     void 회원_저장_테스트() {
 *         // Given-When-Then
 *     }
 * }
 * }</pre>
 *
 * <h2>주의사항</h2>
 * <ul>
 *   <li>하위 클래스에서 @DataJpaTest 어노테이션을 다시 선언하지 마세요</li>
 *   <li>TestEntityManager는 이 클래스에서 주입받아 사용하세요</li>
 * </ul>
 *
 * @see IntegrationTestSupport 통합 테스트용 (Redis 포함)
 */
@DataJpaTest
@Testcontainers
@ActiveProfiles("integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
public abstract class RepositoryTestSupport {

    /**
     * MySQL 컨테이너 (모든 Repository 테스트에서 공유)
     *
     * <p>static final + static 블록으로 JVM 당 한 번만 시작됩니다.
     * 이를 통해 각 테스트 클래스마다 컨테이너를 재시작하는 오버헤드를 제거합니다.</p>
     *
     * @ServiceConnection이 자동으로:
     * - spring.datasource.url
     * - spring.datasource.username
     * - spring.datasource.password
     * 를 설정합니다.
     */
    @ServiceConnection
    protected static final MySQLContainer<?> MYSQL_CONTAINER;

    static {
        MYSQL_CONTAINER = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("preview_test")
                .withUsername("test")
                .withPassword("test")
                .withCommand(
                        "--character-set-server=utf8mb4",
                        "--collation-server=utf8mb4_unicode_ci"
                );
        MYSQL_CONTAINER.start();
    }

    /**
     * TestEntityManager - 1차 캐시 제어용
     *
     * <p>persist(), flush(), clear() 메서드를 통해
     * 영속성 컨텍스트를 명시적으로 제어할 수 있습니다.</p>
     */
    @Autowired
    protected TestEntityManager entityManager;

    /**
     * 영속성 컨텍스트를 초기화합니다.
     *
     * <p>flush()로 쿼리를 DB에 전송하고, clear()로 1차 캐시를 비웁니다.
     * 이후 조회 시 실제 DB에서 데이터를 가져오게 됩니다.</p>
     */
    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * 엔티티를 영속화하고 컨텍스트를 초기화합니다.
     *
     * @param entity 저장할 엔티티
     * @param <T> 엔티티 타입
     * @return 영속화된 엔티티 (ID 할당됨)
     */
    protected <T> T persistAndFlush(T entity) {
        T persisted = entityManager.persist(entity);
        flushAndClear();
        return persisted;
    }
}
