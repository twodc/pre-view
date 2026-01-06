package com.example.pre_view.domain.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.member.dto.MemberResponse;
import com.example.pre_view.domain.member.dto.MemberUpdateRequest;
import com.example.pre_view.domain.member.entity.Member;
import com.example.pre_view.domain.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원 관련 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    /**
     * 현재 로그인한 회원 정보 조회
     */
    @Transactional(readOnly = true)
    public MemberResponse getCurrentMember(Long memberId) {
        log.debug("회원 정보 조회 - memberId: {}", memberId);
        Member member = getMemberById(memberId);
        return MemberResponse.from(member);
    }

    /**
     * 회원 프로필 수정
     */
    @Transactional
    public MemberResponse updateProfile(Long memberId, MemberUpdateRequest request) {
        log.info("회원 프로필 수정 시작 - memberId: {}", memberId);
        Member member = getMemberById(memberId);

        // null이 아닌 필드만 업데이트
        String newName = request.name() != null ? request.name() : member.getName();
        String newProfileImage = request.profileImage() != null ? request.profileImage() : member.getProfileImage();

        member.updateProfile(newName, newProfileImage);
        log.info("회원 프로필 수정 완료 - memberId: {}", memberId);

        return MemberResponse.from(member);
    }

    private Member getMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.warn("회원을 찾을 수 없음 - memberId: {}", memberId);
                    return new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
                });
    }
}
