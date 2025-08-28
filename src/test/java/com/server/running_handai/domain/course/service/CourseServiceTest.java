package com.server.running_handai.domain.course.service;

import static com.server.running_handai.domain.course.entity.CourseFilter.*;
import static com.server.running_handai.domain.course.service.CourseService.MYSQL_POINT_FORMAT;
import static com.server.running_handai.global.response.ResponseCode.COURSE_NOT_FOUND;
import static com.server.running_handai.global.response.ResponseCode.DUPLICATE_COURSE_NAME;
import static com.server.running_handai.global.response.ResponseCode.MEMBER_NOT_FOUND;
import static com.server.running_handai.global.response.ResponseCode.NO_AUTHORITY_TO_DELETE_COURSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.running_handai.domain.bookmark.repository.BookmarkRepository;
import com.server.running_handai.domain.bookmark.dto.BookmarkCountDto;
import com.server.running_handai.domain.course.dto.CourseCreateRequestDto;
import com.server.running_handai.domain.course.dto.CourseDetailDto;
import com.server.running_handai.domain.course.dto.CourseFilterRequestDto;
import com.server.running_handai.domain.course.dto.CourseInfoDto;
import com.server.running_handai.domain.course.dto.CourseInfoWithDetailsDto;
import com.server.running_handai.domain.course.dto.CourseSummaryDto;
import com.server.running_handai.domain.course.dto.GpxCourseRequestDto;
import com.server.running_handai.domain.course.entity.Area;
import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.entity.CourseFilter;
import com.server.running_handai.domain.course.entity.CourseLevel;
import com.server.running_handai.domain.course.entity.RoadCondition;
import com.server.running_handai.domain.course.entity.Theme;
import com.server.running_handai.domain.course.entity.TrackPoint;
import com.server.running_handai.domain.course.event.CourseCreatedEvent;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.domain.course.repository.TrackPointRepository;
import com.server.running_handai.domain.member.entity.Member;
import com.server.running_handai.domain.member.entity.Provider;
import com.server.running_handai.domain.member.entity.Role;
import com.server.running_handai.domain.member.repository.MemberRepository;
import com.server.running_handai.domain.review.dto.ReviewInfoDto;
import com.server.running_handai.domain.review.entity.Review;
import com.server.running_handai.domain.review.repository.ReviewRepository;
import com.server.running_handai.domain.review.service.ReviewService;
import com.server.running_handai.domain.spot.dto.SpotInfoDto;
import com.server.running_handai.domain.spot.entity.Spot;
import com.server.running_handai.domain.spot.repository.SpotRepository;
import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.global.response.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @InjectMocks
    private CourseService courseService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private TrackPointRepository trackPointRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private ReviewService reviewService;

    @Mock
    private GeometryFactory geometryFactory;

    @Mock
    private CourseDataService courseDataService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private KakaoMapService kakaoMapService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private static final Long COURSE_ID = 1L;
    private static final Long MEMBER_ID = 1L;
    private static final Double USER_LAT = 37.5665;
    private static final Double USER_LON = -122.456;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(courseService, "distanceTolerance", 0.0001);
    }

    private static Stream<Arguments> filterOptionsProvider() {
        CourseFilterRequestDto nearbyFilter = new CourseFilterRequestDto(NEARBY, USER_LAT, USER_LON, null, null);
        CourseFilterRequestDto areaFilter = new CourseFilterRequestDto(AREA, USER_LAT, USER_LON, Area.HAEUN_GWANGAN, null);
        CourseFilterRequestDto themeFilter = new CourseFilterRequestDto(THEME, USER_LAT, USER_LON, null, Theme.MOUNTAIN);

        return Stream.of(
                Arguments.of(NEARBY, nearbyFilter),
                Arguments.of(AREA, areaFilter),
                Arguments.of(THEME, themeFilter)
        );
    }

    @ParameterizedTest
    @MethodSource("filterOptionsProvider")
    @DisplayName("코스 전체 조회 성공 - NEARBY, AREA, THEME 필터링 (회원)")
    void findCourses_success_forMember(CourseFilter filterType, CourseFilterRequestDto filterOption) {
        // given
        Course course = createMockCourse(COURSE_ID);
        CourseInfoDto courseInfoDto = createCourseInfoDto(course);
        List<TrackPoint> trackPoints = course.getTrackPoints();
        BookmarkCountDto bookmarkCountDto = new BookmarkCountDto(COURSE_ID, 3L); // 북마크 3개로 가정

        switch (filterType) {
            case NEARBY -> given(courseRepository.findCoursesNearbyUser(anyString())).willReturn(List.of(courseInfoDto));
            case AREA -> given(courseRepository.findCoursesByArea(anyString(), eq(Area.HAEUN_GWANGAN.name()))).willReturn(List.of(courseInfoDto));
            case THEME -> given(courseRepository.findCoursesByTheme(anyString(), eq(Theme.MOUNTAIN.name()))).willReturn(List.of(courseInfoDto));
        }

        Coordinate[] coordinates = course.getTrackPoints().stream()
                .map(p -> new Coordinate(p.getLon(), p.getLat(), p.getEle()))
                .toArray(Coordinate[]::new);
        LineString realLineString = new GeometryFactory().createLineString(coordinates);

        given(geometryFactory.createLineString(any(Coordinate[].class))).willReturn(realLineString);
        given(trackPointRepository.findByCourseIdInOrderBySequenceAsc(List.of(COURSE_ID))).willReturn(trackPoints);
        given(bookmarkRepository.countByCourseIdIn(List.of(COURSE_ID))).willReturn(List.of(bookmarkCountDto));
        given(bookmarkRepository.findBookmarkedCourseIdsByMember(List.of(COURSE_ID), MEMBER_ID)).willReturn(Set.of(COURSE_ID));

        // when
        List<CourseInfoWithDetailsDto> result = courseService.findCourses(filterOption, MEMBER_ID);

        // then
        assertNotNull(result);
        assertThat(result.size()).isEqualTo(1);
        CourseInfoWithDetailsDto details = result.getFirst();

        assertThat(details.courseId()).isEqualTo(COURSE_ID);
        assertThat(details.courseName()).isEqualTo(course.getName());
        assertThat(details.distance()).isEqualTo((int) course.getDistance());
        assertThat(details.duration()).isEqualTo(course.getDuration());
        assertThat(details.maxElevation()).isEqualTo((int) course.getMaxElevation().doubleValue());
        assertThat(details.thumbnailUrl()).isEqualTo("thumbnailUrl");
        assertThat(details.distanceFromUser()).isEqualTo(1.5);
        assertThat(details.trackPoints().size()).isLessThanOrEqualTo(trackPoints.size());
        assertThat(details.bookmarks()).isEqualTo(3);
        assertThat(details.isBookmarked()).isTrue();

        switch (filterType) {
            case NEARBY -> verify(courseRepository).findCoursesNearbyUser(anyString());
            case AREA -> verify(courseRepository).findCoursesByArea(anyString(), eq(Area.HAEUN_GWANGAN.name()));
            case THEME -> verify(courseRepository).findCoursesByTheme(anyString(), eq(Theme.MOUNTAIN.name()));
        }

        verify(trackPointRepository).findByCourseIdInOrderBySequenceAsc(anyList());
        verify(bookmarkRepository).countByCourseIdIn(anyList());
        verify(bookmarkRepository).findBookmarkedCourseIdsByMember(anyList(), anyLong());
    }

    @ParameterizedTest
    @MethodSource("filterOptionsProvider")
    @DisplayName("코스 전체 조회 성공 - NEARBY, AREA, THEME 필터링 (비회원)")
    void findCourses_success_forGuest(CourseFilter filterType, CourseFilterRequestDto filterOption) {
        // given
        Course course = createMockCourse(COURSE_ID);
        CourseInfoDto courseInfoDto = createCourseInfoDto(course);
        List<TrackPoint> trackPoints = course.getTrackPoints();
        BookmarkCountDto bookmarkCountDto = new BookmarkCountDto(COURSE_ID, 3L); // 북마크 3개로 가정

        switch (filterType) {
            case NEARBY -> given(courseRepository.findCoursesNearbyUser(anyString())).willReturn(List.of(courseInfoDto));
            case AREA -> given(courseRepository.findCoursesByArea(anyString(), eq(Area.HAEUN_GWANGAN.name()))).willReturn(List.of(courseInfoDto));
            case THEME -> given(courseRepository.findCoursesByTheme(anyString(), eq(Theme.MOUNTAIN.name()))).willReturn(List.of(courseInfoDto));
        }

        Coordinate[] coordinates = course.getTrackPoints().stream()
                .map(p -> new Coordinate(p.getLon(), p.getLat(), p.getEle()))
                .toArray(Coordinate[]::new);
        LineString realLineString = new GeometryFactory().createLineString(coordinates);

        given(geometryFactory.createLineString(any(Coordinate[].class))).willReturn(realLineString);
        given(trackPointRepository.findByCourseIdInOrderBySequenceAsc(List.of(COURSE_ID))).willReturn(trackPoints);
        given(bookmarkRepository.countByCourseIdIn(List.of(COURSE_ID))).willReturn(List.of(bookmarkCountDto));

        // when
        List<CourseInfoWithDetailsDto> result = courseService.findCourses(filterOption, null);

        // then
        assertNotNull(result);
        assertThat(result.size()).isEqualTo(1);
        CourseInfoWithDetailsDto details = result.getFirst();

        assertThat(details.courseId()).isEqualTo(COURSE_ID);
        assertThat(details.courseName()).isEqualTo(course.getName());
        assertThat(details.distance()).isEqualTo((int) course.getDistance());
        assertThat(details.duration()).isEqualTo(course.getDuration());
        assertThat(details.maxElevation()).isEqualTo((int) course.getMaxElevation().doubleValue());
        assertThat(details.thumbnailUrl()).isEqualTo("thumbnailUrl");
        assertThat(details.distanceFromUser()).isEqualTo(1.5);
        assertThat(details.trackPoints().size()).isLessThanOrEqualTo(trackPoints.size());
        assertThat(details.bookmarks()).isEqualTo(3);
        assertThat(details.isBookmarked()).isFalse();

        switch (filterType) {
            case NEARBY -> verify(courseRepository).findCoursesNearbyUser(anyString());
            case AREA -> verify(courseRepository).findCoursesByArea(anyString(), eq(Area.HAEUN_GWANGAN.name()));
            case THEME -> verify(courseRepository).findCoursesByTheme(anyString(), eq(Theme.MOUNTAIN.name()));
        }

        verify(trackPointRepository).findByCourseIdInOrderBySequenceAsc(anyList());
        verify(bookmarkRepository).countByCourseIdIn(anyList());
        verify(bookmarkRepository, never()).findBookmarkedCourseIdsByMember(anyList(), anyLong());
    }

    @Test
    @DisplayName("코스 전체 조회 성공 - 조회된 코스가 없는 경우")
    void findCourses_success_emptyResult() {
        // given
        CourseFilterRequestDto filterOption = new CourseFilterRequestDto(NEARBY, USER_LAT, USER_LON, null, null);
        given(courseRepository.findCoursesNearbyUser(String.format(MYSQL_POINT_FORMAT, filterOption.lat(), filterOption.lon()))).willReturn(List.of());

        // when
        List<CourseInfoWithDetailsDto> result = courseService.findCourses(filterOption, null);

        // then
        assertThat(result).isEmpty();
        verify(trackPointRepository, never()).findByCourseIdInOrderBySequenceAsc(anyList());
        verify(bookmarkRepository, never()).countByCourseIdIn(anyList());
    }

    @Test
    @DisplayName("코스 전체 조회 실패 - 지역 필터링인데 Area가 null")
    void findCourses_byArea_fail_withNullArea() {
        // given
        CourseFilterRequestDto filterOption = new CourseFilterRequestDto(AREA, USER_LAT, USER_LON, null, null);

        // when, then
        BusinessException exception = assertThrows(BusinessException.class, () -> courseService.findCourses(filterOption, MEMBER_ID));
        assertThat(exception.getResponseCode()).isEqualTo(ResponseCode.INVALID_AREA_PARAMETER);
    }

    @Test
    @DisplayName("코스 전체 조회 실패 - 테마 필터링인데 Theme이 null")
    void findCourses_byTheme_fail_withNullTheme() {
        // given
        CourseFilterRequestDto filterOption = new CourseFilterRequestDto(THEME, USER_LAT, USER_LON, null, null);

        // when, then
        BusinessException exception = assertThrows(BusinessException.class, () -> courseService.findCourses(filterOption, MEMBER_ID));
        assertThat(exception.getResponseCode()).isEqualTo(ResponseCode.INVALID_THEME_PARAMETER);
    }

    @Test
    @DisplayName("코스 상세 조회 성공 - 회원")
    void findCourseDetails_success_forMember() {
        // given
        Course course = createMockCourse(COURSE_ID);

        Coordinate[] coordinates = course.getTrackPoints().stream()
                .map(p -> new Coordinate(p.getLon(), p.getLat(), p.getEle()))
                .toArray(Coordinate[]::new);
        LineString realLineString = new GeometryFactory().createLineString(coordinates);

        given(geometryFactory.createLineString(any(Coordinate[].class))).willReturn(realLineString);
        given(courseRepository.findCourseWithDetailsById(COURSE_ID)).willReturn(Optional.of(course));
        given(bookmarkRepository.countByCourseId(COURSE_ID)).willReturn(5); // 북마크 수 5개
        given(bookmarkRepository.existsByCourseIdAndMemberId(COURSE_ID, MEMBER_ID)).willReturn(true); // 북마크 여부 true

        // when
        CourseDetailDto result = courseService.findCourseDetails(COURSE_ID, MEMBER_ID);

        // then
        assertNotNull(result);
        assertThat(result.courseId()).isEqualTo(COURSE_ID);
        assertThat(result.courseName()).isEqualTo(course.getName());
        assertThat(result.distance()).isEqualTo((int) course.getDistance());
        assertThat(result.duration()).isEqualTo(course.getDuration());
        assertThat(result.minElevation()).isEqualTo((int) course.getMinElevation().doubleValue());
        assertThat(result.maxElevation()).isEqualTo((int) course.getMaxElevation().doubleValue());
        assertThat(result.trackPoints()).isNotEmpty();
        assertThat(result.trackPoints().size()).isLessThanOrEqualTo(course.getTrackPoints().size());
        assertThat(result.bookmarks()).isEqualTo(5);
        assertThat(result.isBookmarked()).isTrue();

        verify(courseRepository).findCourseWithDetailsById(COURSE_ID);
        verify(bookmarkRepository).countByCourseId(COURSE_ID);
        verify(bookmarkRepository).existsByCourseIdAndMemberId(COURSE_ID, MEMBER_ID);
        verify(geometryFactory).createLineString(any(Coordinate[].class));
    }

    @Test
    @DisplayName("코스 상세 조회 성공 - 비회원")
    void findCourseDetails_success_forGuest() {
        // given
        Course course = createMockCourse(COURSE_ID);

        Coordinate[] coordinates = course.getTrackPoints().stream()
                .map(p -> new Coordinate(p.getLon(), p.getLat(), p.getEle()))
                .toArray(Coordinate[]::new);
        LineString realLineString = new GeometryFactory().createLineString(coordinates);

        given(geometryFactory.createLineString(any(Coordinate[].class))).willReturn(realLineString);
        given(courseRepository.findCourseWithDetailsById(COURSE_ID)).willReturn(Optional.of(course));
        given(bookmarkRepository.countByCourseId(COURSE_ID)).willReturn(5); // 북마크 수 5개

        // when
        CourseDetailDto result = courseService.findCourseDetails(COURSE_ID, null);

        // then
        assertNotNull(result);
        assertThat(result.courseId()).isEqualTo(COURSE_ID);
        assertThat(result.courseName()).isEqualTo(course.getName());
        assertThat(result.distance()).isEqualTo((int) course.getDistance());
        assertThat(result.duration()).isEqualTo(course.getDuration());
        assertThat(result.minElevation()).isEqualTo((int)course.getMinElevation().doubleValue());
        assertThat(result.maxElevation()).isEqualTo((int)course.getMaxElevation().doubleValue());
        assertThat(result.trackPoints()).isNotEmpty();
        assertThat(result.trackPoints().size()).isLessThanOrEqualTo(course.getTrackPoints().size());
        assertThat(result.bookmarks()).isEqualTo(5);
        assertThat(result.isBookmarked()).isFalse();

        verify(courseRepository).findCourseWithDetailsById(COURSE_ID);
        verify(bookmarkRepository).countByCourseId(COURSE_ID);
        verify(geometryFactory).createLineString(any(Coordinate[].class));
    }

    @Test
    @DisplayName("코스 상세 조회 실패 - 존재하지 않는 코스")
    void findCourseDetails_fail_courseNotFound() {
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
     */
    private Course createMockCourse(Long courseId) {
        // Course 객체 생성 (빌더로 설정 가능한 필드 우선 설정)
        Course course = Course.builder()
                .name("courseName1")
                .distance(15.3)
                .duration(120)
                .minElevation(30.4)
                .maxElevation(150.5)
                .level(CourseLevel.MEDIUM)
                .build();

        // 연관관계 필드(컬렉션)를 위한 더미 데이터 생성
        List<RoadCondition> roadConditions = createDummyRoadConditions();
        List<TrackPoint> trackPoints = createDummyTrackPoints();

        // Reflection을 사용하여 id 및 연관관계 필드(컬렉션) 설정
        ReflectionTestUtils.setField(course, "id", courseId);
        ReflectionTestUtils.setField(course, "roadConditions", roadConditions);
        ReflectionTestUtils.setField(course, "trackPoints", trackPoints);

        trackPoints.forEach(trackPoint ->
                ReflectionTestUtils.setField(trackPoint, "course", course)
        );

        return course;
    }

    private List<RoadCondition> createDummyRoadConditions() {
        return List.of(
                RoadCondition.builder().description("roadCondition1").build(),
                RoadCondition.builder().description("roadCondition2").build(),
                RoadCondition.builder().description("roadCondition3").build(),
                RoadCondition.builder().description("roadCondition4").build(),
                RoadCondition.builder().description("roadCondition5").build()
        );
    }

    private List<TrackPoint> createDummyTrackPoints() {
        return List.of(
                TrackPoint.builder().lat(35.08).lon(129.01).ele(16.77).build(),
                TrackPoint.builder().lat(35.09).lon(129.02).ele(16.78).build(),
                TrackPoint.builder().lat(35.10).lon(129.03).ele(16.79).build(),
                TrackPoint.builder().lat(35.11).lon(129.04).ele(16.80).build(),
                TrackPoint.builder().lat(35.12).lon(129.05).ele(16.81).build(),
                TrackPoint.builder().lat(35.13).lon(129.06).ele(16.82).build(),
                TrackPoint.builder().lat(35.14).lon(129.07).ele(16.83).build(),
                TrackPoint.builder().lat(35.15).lon(129.08).ele(16.84).build(),
                TrackPoint.builder().lat(35.16).lon(129.09).ele(16.85).build(),
                TrackPoint.builder().lat(35.17).lon(129.10).ele(16.86).build()
        );
    }

    private CourseInfoDto createCourseInfoDto(Course course) {
        CourseInfoDto courseInfoDto = Mockito.mock(CourseInfoDto.class);
        given(courseInfoDto.getId()).willReturn(course.getId());
        given(courseInfoDto.getName()).willReturn(course.getName());
        given(courseInfoDto.getThumbnailUrl()).willReturn("thumbnailUrl");
        given(courseInfoDto.getDistance()).willReturn(course.getDistance());
        given(courseInfoDto.getDuration()).willReturn(course.getDuration());
        given(courseInfoDto.getMaxElevation()).willReturn(course.getMaxElevation());
        given(courseInfoDto.getDistanceFromUser()).willReturn(1.5);
        return courseInfoDto;
    }

    @Nested
    @DisplayName("코스 요약 조회 테스트")
    class CourseSummaryTest {

        private Member writer;

        @BeforeEach
        void setUp() {
            writer = Member.builder()
                    .nickname("nickname1")
                    .providerId("providerId1")
                    .provider(Provider.GOOGLE)
                    .email("email1")
                    .role(Role.USER)
                    .build();
        }

        private Review createMockReview(Long reviewId, double stars, String contents) {
            Review review = Review.builder().stars(stars).contents(contents).build();
            ReflectionTestUtils.setField(review, "id", reviewId);
            ReflectionTestUtils.setField(review, "createdAt", LocalDateTime.now());
            review.setWriter(writer);
            return review;
        }

        private Spot createMockSpot(Long spotId) {
            Spot spot = Spot.builder().build();
            ReflectionTestUtils.setField(spot, "id", spotId);
            return spot;
        }

        private static Stream<Arguments> memberAndGuestCases() {
            return Stream.of(
                    Arguments.of(1L, true), // 회원이면 memberId=1L, isMyReview=true
                    Arguments.of(null, false) // 게스트이면 memberId=null, isMyReview=false
            );
        }

        @ParameterizedTest
        @DisplayName("코스 요약 조회 성공")
        @MethodSource("memberAndGuestCases")
        void getCourseSummary_success(Long memberId, boolean isMyReview) {
            // given
            Long courseId = 1L;

            Course course = createMockCourse(courseId);
            Review review1 = createMockReview(1L, 4.0, "review1");
            Review review2 = createMockReview(2L, 5.0, "review2");
            List<Review> reviews = List.of(review1, review2);

            List<ReviewInfoDto> reviewInfoDtos = List.of(
                    ReviewInfoDto.from(review1, isMyReview),
                    ReviewInfoDto.from(review2, isMyReview)
            );

            Spot spot1 = createMockSpot(101L);
            Spot spot2 = createMockSpot(102L);
            Spot spot3 = createMockSpot(103L);

            List<SpotInfoDto> spotInfoDtos = List.of(
                    new SpotInfoDto(101L, "Spot1", "Description1", "http://mock-image-url"),
                    new SpotInfoDto(102L, "Spot2", "Description2", "http://mock-image-url"),
                    new SpotInfoDto(103L, "Spot3", "Description3", "http://mock-image-url")
            );

            given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
            given(reviewRepository.findRecent2ByCourseId(courseId)).willReturn(reviews);
            given(reviewRepository.countByCourseId(courseId)).willReturn(3L);
            given(reviewService.calculateAverageStars(courseId)).willReturn(4.2);
            given(reviewService.convertToReviewInfoDtos(reviews, memberId)).willReturn(reviewInfoDtos);
            given(spotRepository.findRandom3ByCourseId(courseId)).willReturn(spotInfoDtos);

            // when
            CourseSummaryDto result = courseService.getCourseSummary(courseId, memberId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.distance()).isEqualTo((int) course.getDistance());
            assertThat(result.duration()).isEqualTo(course.getDuration());
            assertThat(result.maxElevation()).isEqualTo((int) course.getMaxElevation().doubleValue());
            assertThat(result.starAverage()).isEqualTo(4.2);
            assertThat(result.reviewCount()).isEqualTo(3L);
            assertThat(result.reviews().getFirst().reviewId()).isEqualTo(review1.getId());
            assertThat(result.reviews().getLast().reviewId()).isEqualTo(review2.getId());
            assertThat(result.spots().size()).isEqualTo(3);
            assertThat(result.spots().get(0).spotId()).isEqualTo(spot1.getId());
            assertThat(result.spots().get(1).spotId()).isEqualTo(spot2.getId());
            assertThat(result.spots().get(2).spotId()).isEqualTo(spot3.getId());

            verify(courseRepository).findById(courseId);
            verify(reviewRepository).findRecent2ByCourseId(courseId);
            verify(reviewRepository).countByCourseId(courseId);
            verify(reviewService).calculateAverageStars(courseId);
            verify(reviewService).convertToReviewInfoDtos(reviews, memberId);
            verify(spotRepository).findRandom3ByCourseId(courseId);
        }

        @Test
        @DisplayName("코스 요약 조회 실패 - 존재하지 않는 코스")
        void getCourseSummary_fail_courseNotFound() {
            // given
            Long memberId = 1L;
            Long courseId = 999L; // 존재하지 않는 코스

            // when, then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> courseService.getCourseSummary(courseId, memberId));
            assertThat(exception.getResponseCode()).isEqualTo(COURSE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("지역 판별 테스트")
    class RegionCheckTest {
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Test
        @DisplayName("부산 지역 판별 성공 - true 반환")
        void isInsideBusan_whenCoordinateIsInBusan_shouldReturnTrue() throws Exception {
            // given
            double busanLon = 129.004480714;
            double busanLat = 35.08747067199999;
            JsonNode busanAddressNode = createMockAddressNode("부산광역시");

            when(kakaoMapService.getAddressFromCoordinate(busanLon, busanLat)).thenReturn(busanAddressNode);

            // when
            boolean result = courseService.isInsideBusan(busanLon, busanLat);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("부산이 아닌 지역 판별 성공 - false 반환")
        void isInsideBusan_whenCoordinateIsNotInBusan_shouldReturnFalse() throws Exception {
            // given
            double seoulLon = 127.0276;
            double seoulLat = 37.4979;
            JsonNode seoulAddressNode = createMockAddressNode("서울특별시");

            when(kakaoMapService.getAddressFromCoordinate(seoulLon, seoulLat)).thenReturn(seoulAddressNode);

            // when
            boolean result = courseService.isInsideBusan(seoulLon, seoulLat);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("카카오 API에서 주소 정보를 반환하지 않은 경우(null) false 반환")
        void isInsideBusan_whenAddressNodeIsNull_shouldReturnFalse() {
            // given
            double someLon = 128.0;
            double someLat = 36.0;

            when(kakaoMapService.getAddressFromCoordinate(someLon, someLat)).thenReturn(null);

            // when
            boolean result = courseService.isInsideBusan(someLon, someLat);

            // then
            assertThat(result).isFalse();
        }

        private JsonNode createMockAddressNode(String cityName) throws Exception {
            String jsonString = String.format(
                    "{\"address\": {\"region_1depth_name\": \"%s\"}}",
                    cityName
            );
            return objectMapper.readTree(jsonString);
        }
    }

    @Nested
    @DisplayName("내 코스 생성 테스트")
    class MyCourseCreationTest {

        private final String START_POINT_NAME = "광안리해수욕장";
        private final String END_POINT_NAME = "해운대해수욕장";
        private final String COURSE_NAME = START_POINT_NAME + "-" + END_POINT_NAME;

        private Member member;
        private MultipartFile gpxFile;
        private MultipartFile thumbnailImgFile;
        private CourseCreateRequestDto request;

        @BeforeEach
        void setUp() {
            member = Member.builder()
                    .nickname("nickname1")
                    .providerId("providerId1")
                    .provider(Provider.GOOGLE)
                    .email("email1")
                    .role(Role.USER)
                    .build();
            gpxFile = new MockMultipartFile("gpxFile", "test.gpx", "application/gpx+xml", "<gpx></gpx>".getBytes());
            thumbnailImgFile = new MockMultipartFile("thumbnail", "thumb.jpg", "image/jpeg", "thumbnail-image".getBytes());
            request = new CourseCreateRequestDto(START_POINT_NAME, END_POINT_NAME, gpxFile, thumbnailImgFile, true);
        }

        @Test
        @DisplayName("내 코스 생성 성공")
        void createMemberCourse_success() {
            // given
            Long memberId = 1L;
            Long courseId = 100L;
            Course newCourse = createMockCourse(courseId);

            when(courseRepository.existsByName(COURSE_NAME)).thenReturn(false); // 중복된 이름 없음
            when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
            when(courseDataService.createCourseToGpx(any(GpxCourseRequestDto.class), any(MultipartFile.class))).thenReturn(newCourse);

            // when
            Long result = courseService.createMemberCourse(memberId, request);

            // then
            assertThat(result).isEqualTo(newCourse.getId());
            assertThat(newCourse.getCreator()).isEqualTo(member);
            assertThat(member.getCourses()).contains(newCourse);

            // 이벤트 캡처
            ArgumentCaptor<CourseCreatedEvent> eventCaptor = ArgumentCaptor.forClass(CourseCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            // 캡처한 이벤트의 내용 검증
            CourseCreatedEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.courseId()).isEqualTo(newCourse.getId());
            assertThat(capturedEvent.isInsideBusan()).isTrue();

            verify(courseRepository).existsByName(COURSE_NAME);
            verify(memberRepository).findById(memberId);
            verify(courseDataService).createCourseToGpx(any(GpxCourseRequestDto.class), eq(gpxFile));
            verify(courseDataService).updateCourseImage(newCourse.getId(), thumbnailImgFile);
        }

        @Test
        @DisplayName("실패 - 중복된 코스 이름")
        void createMemberCourse_fail_duplicateCourseName() {
            // given
            Long memberId = 1L;
            when(courseRepository.existsByName(COURSE_NAME)).thenReturn(true); // 코스 이름이 이미 존재함

            // when, then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> courseService.createMemberCourse(memberId, request));
            assertThat(exception.getResponseCode()).isEqualTo(DUPLICATE_COURSE_NAME);

            verify(memberRepository, never()).findById(anyLong());
            verify(courseDataService, never()).createCourseToGpx(any(), any());
            verify(courseDataService, never()).updateCourseImage(any(), any());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회원")
        void createMemberCourse_fail_memberNotFound() {
            // given
            Long nonExistentMemberId = 999L;
            when(courseRepository.existsByName(COURSE_NAME)).thenReturn(false); // 중복은 통과
            when(memberRepository.findById(nonExistentMemberId)).thenReturn(Optional.empty()); // 존재하지 않는 회원

            // when, then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> courseService.createMemberCourse(nonExistentMemberId, request));
            assertThat(exception.getResponseCode()).isEqualTo(MEMBER_NOT_FOUND);

            verify(courseDataService, never()).createCourseToGpx(any(), any());
            verify(courseDataService, never()).updateCourseImage(any(), any());
        }
    }
}