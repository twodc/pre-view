package com.example.pre_view.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.pre_view.domain.auth.resolver.CurrentMemberIdArgumentResolver;

import lombok.RequiredArgsConstructor;

/**
 * Spring MVC 설정
 *
 * 커스텀 ArgumentResolver 등록 등 MVC 관련 설정을 담당합니다.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CurrentMemberIdArgumentResolver currentMemberIdArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentMemberIdArgumentResolver);
    }
}
