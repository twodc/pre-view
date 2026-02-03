package com.example.pre_view.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.example.pre_view.domain.auth.jwt.JwtAuthenticationFilter;
import com.example.pre_view.domain.auth.oauth2.OAuth2SuccessHandler;
import com.example.pre_view.domain.auth.oauth2.OAuth2UserServiceImpl;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security 설정
 *
 * OAuth2 소셜 로그인 + JWT 토큰 인증 조합
 * - OAuth2 로그인 성공 → JWT 발급 → 클라이언트 저장
 * - 이후 요청은 JWT로 인증 (Stateless)
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2UserServiceImpl oAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정 활성화
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // CSRF 비활성화 (JWT 사용 시 불필요, 토큰 자체가 CSRF 방어)
                .csrf(csrf -> csrf.disable())

                // 세션 사용 안함 (JWT는 Stateless)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 경로별 인가 설정
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 접근 가능한 경로
                        .requestMatchers(
                                "/",
                                "/api/v1/auth/**",    // 로그인, 토큰 재발급 등
                                "/api/loadtest/**",   // 부하 테스트용 (loadtest 프로파일에서만 활성화)
                                "/oauth2/**",         // OAuth2 관련 경로
                                "/login/**",          // 로그인 페이지
                                "/swagger-ui.html",   // Swagger UI 진입점
                                "/swagger-ui/**",     // Swagger UI 리소스
                                "/api-docs/**",       // API 문서
                                "/v3/api-docs/**",    // OpenAPI 3.0
                                "/error",             // 에러 페이지
                                "/actuator/health",   // 헬스 체크 (기본)
                                "/actuator/health/**" // liveness/readiness probes
                        ).permitAll()

                        // Actuator 상세 정보는 인증 필요 (보안)
                        .requestMatchers("/actuator/**").authenticated()

                        // 관리자 전용 경로
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )

                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        // OAuth2 로그인 성공 후 사용자 정보 처리
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(oAuth2UserService)
                        )
                        // 로그인 성공 시 JWT 발급 처리
                        .successHandler(oAuth2SuccessHandler)
                )

                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                // → 모든 요청에서 JWT 먼저 검증
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 비밀번호 암호화
     * BCrypt: 단방향 해시 + Salt 자동 생성
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
