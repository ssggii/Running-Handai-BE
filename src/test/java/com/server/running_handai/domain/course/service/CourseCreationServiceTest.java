package com.server.running_handai.domain.course.service;

import static com.server.running_handai.global.response.ResponseCode.DUPLICATE_COURSE_NAME;
import static com.server.running_handai.global.response.ResponseCode.MEMBER_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.server.running_handai.domain.course.dto.CourseCreateRequestDto;
import com.server.running_handai.domain.course.dto.GpxCourseDraft;
import com.server.running_handai.domain.course.dto.GpxCourseRequestDto;
import com.server.running_handai.domain.course.entity.Area;
import com.server.running_handai.domain.course.entity.CourseLevel;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.domain.member.repository.MemberRepository;
import com.server.running_handai.global.response.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class CourseCreationServiceTest {

    @InjectMocks
    private CourseCreationService courseCreationService;

    @Mock
    private CourseDataService courseDataService;

    @Mock
    private CourseCreationSaver courseCreationSaver;

    @Mock
    private FileService fileService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CourseRepository courseRepository;

    private final String START_POINT_NAME = "광안리해수욕장";
    private final String END_POINT_NAME = "해운대해수욕장";
    private final String COURSE_NAME = START_POINT_NAME + "-" + END_POINT_NAME;

    private MultipartFile gpxFile;
    private MultipartFile thumbnailImgFile;
    private CourseCreateRequestDto request;

    @BeforeEach
    void setUp() {
        gpxFile = new MockMultipartFile("gpxFile", "test.gpx", "application/gpx+xml", "<gpx></gpx>".getBytes());
        thumbnailImgFile = new MockMultipartFile("thumbnail", "thumb.jpg", "image/jpeg", "thumbnail-image".getBytes());
        request = new CourseCreateRequestDto(START_POINT_NAME, END_POINT_NAME, gpxFile, thumbnailImgFile, true);
    }

    @Test
    @DisplayName("내 코스 생성 성공 - 외부 호출 결과를 저장 빈에 넘긴다")
    void createMemberCourse_success() {
        // given
        Long memberId = 1L;
        Long courseId = 100L;
        GpxCourseDraft draft = createDraft();

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(courseRepository.existsByName(COURSE_NAME)).thenReturn(false);
        when(courseDataService.prepareCourseFromGpx(any(GpxCourseRequestDto.class), eq(gpxFile))).thenReturn(draft);
        when(fileService.uploadFile(thumbnailImgFile, "image")).thenReturn("https://s3/image/thumb.jpg");
        when(courseCreationSaver.saveMemberCourse(memberId, draft, "https://s3/image/thumb.jpg", true)).thenReturn(courseId);

        // when
        Long result = courseCreationService.createMemberCourse(memberId, request);

        // then
        assertThat(result).isEqualTo(courseId);
        verify(courseCreationSaver).saveMemberCourse(memberId, draft, "https://s3/image/thumb.jpg", true);
        verify(courseDataService, never()).deleteUploadedFile(any());
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 회원이면 외부 API를 호출하지 않는다")
    void createMemberCourse_fail_memberNotFound() {
        // given
        Long nonExistentMemberId = 999L;
        when(memberRepository.existsById(nonExistentMemberId)).thenReturn(false);

        // when, then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> courseCreationService.createMemberCourse(nonExistentMemberId, request));
        assertThat(exception.getResponseCode()).isEqualTo(MEMBER_NOT_FOUND);

        verify(courseDataService, never()).prepareCourseFromGpx(any(), any());
        verify(fileService, never()).uploadFile(any(), any());
    }

    @Test
    @DisplayName("실패 - 이미 있는 코스 이름이면 외부 API를 호출하지 않는다")
    void createMemberCourse_fail_duplicateName() {
        // given
        Long memberId = 1L;
        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(courseRepository.existsByName(COURSE_NAME)).thenReturn(true);

        // when, then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> courseCreationService.createMemberCourse(memberId, request));
        assertThat(exception.getResponseCode()).isEqualTo(DUPLICATE_COURSE_NAME);

        verify(courseDataService, never()).prepareCourseFromGpx(any(), any());
        verify(fileService, never()).uploadFile(any(), any());
    }

    @Test
    @DisplayName("실패 - DB 저장이 실패하면 업로드한 GPX와 썸네일을 삭제한다")
    void createMemberCourse_fail_saveFailed_deletesUploadedFiles() {
        // given
        Long memberId = 1L;
        GpxCourseDraft draft = createDraft();

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(courseRepository.existsByName(COURSE_NAME)).thenReturn(false);
        when(courseDataService.prepareCourseFromGpx(any(GpxCourseRequestDto.class), eq(gpxFile))).thenReturn(draft);
        when(fileService.uploadFile(thumbnailImgFile, "image")).thenReturn("https://s3/image/thumb.jpg");
        when(courseCreationSaver.saveMemberCourse(memberId, draft, "https://s3/image/thumb.jpg", true))
                .thenThrow(new DataIntegrityViolationException("duplicate course name"));

        // when, then
        assertThrows(DataIntegrityViolationException.class, () -> courseCreationService.createMemberCourse(memberId, request));
        verify(courseDataService).deleteUploadedFile(draft.gpxPath());
        verify(courseDataService).deleteUploadedFile("https://s3/image/thumb.jpg");
    }

    private GpxCourseDraft createDraft() {
        return new GpxCourseDraft(COURSE_NAME, 5.0, 33, CourseLevel.MEDIUM, Area.HAEUN_GWANGAN,
                "https://s3/gpx/course.gpx", null, 60.0, 10.0, List.of(), List.of("설명"), List.of());
    }
}
