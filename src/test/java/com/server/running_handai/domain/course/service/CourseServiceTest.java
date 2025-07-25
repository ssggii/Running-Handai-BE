package com.server.running_handai.domain.course.service;

import static com.server.running_handai.global.response.ResponseCode.COURSE_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

import com.server.running_handai.domain.bookmark.repository.BookmarkRepository;
import com.server.running_handai.domain.course.dto.CourseDetailDto;
import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.entity.CourseLevel;
import com.server.running_handai.domain.course.entity.RoadCondition;
import com.server.running_handai.domain.course.entity.TrackPoint;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.global.response.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @InjectMocks
    private CourseService courseService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp() {
        // @Value 필드 값을 수동으로 주입
        ReflectionTestUtils.setField(courseService, "distanceTolerance", 0.0001);
    }

    @Test
    @DisplayName("코스 상세 조회 성공 - 회원")
    void findCourseDetails_success_forMember() {
        // given
        Long courseId = 1L;
        Long memberId = 1L;
        Course course = createMockCourse(courseId);

        Coordinate[] coordinates = course.getTrackPoints().stream()
                .map(p -> new Coordinate(p.getLon(), p.getLat(), p.getEle()))
                .toArray(Coordinate[]::new);
        LineString realLineString = new GeometryFactory().createLineString(coordinates);

        given(courseRepository.findCourseWithDetailsById(courseId)).willReturn(Optional.of(course));
        given(bookmarkRepository.countByCourseId(courseId)).willReturn(5); // 북마크 수 5개
        given(bookmarkRepository.existsByCourseIdAndMemberId(courseId, memberId)).willReturn(true); // 북마크 여부 true

        // courseService 내부의 mock 'geometryFactory'가
        // createLineString을 호출하면realLineString을 반환하도록 설정
        given(geometryFactory.createLineString(any(Coordinate[].class))).willReturn(realLineString);

        // when
        CourseDetailDto result = courseService.findCourseDetails(courseId, memberId);

        // then
        assertNotNull(result);
        assertThat(result.courseId()).isEqualTo(courseId);
        assertThat(result.trackPoints()).isNotEmpty();
        assertThat(result.trackPoints().size()).isLessThanOrEqualTo(course.getTrackPoints().size());
        assertThat(result.bookmarks()).isEqualTo(5);
        assertThat(result.isBookmarked()).isTrue();

        verify(courseRepository).findCourseWithDetailsById(courseId);
        verify(bookmarkRepository).countByCourseId(courseId);
        verify(bookmarkRepository).existsByCourseIdAndMemberId(courseId, memberId);
        verify(geometryFactory).createLineString(any(Coordinate[].class));
    }

    @Test
    @DisplayName("코스 상세 조회 성공 - 비회원")
    void findCourseDetails_success_forGuest() {
        // given
        Long courseId = 1L;
        Course course = createMockCourse(courseId);

        Coordinate[] coordinates = course.getTrackPoints().stream()
                .map(p -> new Coordinate(p.getLon(), p.getLat(), p.getEle()))
                .toArray(Coordinate[]::new);
        LineString realLineString = new GeometryFactory().createLineString(coordinates);

        given(courseRepository.findCourseWithDetailsById(courseId)).willReturn(Optional.of(course));
        given(bookmarkRepository.countByCourseId(courseId)).willReturn(5); // 북마크 수 5개

        // courseService 내부의 mock 'geometryFactory'가
        // createLineString을 호출하면realLineString을 반환하도록 설정
        given(geometryFactory.createLineString(any(Coordinate[].class))).willReturn(realLineString);

        // when
        CourseDetailDto result = courseService.findCourseDetails(courseId, null);

        // then
        assertNotNull(result);
        assertThat(result.courseId()).isEqualTo(courseId);
        assertThat(result.trackPoints()).isNotEmpty();
        assertThat(result.trackPoints().size()).isLessThanOrEqualTo(course.getTrackPoints().size());
        assertThat(result.bookmarks()).isEqualTo(5);
        assertThat(result.isBookmarked()).isFalse();

        verify(courseRepository).findCourseWithDetailsById(courseId);
        verify(bookmarkRepository).countByCourseId(courseId);
        verify(geometryFactory).createLineString(any(Coordinate[].class));
    }

    @Test
    @DisplayName("코스 상세 조회 실패 - 존재하지 않는 코스")
    void findCourseDetails_Fail_CourseNotFound() {
        // given
        Long nonExistentCourseId = 999L;
        given(courseRepository.findCourseWithDetailsById(nonExistentCourseId)).willReturn(Optional.empty());

        // when, then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> courseService.findCourseDetails(nonExistentCourseId, null));

        assertThat(exception.getResponseCode()).isEqualTo(COURSE_NOT_FOUND);
    }

    /**
     * 테스트용 course 데이터 생성을 위한 헬퍼 메서드
     * TrackPoint, RoadCondition 등 필요한 테스트용 객체를 생성하여 Course에 설정
     * @param courseId
     * @return
     */
    private Course createMockCourse(Long courseId) {
        // Course 객체 생성 (빌더로 설정 가능한 필드 우선 설정)
        Course course = Course.builder()
                .distance(15)
                .duration(120)
                .minElevation(30.0)
                .maxElevation(150.0)
                .level(CourseLevel.MEDIUM)
                .build();

        // 연관관계 필드(컬렉션)를 위한 더미 데이터 생성
        List<RoadCondition> roadConditions = List.of(
                RoadCondition.builder().course(course).description("roadCondition1").build(),
                RoadCondition.builder().course(course).description("roadCondition2").build(),
                RoadCondition.builder().course(course).description("roadCondition3").build()
        );

        List<TrackPoint> trackPoints = List.of(
                TrackPoint.builder().lat(35.08).lon(129.01).ele(16.77).build(),
                TrackPoint.builder().lat(35.09).lon(129.02).ele(16.78).build(),
                TrackPoint.builder().lat(35.10).lon(129.03).ele(16.79).build(),
                TrackPoint.builder().lat(35.11).lon(129.04).ele(16.80).build(),
                TrackPoint.builder().lat(35.12).lon(129.05).ele(16.81).build(),
                TrackPoint.builder().lat(35.13).lon(129.06).ele(16.82).build()
        );

        // Reflection을 사용하여 id 및 연관관계 필드(컬렉션) 설정
        ReflectionTestUtils.setField(course, "id", courseId);
        ReflectionTestUtils.setField(course, "roadConditions", roadConditions);
        ReflectionTestUtils.setField(course, "trackPoints", trackPoints);

        return course;
    }

}