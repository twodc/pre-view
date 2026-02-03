package com.example.pre_view.domain.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.pre_view.domain.member.entity.Member;
import com.example.pre_view.domain.member.enums.Role;
import com.example.pre_view.support.RepositoryTestSupport;

/**
 * MemberRepository 슬라이스 테스트
 *
 * <h2>설계 의도</h2>
 * <ul>
 *   <li>RepositoryTestSupport 상속: MySQL Testcontainers 공유, JPA Auditing 활성화</li>
 *   <li>DCI 패턴: Describe-Context-It 구조로 가독성 향상</li>
 * </ul>
 *
 * <h2>주요 검증 포인트</h2>
 * <ul>
 *   <li>커스텀 쿼리 메서드 동작 검증 (findByEmail, existsByEmail)</li>
 *   <li>JPA Auditing 필드 자동 설정 검증</li>
 *   <li>Unique 제약 조건 검증</li>
 * </ul>
 *
 * @see MemberRepository
 * @see RepositoryTestSupport
 */
@DisplayName("MemberRepository 슬라이스 테스트")
class MemberRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    // 테스트 픽스처
    private Member savedMember;
    private static final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 기본 회원 데이터 생성
        savedMember = Member.builder()
                .email(TEST_EMAIL)
                .name("테스트 유저")
                .profileImage("https://example.com/profile.jpg")
                .role(Role.USER)
                .build();

        // persist -> flush -> clear로 1차 캐시 비우기
        persistAndFlush(savedMember);
    }

    @Nested
    @DisplayName("findByEmail 메서드는")
    class Describe_findByEmail {

        @Nested
        @DisplayName("존재하는 이메일이 주어지면")
        class Context_with_existing_email {

            @Test
            @DisplayName("해당 Member를 Optional로 감싸서 반환한다")
            void it_returns_member_wrapped_in_optional() {
                // when
                var result = memberRepository.findByEmail(TEST_EMAIL);

                // then
                assertThat(result)
                        .isPresent()
                        .get()
                        .satisfies(member -> {
                            assertThat(member.getEmail()).isEqualTo(TEST_EMAIL);
                            assertThat(member.getName()).isEqualTo("테스트 유저");
                            assertThat(member.getProfileImage()).isEqualTo("https://example.com/profile.jpg");
                            assertThat(member.getRole()).isEqualTo(Role.USER);
                        });
            }
        }

        @Nested
        @DisplayName("존재하지 않는 이메일이 주어지면")
        class Context_with_non_existing_email {

            @Test
            @DisplayName("빈 Optional을 반환한다")
            void it_returns_empty_optional() {
                // when
                var result = memberRepository.findByEmail("nonexistent@example.com");

                // then
                assertThat(result).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("existsByEmail 메서드는")
    class Describe_existsByEmail {

        @Nested
        @DisplayName("존재하는 이메일이 주어지면")
        class Context_with_existing_email {

            @Test
            @DisplayName("true를 반환한다")
            void it_returns_true() {
                // when
                boolean exists = memberRepository.existsByEmail(TEST_EMAIL);

                // then
                assertThat(exists).isTrue();
            }
        }

        @Nested
        @DisplayName("존재하지 않는 이메일이 주어지면")
        class Context_with_non_existing_email {

            @Test
            @DisplayName("false를 반환한다")
            void it_returns_false() {
                // when
                boolean exists = memberRepository.existsByEmail("nonexistent@example.com");

                // then
                assertThat(exists).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("JPA Auditing은")
    class Describe_jpa_auditing {

        @Test
        @DisplayName("엔티티 저장 시 createdAt과 updatedAt을 자동으로 설정한다")
        void it_sets_audit_fields_on_save() {
            // given
            Member newMember = Member.builder()
                    .email("audit-test@example.com")
                    .name("Audit 테스트")
                    .role(Role.USER)
                    .build();

            // when
            memberRepository.save(newMember);
            flushAndClear();

            // then
            var found = memberRepository.findByEmail("audit-test@example.com");
            assertThat(found)
                    .isPresent()
                    .get()
                    .satisfies(member -> {
                        assertThat(member.getCreatedAt()).isNotNull();
                        assertThat(member.getUpdatedAt()).isNotNull();
                        assertThat(member.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
                    });
        }

        @Test
        @DisplayName("엔티티 수정 시 updatedAt을 자동으로 갱신한다")
        void it_updates_updatedAt_on_modification() {
            // given - 이미 저장된 회원 조회
            var member = memberRepository.findByEmail(TEST_EMAIL).orElseThrow();
            LocalDateTime originalUpdatedAt = member.getUpdatedAt();

            // when - 프로필 수정
            member.updateProfile("수정된 이름", "https://new-image.com/profile.jpg");
            flushAndClear();

            // then - updatedAt이 갱신되었는지 확인 (시간 정밀도에 따라 같을 수도 있음)
            var updated = memberRepository.findByEmail(TEST_EMAIL).orElseThrow();
            assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
            assertThat(updated.getName()).isEqualTo("수정된 이름");
            // 중요: 이름이 변경되었다면 dirty checking으로 update 쿼리가 실행됨
        }
    }

    @Nested
    @DisplayName("Unique 제약 조건은")
    class Describe_unique_constraint {

        @Test
        @DisplayName("동일한 이메일로 회원 생성 시 예외가 발생한다")
        void it_throws_exception_for_duplicate_email() {
            // given - 이미 동일 이메일의 회원이 존재함 (setUp에서 생성)
            Member duplicateMember = Member.builder()
                    .email(TEST_EMAIL)  // 중복 이메일
                    .name("중복 유저")
                    .role(Role.USER)
                    .build();

            // when & then
            assertThatThrownBy(() -> {
                memberRepository.save(duplicateMember);
                flushAndClear();
            }).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("delete 메서드 (소프트 삭제)는")
    class Describe_soft_delete {

        @Test
        @DisplayName("deleted 플래그를 true로 설정하고 deletedAt을 기록한다")
        void it_sets_deleted_flag_and_timestamp() {
            // given
            var member = memberRepository.findByEmail(TEST_EMAIL).orElseThrow();

            // when
            member.delete();
            flushAndClear();

            // then
            var deleted = memberRepository.findById(member.getId()).orElseThrow();
            assertThat(deleted.isDeleted()).isTrue();
            assertThat(deleted.getDeletedAt()).isNotNull();
            assertThat(deleted.getDeletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        }
    }
}
