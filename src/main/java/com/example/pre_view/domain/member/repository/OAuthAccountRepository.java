package com.example.pre_view.domain.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.pre_view.domain.member.entity.Member;
import com.example.pre_view.domain.member.entity.OAuthAccount;
import com.example.pre_view.domain.member.enums.Provider;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {

    /**
     * OAuth 제공자와 제공자 ID로 OAuth 계정을 조회합니다.
     * 소셜 로그인 시 기존 연동 여부 확인에 사용
     */
    Optional<OAuthAccount> findByProviderAndProviderId(Provider provider, String providerId);

    /**
     * OAuth 제공자와 제공자 ID로 연동된 회원을 조회합니다.
     * Member를 함께 fetch join하여 N+1 문제 방지
     */
    @Query("SELECT oa.member FROM OAuthAccount oa " +
           "WHERE oa.provider = :provider AND oa.providerId = :providerId")
    Optional<Member> findMemberByProviderAndProviderId(
            @Param("provider") Provider provider,
            @Param("providerId") String providerId
    );

    /**
     * 해당 OAuth 계정이 이미 연동되어 있는지 확인합니다.
     */
    boolean existsByProviderAndProviderId(Provider provider, String providerId);
}
