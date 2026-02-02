package com.example.pre_view.domain.member.entity;

import java.util.ArrayList;
import java.util.List;

import com.example.pre_view.common.BaseEntity;
import com.example.pre_view.domain.member.enums.Role;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 엔티티
 *
 * - email: 회원의 고유 식별자 (unique)
 * - password: 일반 회원가입 시 사용 (OAuth 회원은 null)
 * - oauthAccounts: 연동된 소셜 계정 목록 (1:N)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    private String profileImage;

    // 일반 회원가입 시 비밀번호 (OAuth 전용 회원은 null)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // 연동된 OAuth 계정들 (Google, Kakao 등)
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OAuthAccount> oauthAccounts = new ArrayList<>();

    @Builder
    public Member(String email, String name, String profileImage, String password, Role role) {
        this.email = email;
        this.name = name;
        this.profileImage = profileImage;
        this.password = password;
        this.role = role != null ? role : Role.USER;
    }

    /**
     * 일반 회원가입용 정적 팩토리 메서드
     */
    public static Member createLocalMember(String email, String name, String encodedPassword) {
        return Member.builder()
                .email(email)
                .name(name)
                .password(encodedPassword)
                .role(Role.USER)
                .build();
    }

    /**
     * OAuth 로그인으로 신규 가입 시 사용
     */
    public static Member createOAuthMember(String email, String name, String profileImage) {
        return Member.builder()
                .email(email)
                .name(name)
                .profileImage(profileImage)
                .role(Role.USER)
                .build();
    }

    /**
     * OAuth 로그인 시 변경된 사용자 정보를 업데이트합니다.
     */
    public void updateProfile(String name, String profileImage) {
        this.name = name;
        this.profileImage = profileImage;
    }

    /**
     * 비밀번호 설정 (OAuth 회원이 비밀번호를 추가 설정할 때)
     */
    public void setPassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /**
     * 일반 로그인이 가능한 회원인지 확인
     */
    public boolean hasPassword() {
        return this.password != null;
    }

    /**
     * OAuth 계정 연동
     */
    public void linkOAuthAccount(OAuthAccount oauthAccount) {
        this.oauthAccounts.add(oauthAccount);
    }

    /**
     * 사용자 권한 변경 (관리자 전용)
     */
    public void updateRole(Role role) {
        this.role = role;
    }

    /**
     * 계정 비활성화 (관리자 전용)
     * BaseEntity의 delete() 메서드를 활용하여 소프트 삭제
     */
    public void deactivate() {
        super.delete();
    }

    /**
     * 계정 활성화 상태 확인
     */
    public boolean isActive() {
        return !isDeleted();
    }
}
