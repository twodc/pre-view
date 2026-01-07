package com.example.pre_view.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.member.dto.MemberUpdateRequest;
import com.example.pre_view.domain.member.entity.Member;
import com.example.pre_view.domain.member.repository.MemberRepository;
import com.example.pre_view.support.IntegrationTestSupport;

/**
 * MemberService 통합 테스트
 *
 * <h2>테스트 특징</h2>
 * <ul>
 *   <li>MySQL Testcontainers 사용 (프로덕션 환경 미러링)</li>
 *   <li>실제 트랜잭션 동작 검증</li>
 *   <li>DCI 패턴 (Describe-Context-It) 적용</li>
 *   <li>Awaitility를 활용한 비동기 검증</li>
 * </ul>
 */
@DisplayName("MemberService 통합 테스트")
class MemberServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Nested
    @DisplayName("getCurrentMember 메서드는")
    class Describe_getCurrentMember {

        @Nested
        @DisplayName("존재하는 회원 ID가 주어지면")
        class Context_with_existing_member_id {

            private Member savedMember;

            @BeforeEach
            void setUp() {
                // Given: DB에 회원이 저장되어 있음
                savedMember = memberRepository.save(
                        Member.createLocalMember("test@example.com", "테스트 유저", "encodedPassword")
                );
            }

            @Test
            @DisplayName("회원 정보를 담은 MemberResponse를 반환한다")
            @Transactional
            void it_returns_member_response() {
                // When
                var response = memberService.getCurrentMember(savedMember.getId());

                // Then
                assertThat(response)
                        .isNotNull()
                        .satisfies(r -> {
                            assertThat(r.email()).isEqualTo("test@example.com");
                            assertThat(r.name()).isEqualTo("테스트 유저");
                        });
            }

            @Test
            @DisplayName("실제 MySQL 쿼리가 정상 실행된다")
            @Transactional
            void it_executes_real_mysql_query() {
                // When
                var response = memberService.getCurrentMember(savedMember.getId());

                // Then - 실제 DB에서 조회되었는지 확인
                assertThat(response.email()).isEqualTo(savedMember.getEmail());

                // 컨테이너가 MySQL인지 확인
                assertThat(MYSQL_CONTAINER.isRunning()).isTrue();
                assertThat(MYSQL_CONTAINER.getDatabaseName()).isEqualTo("preview_test");
            }
        }

        @Nested
        @DisplayName("존재하지 않는 회원 ID가 주어지면")
        class Context_with_non_existing_member_id {

            @Test
            @DisplayName("MEMBER_NOT_FOUND 예외를 던진다")
            void it_throws_member_not_found_exception() {
                // Given
                var nonExistingId = 999999L;

                // When & Then
                assertThatThrownBy(() -> memberService.getCurrentMember(nonExistingId))
                        .isInstanceOf(BusinessException.class)
                        .extracting("errorCode")
                        .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("updateProfile 메서드는")
    class Describe_updateProfile {

        @Nested
        @DisplayName("유효한 수정 요청이 주어지면")
        class Context_with_valid_update_request {

            private Member savedMember;

            @BeforeEach
            void setUp() {
                savedMember = memberRepository.save(
                        Member.createLocalMember("update@example.com", "원래 이름", "password")
                );
            }

            @Test
            @DisplayName("이름을 성공적으로 변경한다")
            @Transactional
            void it_updates_name_successfully() {
                // Given
                var request = new MemberUpdateRequest("새로운 이름", null);

                // When
                var response = memberService.updateProfile(savedMember.getId(), request);

                // Then
                assertThat(response.name()).isEqualTo("새로운 이름");

                // DB에도 반영되었는지 확인
                var updatedMember = memberRepository.findById(savedMember.getId()).orElseThrow();
                assertThat(updatedMember.getName()).isEqualTo("새로운 이름");
            }

            @Test
            @DisplayName("프로필 이미지를 성공적으로 변경한다")
            @Transactional
            void it_updates_profile_image_successfully() {
                // Given
                var newImageUrl = "https://cdn.example.com/new-profile.jpg";
                var request = new MemberUpdateRequest(null, newImageUrl);

                // When
                var response = memberService.updateProfile(savedMember.getId(), request);

                // Then
                assertThat(response.profileImage()).isEqualTo(newImageUrl);
            }
        }
    }

    @Nested
    @DisplayName("동시성 테스트")
    class Describe_concurrency {

        @Test
        @DisplayName("여러 스레드에서 동시에 회원 조회가 가능하다 (Awaitility 사용)")
        // @Transactional 제거: 다른 스레드는 커밋된 데이터만 볼 수 있음
        void it_handles_concurrent_reads() {
            // Given: 회원 저장 (트랜잭션 커밋 필요)
            var member = memberRepository.saveAndFlush(
                    Member.createLocalMember("concurrent@example.com", "동시성 테스트", "password")
            );

            // Virtual Thread Executor 사용 (Java 21+)
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

                // When: 10개의 동시 요청
                var futures = java.util.stream.IntStream.range(0, 10)
                        .mapToObj(i -> CompletableFuture.supplyAsync(
                                () -> memberService.getCurrentMember(member.getId()),
                                executor
                        ))
                        .toList();

                // Then: Awaitility로 비동기 검증 (Thread.sleep 대신)
                await()
                        .atMost(Duration.ofSeconds(5))
                        .until(() -> futures.stream().allMatch(CompletableFuture::isDone));

                // 모든 결과 검증
                futures.forEach(future -> {
                    var result = future.join();
                    assertThat(result.email()).isEqualTo("concurrent@example.com");
                });
            } finally {
                // 테스트 데이터 정리
                memberRepository.delete(member);
            }
        }
    }

    @Nested
    @DisplayName("Virtual Thread 검증")
    class Describe_virtual_thread {

        @Test
        @DisplayName("Virtual Thread에서 DB 작업이 정상 동작한다")
        void it_works_on_virtual_thread() throws Exception {
            // Given
            var member = memberRepository.save(
                    Member.createLocalMember("virtual@example.com", "Virtual Thread 테스트", "password")
            );

            // When: Virtual Thread에서 실행
            var resultHolder = new AtomicReference<Boolean>();

            Thread virtualThread = Thread.ofVirtual()
                    .name("test-virtual-thread")
                    .start(() -> {
                        // Virtual Thread 확인
                        resultHolder.set(Thread.currentThread().isVirtual());

                        // DB 조회
                        memberService.getCurrentMember(member.getId());
                    });

            // Then: Virtual Thread 완료 대기
            virtualThread.join();

            assertThat(resultHolder.get())
                    .as("Virtual Thread에서 실행되어야 함")
                    .isTrue();
        }
    }
}
