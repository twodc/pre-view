package com.example.pre_view.domain.auth.oauth2;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.pre_view.domain.member.entity.Member;
import com.example.pre_view.domain.member.entity.OAuthAccount;
import com.example.pre_view.domain.member.enums.Provider;
import com.example.pre_view.domain.member.repository.MemberRepository;
import com.example.pre_view.domain.member.repository.OAuthAccountRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 로그인 시 사용자 정보를 처리하는 서비스
 *
 * 새로운 구조:
 * - Member: 회원 기본 정보 (email unique)
 * - OAuthAccount: 연동된 소셜 계정 정보 (1:N)
 *
 * 흐름:
 * 1. OAuth2 제공자(Google)에서 사용자 정보 수신
 * 2. OAuthAccount 테이블에서 기존 연동 여부 확인
 * 3-1. 연동된 계정이 있으면 → 해당 Member 반환 (프로필 업데이트)
 * 3-2. 연동된 계정이 없으면 →
 *      - 같은 이메일의 Member가 있으면 → OAuth 계정 연동
 *      - Member도 없으면 → 신규 Member 생성 + OAuth 계정 연동
 * 4. CustomOAuth2User 반환 → SuccessHandler에서 JWT 발급
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserServiceImpl extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final OAuthAccountRepository oauthAccountRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. OAuth2 제공자로부터 사용자 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. 제공자 식별 (google, kakao 등)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Provider provider = Provider.valueOf(registrationId.toUpperCase());

        // 3. 제공자별 사용자 정보 파싱
        OAuth2UserInfo userInfo = OAuth2UserInfo.of(registrationId, oAuth2User.getAttributes());

        // 4. 회원 조회 또는 생성
        Member member = getOrCreateMember(provider, userInfo);

        log.info("OAuth2 로그인 성공 - provider: {}, email: {}", registrationId, member.getEmail());

        // 5. CustomOAuth2User 반환 (JWT 발급에 필요한 정보 포함)
        return new CustomOAuth2User(member, oAuth2User.getAttributes());
    }

    /**
     * OAuth 계정으로 회원을 조회하거나 생성
     *
     * 케이스:
     * 1. 이미 연동된 OAuth 계정 → 해당 Member 반환 (프로필 업데이트)
     * 2. 연동 안됨 + 같은 이메일 Member 존재 → OAuth 계정 연동
     * 3. 연동 안됨 + Member도 없음 → 신규 가입
     */
    private Member getOrCreateMember(Provider provider, OAuth2UserInfo userInfo) {
        // 1. 이미 연동된 OAuth 계정이 있는지 확인
        return oauthAccountRepository.findMemberByProviderAndProviderId(provider, userInfo.getProviderId())
                .map(existingMember -> {
                    // 기존 연동 회원: 프로필 정보 업데이트
                    existingMember.updateProfile(userInfo.getName(), userInfo.getProfileImage());
                    return existingMember;
                })
                .orElseGet(() -> {
                    // 연동된 계정 없음 → 이메일로 기존 회원 확인
                    return memberRepository.findByEmail(userInfo.getEmail())
                            .map(existingMember -> {
                                // 같은 이메일의 회원 존재 → OAuth 계정 연동
                                linkOAuthAccount(existingMember, provider, userInfo);
                                return existingMember;
                            })
                            .orElseGet(() -> {
                                // 완전 신규 회원 → Member 생성 + OAuth 계정 연동
                                return createNewMemberWithOAuth(provider, userInfo);
                            });
                });
    }

    /**
     * 기존 회원에 OAuth 계정 연동
     */
    private void linkOAuthAccount(Member member, Provider provider, OAuth2UserInfo userInfo) {
        OAuthAccount oauthAccount = OAuthAccount.create(member, provider, userInfo.getProviderId());
        oauthAccountRepository.save(oauthAccount);

        // 프로필 이미지가 없으면 OAuth 프로필로 업데이트
        if (member.getProfileImage() == null) {
            member.updateProfile(member.getName(), userInfo.getProfileImage());
        }

        log.info("OAuth 계정 연동 완료 - memberId: {}, provider: {}", member.getId(), provider);
    }

    /**
     * 신규 회원 생성 + OAuth 계정 연동
     */
    private Member createNewMemberWithOAuth(Provider provider, OAuth2UserInfo userInfo) {
        // 1. Member 생성
        Member newMember = Member.createOAuthMember(
                userInfo.getEmail(),
                userInfo.getName(),
                userInfo.getProfileImage()
        );
        memberRepository.save(newMember);

        // 2. OAuth 계정 연동
        OAuthAccount oauthAccount = OAuthAccount.create(newMember, provider, userInfo.getProviderId());
        oauthAccountRepository.save(oauthAccount);

        log.info("OAuth 신규 회원가입 완료 - email: {}, provider: {}", userInfo.getEmail(), provider);

        return newMember;
    }
}
