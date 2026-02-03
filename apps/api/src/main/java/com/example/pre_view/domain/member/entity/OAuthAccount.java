package com.example.pre_view.domain.member.entity;

import com.example.pre_view.common.BaseEntity;
import com.example.pre_view.domain.member.enums.Provider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OAuth 계정 연동 정보
 *
 * - 한 회원이 여러 소셜 계정을 연동할 수 있음 (Google + Kakao + ...)
 * - (provider, providerId) 조합은 유니크 (같은 소셜 계정은 한 번만 연동)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "oauth_account",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_oauth_provider_provider_id",
            columnNames = {"provider", "provider_id"}
        )
    }
)
public class OAuthAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    // OAuth 제공자가 부여한 고유 ID (Google sub, Kakao id 등)
    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Builder
    public OAuthAccount(Member member, Provider provider, String providerId) {
        this.member = member;
        this.provider = provider;
        this.providerId = providerId;
    }

    /**
     * OAuth 계정 생성 및 회원과 연결
     */
    public static OAuthAccount create(Member member, Provider provider, String providerId) {
        OAuthAccount oauthAccount = OAuthAccount.builder()
                .member(member)
                .provider(provider)
                .providerId(providerId)
                .build();

        member.linkOAuthAccount(oauthAccount);
        return oauthAccount;
    }
}
