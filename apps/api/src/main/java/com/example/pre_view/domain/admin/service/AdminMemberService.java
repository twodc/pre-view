package com.example.pre_view.domain.admin.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.pre_view.common.exception.BusinessException;
import com.example.pre_view.common.exception.ErrorCode;
import com.example.pre_view.domain.admin.dto.AdminMemberResponse;
import com.example.pre_view.domain.admin.dto.RoleUpdateRequest;
import com.example.pre_view.domain.member.entity.Member;
import com.example.pre_view.domain.member.enums.Role;
import com.example.pre_view.domain.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관리자용 회원 관리 서비스
 *
 * 관리자가 회원을 조회, 권한 변경, 비활성화할 수 있는 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    private final MemberRepository memberRepository;

    /**
     * 회원 목록 조회 (페이징)
     */
    public Page<AdminMemberResponse> getMembers(Pageable pageable) {
        log.debug("관리자 회원 목록 조회 - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return memberRepository.findByDeletedFalse(pageable)
                .map(AdminMemberResponse::from);
    }

    /**
     * 회원 상세 조회
     */
    public AdminMemberResponse getMember(Long memberId) {
        log.debug("관리자 회원 상세 조회 - memberId: {}", memberId);
        Member member = findMemberById(memberId);
        return AdminMemberResponse.from(member);
    }

    /**
     * 회원 권한 변경
     *
     * @param memberId 대상 회원 ID
     * @param adminMemberId 요청한 관리자 ID
     * @param request 권한 변경 요청
     * @return 변경된 회원 정보
     * @throws BusinessException 자신의 권한을 변경하려는 경우
     */
    @Transactional
    public AdminMemberResponse updateRole(Long memberId, Long adminMemberId, RoleUpdateRequest request) {
        log.info("회원 권한 변경 - targetMemberId: {}, adminMemberId: {}, newRole: {}",
                memberId, adminMemberId, request.role());

        // 자기 자신 권한 변경 방지
        if (memberId.equals(adminMemberId)) {
            throw new BusinessException(ErrorCode.CANNOT_MODIFY_OWN_ROLE);
        }

        Member member = findMemberById(memberId);
        Role oldRole = member.getRole();
        member.updateRole(request.role());

        log.info("회원 권한 변경 완료 - memberId: {}, oldRole: {}, newRole: {}",
                memberId, oldRole, request.role());

        return AdminMemberResponse.from(member);
    }

    /**
     * 회원 계정 비활성화
     *
     * @param memberId 대상 회원 ID
     * @param adminMemberId 요청한 관리자 ID
     * @return 비활성화된 회원 정보
     * @throws BusinessException 이미 비활성화된 계정이거나 자신을 비활성화하려는 경우
     */
    @Transactional
    public AdminMemberResponse deactivateMember(Long memberId, Long adminMemberId) {
        log.info("회원 계정 비활성화 - targetMemberId: {}, adminMemberId: {}", memberId, adminMemberId);

        // 자기 자신 비활성화 방지
        if (memberId.equals(adminMemberId)) {
            throw new BusinessException(ErrorCode.CANNOT_MODIFY_OWN_ROLE);
        }

        Member member = findMemberById(memberId);

        // 이미 비활성화된 계정 확인
        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.ALREADY_DEACTIVATED);
        }

        member.deactivate();
        log.info("회원 계정 비활성화 완료 - memberId: {}", memberId);

        return AdminMemberResponse.from(member);
    }

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
