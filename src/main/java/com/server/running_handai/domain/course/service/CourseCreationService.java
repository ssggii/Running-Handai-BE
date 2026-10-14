package com.server.running_handai.domain.course.service;

import static com.server.running_handai.global.response.ResponseCode.DUPLICATE_COURSE_NAME;
import static com.server.running_handai.global.response.ResponseCode.MEMBER_NOT_FOUND;

import com.server.running_handai.domain.course.dto.CourseCreateRequestDto;
import com.server.running_handai.domain.course.dto.GpxCourseDraft;
import com.server.running_handai.domain.course.dto.GpxCourseRequestDto;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.domain.member.repository.MemberRepository;
import com.server.running_handai.global.response.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 회원 코스 생성의 흐름을 조율합니다.
 * 외부 API(카카오, OpenAI, S3) 호출 동안 DB 커넥션을 잡지 않도록 이 클래스에는 @Transactional을 붙이지 않고,
 * DB 저장과 이벤트 발행만 CourseCreationSaver의 트랜잭션에서 합니다.
 */
@Service
@RequiredArgsConstructor
public class CourseCreationService {

    private final CourseDataService courseDataService;
    private final CourseCreationSaver courseCreationSaver;
    private final FileService fileService;
    private final MemberRepository memberRepository;
    private final CourseRepository courseRepository;

    /**
     * 회원이 생성한 코스를 저장합니다.
     * 저장에 실패하면 업로드한 GPX·썸네일 파일을 S3에서 삭제합니다.
     *
     * @param memberId 요청 회원의 ID
     * @param request 코스 생성에 필요한 데이터 DTO
     * @return 저장된 코스의 ID
     */
    public Long createMemberCourse(Long memberId, CourseCreateRequestDto request) {
        validateBeforeCreation(memberId, request.startPointName() + "-" + request.endPointName());

        GpxCourseDraft draft = courseDataService.prepareCourseFromGpx(
                new GpxCourseRequestDto(request.startPointName(), request.endPointName()), request.gpxFile());
        String thumbnailUrl = null;
        try {
            thumbnailUrl = fileService.uploadFile(request.thumbnailImage(), "image");
            return courseCreationSaver.saveMemberCourse(memberId, draft, thumbnailUrl, request.isInsideBusan());
        } catch (RuntimeException e) {
            courseDataService.deleteUploadedFile(draft.gpxPath());
            courseDataService.deleteUploadedFile(thumbnailUrl);
            throw e;
        }
    }

    /**
     * 외부 API를 호출하기 전에 끝낼 수 있는 검증을 합니다. (회원 존재, 코스 이름 중복)
     * 검증과 저장 사이에 같은 이름이 생기는 경우는 DB 유니크 제약이 막습니다.
     */
    private void validateBeforeCreation(Long memberId, String courseName) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(MEMBER_NOT_FOUND);
        }
        if (courseRepository.existsByName(courseName)) {
            throw new BusinessException(DUPLICATE_COURSE_NAME);
        }
    }
}
