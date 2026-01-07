package com.example.pre_view.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정
 *
 * @EnableJpaAuditing을 별도 Configuration으로 분리하여
 * @WebMvcTest 등 슬라이스 테스트에서 JPA 관련 빈 로드 문제를 방지
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
