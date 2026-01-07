package com.example.pre_view.support;

import java.util.List;

import com.example.pre_view.domain.answer.entity.Answer;
import com.example.pre_view.domain.interview.entity.Interview;
import com.example.pre_view.domain.interview.enums.ExperienceLevel;
import com.example.pre_view.domain.interview.enums.InterviewPhase;
import com.example.pre_view.domain.interview.enums.InterviewStatus;
import com.example.pre_view.domain.interview.enums.InterviewType;
import com.example.pre_view.domain.interview.enums.Position;
import com.example.pre_view.domain.member.entity.Member;
import com.example.pre_view.domain.member.enums.Role;
import com.example.pre_view.domain.question.entity.Question;

/**
 * 테스트 픽스처 팩토리 클래스
 *
 * <h2>설계 의도</h2>
 * <ul>
 *   <li>테스트 객체 생성 로직 중앙화</li>
 *   <li>일관된 테스트 데이터 보장</li>
 *   <li>테스트 코드 가독성 향상</li>
 * </ul>
 *
 * <h2>사용법</h2>
 * <pre>{@code
 * // 기본 Member 생성
 * Member member = TestFixtures.member();
 *
 * // 커스텀 이메일로 Member 생성
 * Member member = TestFixtures.member("custom@example.com");
 *
 * // 기본 Interview 생성
 * Interview interview = TestFixtures.interview(1L);
 *
 * // 기술 면접 Interview 생성
 * Interview interview = TestFixtures.technicalInterview(1L);
 * }</pre>
 */
public final class TestFixtures {

    // 기본 테스트 데이터 상수
    public static final String DEFAULT_EMAIL = "test@example.com";
    public static final String DEFAULT_NAME = "테스트 유저";
    public static final String DEFAULT_PROFILE_IMAGE = "https://example.com/profile.jpg";
    public static final String DEFAULT_PASSWORD = "encodedPassword123!";

    public static final Long DEFAULT_MEMBER_ID = 1L;
    public static final String DEFAULT_INTERVIEW_TITLE = "백엔드 기술 면접";
    public static final List<String> DEFAULT_TECH_STACKS = List.of("Java", "Spring", "MySQL");

    private TestFixtures() {
        // 인스턴스화 방지
    }

    // ==================== Member Fixtures ====================

    /**
     * 기본 Member 생성
     */
    public static Member member() {
        return Member.builder()
                .email(DEFAULT_EMAIL)
                .name(DEFAULT_NAME)
                .profileImage(DEFAULT_PROFILE_IMAGE)
                .role(Role.USER)
                .build();
    }

    /**
     * 커스텀 이메일로 Member 생성
     */
    public static Member member(String email) {
        return Member.builder()
                .email(email)
                .name(DEFAULT_NAME)
                .profileImage(DEFAULT_PROFILE_IMAGE)
                .role(Role.USER)
                .build();
    }

    /**
     * 커스텀 이메일과 이름으로 Member 생성
     */
    public static Member member(String email, String name) {
        return Member.builder()
                .email(email)
                .name(name)
                .profileImage(DEFAULT_PROFILE_IMAGE)
                .role(Role.USER)
                .build();
    }

    /**
     * 로컬 회원 생성 (비밀번호 포함)
     */
    public static Member localMember() {
        return Member.createLocalMember(DEFAULT_EMAIL, DEFAULT_NAME, DEFAULT_PASSWORD);
    }

    /**
     * 커스텀 이메일로 로컬 회원 생성
     */
    public static Member localMember(String email) {
        return Member.createLocalMember(email, DEFAULT_NAME, DEFAULT_PASSWORD);
    }

    // ==================== Interview Fixtures ====================

    /**
     * 기본 Interview 생성 (FULL 타입, READY 상태)
     */
    public static Interview interview(Long memberId) {
        return Interview.builder()
                .memberId(memberId)
                .title(DEFAULT_INTERVIEW_TITLE)
                .type(InterviewType.FULL)
                .position(Position.BACKEND)
                .level(ExperienceLevel.JUNIOR)
                .techStacks(DEFAULT_TECH_STACKS)
                .status(InterviewStatus.READY)
                .build();
    }

    /**
     * 커스텀 제목으로 Interview 생성
     */
    public static Interview interview(Long memberId, String title) {
        return Interview.builder()
                .memberId(memberId)
                .title(title)
                .type(InterviewType.FULL)
                .position(Position.BACKEND)
                .level(ExperienceLevel.JUNIOR)
                .techStacks(DEFAULT_TECH_STACKS)
                .status(InterviewStatus.READY)
                .build();
    }

    /**
     * 기술 면접 Interview 생성 (TECHNICAL 타입)
     */
    public static Interview technicalInterview(Long memberId) {
        return Interview.builder()
                .memberId(memberId)
                .title("기술 면접")
                .type(InterviewType.TECHNICAL)
                .position(Position.BACKEND)
                .level(ExperienceLevel.JUNIOR)
                .techStacks(DEFAULT_TECH_STACKS)
                .status(InterviewStatus.READY)
                .build();
    }

    /**
     * 인성 면접 Interview 생성 (PERSONALITY 타입)
     */
    public static Interview personalityInterview(Long memberId) {
        return Interview.builder()
                .memberId(memberId)
                .title("인성 면접")
                .type(InterviewType.PERSONALITY)
                .position(Position.BACKEND)
                .level(ExperienceLevel.JUNIOR)
                .status(InterviewStatus.READY)
                .build();
    }

    /**
     * 진행 중인 Interview 생성
     */
    public static Interview inProgressInterview(Long memberId) {
        return Interview.builder()
                .memberId(memberId)
                .title(DEFAULT_INTERVIEW_TITLE)
                .type(InterviewType.FULL)
                .position(Position.BACKEND)
                .level(ExperienceLevel.JUNIOR)
                .techStacks(DEFAULT_TECH_STACKS)
                .status(InterviewStatus.IN_PROGRESS)
                .build();
    }

    // ==================== Question Fixtures ====================

    /**
     * 기본 기술 질문 생성
     */
    public static Question technicalQuestion(Interview interview, int sequence) {
        return Question.builder()
                .interview(interview)
                .content("Java의 GC에 대해 설명해주세요.")
                .phase(InterviewPhase.TECHNICAL)
                .sequence(sequence)
                .isFollowUp(false)
                .build();
    }

    /**
     * 커스텀 내용의 기술 질문 생성
     */
    public static Question technicalQuestion(Interview interview, String content, int sequence) {
        return Question.builder()
                .interview(interview)
                .content(content)
                .phase(InterviewPhase.TECHNICAL)
                .sequence(sequence)
                .isFollowUp(false)
                .build();
    }

    /**
     * 인성 질문 생성
     */
    public static Question personalityQuestion(Interview interview, int sequence) {
        return Question.builder()
                .interview(interview)
                .content("팀 프로젝트에서 갈등을 해결한 경험이 있나요?")
                .phase(InterviewPhase.PERSONALITY)
                .sequence(sequence)
                .isFollowUp(false)
                .build();
    }

    /**
     * 꼬리 질문 생성
     */
    public static Question followUpQuestion(Interview interview, Question parentQuestion, int sequence) {
        return Question.builder()
                .interview(interview)
                .content("더 자세히 설명해주세요.")
                .phase(parentQuestion.getPhase())
                .sequence(sequence)
                .parentQuestion(parentQuestion)
                .isFollowUp(true)
                .build();
    }

    /**
     * 오프닝 질문 생성
     */
    public static Question openingQuestion(Interview interview, int sequence) {
        return Question.builder()
                .interview(interview)
                .content("자기소개를 해주세요.")
                .phase(InterviewPhase.OPENING)
                .sequence(sequence)
                .isFollowUp(false)
                .build();
    }

    /**
     * 클로징 질문 생성
     */
    public static Question closingQuestion(Interview interview, int sequence) {
        return Question.builder()
                .interview(interview)
                .content("마지막으로 하고 싶은 말이 있나요?")
                .phase(InterviewPhase.CLOSING)
                .sequence(sequence)
                .isFollowUp(false)
                .build();
    }

    // ==================== Answer Fixtures ====================

    /**
     * 기본 Answer 생성
     */
    public static Answer answer(Question question) {
        return Answer.builder()
                .question(question)
                .content("GC는 더 이상 사용되지 않는 객체를 자동으로 회수합니다.")
                .feedback("좋은 답변입니다.")
                .score(80)
                .build();
    }

    /**
     * 커스텀 내용의 Answer 생성
     */
    public static Answer answer(Question question, String content, int score) {
        return Answer.builder()
                .question(question)
                .content(content)
                .feedback("피드백입니다.")
                .score(score)
                .build();
    }

    /**
     * 피드백 포함 Answer 생성
     */
    public static Answer answer(Question question, String content, String feedback, int score) {
        return Answer.builder()
                .question(question)
                .content(content)
                .feedback(feedback)
                .score(score)
                .build();
    }
}
