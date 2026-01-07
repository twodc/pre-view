package com.example.pre_view.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.pre_view.domain.auth.jwt.JwtTokenProvider;
import com.example.pre_view.domain.auth.resolver.CurrentMemberIdArgumentResolver;
import com.example.pre_view.domain.auth.service.AccessTokenBlacklistService;

import tools.jackson.databind.json.JsonMapper;

/**
 * @WebMvcTest 슬라이스 테스트를 위한 공통 지원 클래스
 *
 * <h2>설계 의도</h2>
 * <ul>
 *   <li>공통 Mock Bean 선언: Security 관련 빈들을 미리 Mock으로 등록</li>
 *   <li>@CurrentMemberId ArgumentResolver Stubbing: 테스트에서 memberId 주입 문제 해결</li>
 *   <li>JsonMapper 인스턴스 공유: JSON 직렬화 도구 재사용</li>
 * </ul>
 *
 * <h2>사용법</h2>
 * <pre>{@code
 * @WebMvcTest(
 *     controllers = MemberController.class,
 *     excludeAutoConfiguration = {...},
 *     excludeFilters = @ComponentScan.Filter(...)
 * )
 * @Import({
 *     WebMvcTestSupport.TestSecurityConfig.class,
 *     WebMvcTestSupport.ArgumentResolverConfig.class
 * })
 * @DisplayName("MemberController 슬라이스 테스트")
 * class MemberControllerWebMvcTest extends WebMvcTestSupport {
 *
 *     @MockitoBean
 *     private MemberService memberService;
 *
 *     @Test
 *     void 회원조회_테스트() {
 *         // TEST_MEMBER_ID(1L)가 자동으로 주입됨
 *     }
 * }
 * }</pre>
 *
 * <h2>주의사항</h2>
 * <ul>
 *   <li>하위 클래스에서 @MockitoBean으로 ArgumentResolver를 다시 선언하지 마세요</li>
 *   <li>setTestMemberId()로 테스트별 memberId 변경 가능</li>
 * </ul>
 *
 * @see RepositoryTestSupport Repository 슬라이스 테스트용
 * @see IntegrationTestSupport 통합 테스트용
 */
public abstract class WebMvcTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    /**
     * @CurrentMemberId ArgumentResolver Mock
     *
     * <p>실제 SecurityContext에서 memberId를 추출하는 대신,
     * 테스트에서 지정한 값을 반환하도록 stubbing합니다.</p>
     */
    @MockitoBean
    protected CurrentMemberIdArgumentResolver currentMemberIdArgumentResolver;

    /**
     * JWT 토큰 프로바이더 Mock
     */
    @MockitoBean
    protected JwtTokenProvider jwtTokenProvider;

    /**
     * Access Token Blacklist 서비스 Mock
     */
    @MockitoBean
    protected AccessTokenBlacklistService accessTokenBlacklistService;

    /**
     * JSON 직렬화 도구
     */
    protected final JsonMapper jsonMapper = JsonMapper.builder().build();

    /**
     * 테스트에서 사용할 기본 Member ID
     */
    protected static final Long TEST_MEMBER_ID = 1L;

    /**
     * 현재 테스트에서 사용할 Member ID
     * <p>setTestMemberId()로 변경 가능</p>
     */
    private Long currentTestMemberId = TEST_MEMBER_ID;

    /**
     * 각 테스트 전에 ArgumentResolver stubbing을 설정합니다.
     *
     * <p>@CurrentMemberId 어노테이션이 붙은 파라미터에
     * currentTestMemberId 값이 주입되도록 합니다.</p>
     */
    @BeforeEach
    void setUpArgumentResolver() throws Exception {
        // supportsParameter: @CurrentMemberId가 붙은 Long 타입 파라미터 지원
        given(currentMemberIdArgumentResolver.supportsParameter(any()))
                .willAnswer(invocation -> {
                    var parameter = invocation.getArgument(0, org.springframework.core.MethodParameter.class);
                    return parameter.hasParameterAnnotation(
                            com.example.pre_view.domain.auth.annotation.CurrentMemberId.class)
                            && Long.class.isAssignableFrom(parameter.getParameterType());
                });

        // resolveArgument: 테스트용 memberId 반환
        given(currentMemberIdArgumentResolver.resolveArgument(any(), any(), any(), any()))
                .willReturn(currentTestMemberId);
    }

    /**
     * 테스트에서 사용할 Member ID를 변경합니다.
     *
     * <p>권한 검증이나 다른 사용자 접근 테스트 시 사용합니다.</p>
     *
     * @param memberId 새로운 Member ID
     */
    protected void setTestMemberId(Long memberId) throws Exception {
        this.currentTestMemberId = memberId;
        // ArgumentResolver stubbing 재설정
        given(currentMemberIdArgumentResolver.resolveArgument(any(), any(), any(), any()))
                .willReturn(memberId);
    }

    /**
     * 현재 테스트용 Member ID를 반환합니다.
     *
     * @return 현재 설정된 Member ID
     */
    protected Long getCurrentTestMemberId() {
        return currentTestMemberId;
    }
}
