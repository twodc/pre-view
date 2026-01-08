package com.example.pre_view.domain.auth.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * 현재 인증된 사용자의 memberId를 주입받는 커스텀 어노테이션
 *
 * 컨트롤러 메서드 파라미터에 사용하면 SecurityContext에서
 * 현재 인증된 사용자의 ID를 자동으로 주입받습니다.
 *
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Parameter(hidden = true)
public @interface CurrentMemberId {
}
