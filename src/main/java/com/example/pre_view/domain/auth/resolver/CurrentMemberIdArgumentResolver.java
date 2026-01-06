package com.example.pre_view.domain.auth.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.auth.annotation.CurrentMemberId;

/**
 * @CurrentMemberId 어노테이션을 처리하는 ArgumentResolver
 *
 * SecurityContext에서 현재 인증된 사용자의 memberId를 추출하여
 * 컨트롤러 메서드 파라미터에 주입합니다.
 */
@Component
public class CurrentMemberIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // @CurrentMemberId 어노테이션이 붙어있고, 타입이 Long인 경우에만 처리
        return parameter.hasParameterAnnotation(CurrentMemberId.class)
                && Long.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();

        // JwtAuthenticationFilter에서 memberId를 String으로 저장함
        if (principal instanceof String memberId) {
            try {
                return Long.parseLong(memberId);
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}
