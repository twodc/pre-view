package com.example.pre_view.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.member.dto.MemberResponse;
import com.example.pre_view.domain.member.dto.MemberUpdateRequest;
import com.example.pre_view.domain.member.entity.Member;
import com.example.pre_view.domain.member.enums.Role;
import com.example.pre_view.domain.member.repository.MemberRepository;

/**
 * MemberService 단위 테스트
 *
 * <h2>테스트 철학</h2>
 * <ul>
 *   <li>Spring Context 로딩 없이 순수 Java + Mockito로 빠른 테스트</li>
 *   <li>DCI 패턴 (Describe-Context-It) 적용으로 가독성 향상</li>
 *   <li>BDD 스타일 (given-when-then) 사용</li>
 *   <li>AssertJ를 활용한 Fluent Assertion</li>
 * </ul>
 *
 * <h2>단위 테스트 vs 통합 테스트</h2>
 * <p>
 * 단위 테스트는 비즈니스 로직만 검증하고, DB 연동은 통합 테스트에서 검증합니다.
 * 이렇게 분리하면 테스트가 빠르고, 실패 원인을 쉽게 파악할 수 있습니다.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 단위 테스트")
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    // 테스트용 Fixture
    private Member testMember;
    private static final Long TEST_MEMBER_ID = 1L;

    @BeforeEach
    void setUp() {
        testMember = createTestMember();
    }

    /**
     * 테스트용 Member 객체 생성 (Fixture)
     */
    private Member createTestMember() {
        return Member.builder()
                .email("test@example.com")
                .name("테스트 유저")
                .profileImage("https://example.com/profile.jpg")
                .role(Role.USER)
                .build();
    }

    @Nested
    @DisplayName("getCurrentMember 메서드는")
    class Describe_getCurrentMember {

        @Nested
        @DisplayName("존재하는 회원 ID가 주어지면")
        class Context_with_valid_member_id {

            @BeforeEach
            void setUp() {
                // Given: Repository가 회원을 반환하도록 설정
                given(memberRepository.findById(TEST_MEMBER_ID))
                        .willReturn(Optional.of(testMember));
            }

            @Test
            @DisplayName("MemberResponse를 반환한다")
            void it_returns_member_response() {
                // When
                MemberResponse response = memberService.getCurrentMember(TEST_MEMBER_ID);

                // Then
                assertThat(response)
                        .isNotNull()
                        .satisfies(r -> {
                            assertThat(r.email()).isEqualTo("test@example.com");
                            assertThat(r.name()).isEqualTo("테스트 유저");
                            assertThat(r.profileImage()).isEqualTo("https://example.com/profile.jpg");
                        });
            }

            @Test
            @DisplayName("Repository의 findById를 정확히 1번 호출한다")
            void it_calls_repository_once() {
                // When
                memberService.getCurrentMember(TEST_MEMBER_ID);

                // Then: Mockito BDD 스타일 검증
                then(memberRepository)
                        .should(times(1))
                        .findById(TEST_MEMBER_ID);
            }
        }

        @Nested
        @DisplayName("존재하지 않는 회원 ID가 주어지면")
        class Context_with_invalid_member_id {

            @BeforeEach
            void setUp() {
                // Given: Repository가 빈 Optional을 반환
                given(memberRepository.findById(anyLong()))
                        .willReturn(Optional.empty());
            }

            @Test
            @DisplayName("MEMBER_NOT_FOUND 예외를 던진다")
            void it_throws_member_not_found_exception() {
                // When & Then
                assertThatThrownBy(() -> memberService.getCurrentMember(999L))
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("updateProfile 메서드는")
    class Describe_updateProfile {

        @Nested
        @DisplayName("이름만 수정 요청하면")
        class Context_with_name_only {

            @BeforeEach
            void setUp() {
                given(memberRepository.findById(TEST_MEMBER_ID))
                        .willReturn(Optional.of(testMember));
            }

            @Test
            @DisplayName("이름만 변경되고 프로필 이미지는 유지된다")
            void it_updates_only_name() {
                // Given
                var request = new MemberUpdateRequest("새로운 이름", null);

                // When
                MemberResponse response = memberService.updateProfile(TEST_MEMBER_ID, request);

                // Then
                assertThat(response.name()).isEqualTo("새로운 이름");
                assertThat(response.profileImage()).isEqualTo("https://example.com/profile.jpg");
            }
        }

        @Nested
        @DisplayName("프로필 이미지만 수정 요청하면")
        class Context_with_profile_image_only {

            @BeforeEach
            void setUp() {
                given(memberRepository.findById(TEST_MEMBER_ID))
                        .willReturn(Optional.of(testMember));
            }

            @Test
            @DisplayName("프로필 이미지만 변경되고 이름은 유지된다")
            void it_updates_only_profile_image() {
                // Given
                var newImageUrl = "https://new-image.com/profile.jpg";
                var request = new MemberUpdateRequest(null, newImageUrl);

                // When
                MemberResponse response = memberService.updateProfile(TEST_MEMBER_ID, request);

                // Then
                assertThat(response.name()).isEqualTo("테스트 유저");
                assertThat(response.profileImage()).isEqualTo(newImageUrl);
            }
        }

        @Nested
        @DisplayName("이름과 프로필 이미지 모두 수정 요청하면")
        class Context_with_both_fields {

            @BeforeEach
            void setUp() {
                given(memberRepository.findById(TEST_MEMBER_ID))
                        .willReturn(Optional.of(testMember));
            }

            @Test
            @DisplayName("두 필드 모두 변경된다")
            void it_updates_both_fields() {
                // Given
                var request = new MemberUpdateRequest("새로운 이름", "https://new-image.com/profile.jpg");

                // When
                MemberResponse response = memberService.updateProfile(TEST_MEMBER_ID, request);

                // Then
                assertThat(response)
                        .satisfies(r -> {
                            assertThat(r.name()).isEqualTo("새로운 이름");
                            assertThat(r.profileImage()).isEqualTo("https://new-image.com/profile.jpg");
                        });
            }
        }

        @Nested
        @DisplayName("존재하지 않는 회원 ID로 수정 요청하면")
        class Context_with_non_existing_member {

            @BeforeEach
            void setUp() {
                given(memberRepository.findById(anyLong()))
                        .willReturn(Optional.empty());
            }

            @Test
            @DisplayName("MEMBER_NOT_FOUND 예외를 던진다")
            void it_throws_member_not_found_exception() {
                // Given
                var request = new MemberUpdateRequest("새로운 이름", null);

                // When & Then
                assertThatThrownBy(() -> memberService.updateProfile(999L, request))
                        .isInstanceOf(BusinessException.class)
                        .extracting("errorCode")
                        .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }
}
