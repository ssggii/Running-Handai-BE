package com.server.running_handai.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.server.running_handai.domain.course.entity.Area;
import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.entity.CourseLevel;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.domain.member.entity.Member;
import com.server.running_handai.domain.member.entity.Provider;
import com.server.running_handai.domain.member.entity.Role;
import com.server.running_handai.domain.review.dto.ReviewInfoDto;
import com.server.running_handai.domain.review.dto.ReviewInfoListDto;
import com.server.running_handai.domain.review.dto.ReviewRequestDto;
import com.server.running_handai.domain.review.entity.Review;
import com.server.running_handai.domain.review.repository.ReviewRepository;
import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.global.response.exception.BusinessException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @InjectMocks
    private ReviewService reviewService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private CourseRepository courseRepository;

    private static final String POINT = "POINT(127.027621 37.497928)";
    private static final double VALID_REVIEW_STARS = 4.5;
    private static final String VALID_REVIEW_CONTENTS = "정말 좋은 코스였습니다!";

    private Member member;
    private Course course;
    private Review review;

    private static Point creatPoint() throws ParseException {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        WKTReader wktReader = new WKTReader(geometryFactory);
        return (Point) wktReader.read(POINT);
    }

    @BeforeEach
    void setUp() throws ParseException {
        // 각 테스트 실행 전에 공통 객체 초기화
        member = Member.builder()
                .nickname("nickname1")
                .email("member1@email.com")
                .role(Role.USER)
                .provider(Provider.GOOGLE)
                .providerId("member1_providerId")
                .build();

        course = Course.builder()
                .name("courseName1")
                .distance(10)
                .duration(120)
                .level(CourseLevel.MEDIUM)
                .area(Area.HAEUN_GWANGAN)
                .gpxPath("gpxPath1")
                .maxElevation(128.99)
                .minElevation(-0.9)
                .startPoint(creatPoint())
                .build();

        review = Review.builder()
                .stars(VALID_REVIEW_STARS)
                .contents(VALID_REVIEW_CONTENTS)
                .build();

        // 테스트 리뷰에 연관관계 설정
        review.setWriter(member);
        review.setCourse(course);

        ReflectionTestUtils.setField(review, "id", 100L);
    }

    @Nested
    @DisplayName("리뷰 생성 테스트")
    class CreateReviewTest {

        @Test
        @DisplayName("리뷰 등록 성공 - 유효한 요청 시 리뷰를 성공적으로 생성한다.")
        void createReview_success() {
            // given
            ReviewRequestDto reviewRequest = new ReviewRequestDto(VALID_REVIEW_STARS, VALID_REVIEW_CONTENTS);
            Long courseId = course.getId();

            given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
            given(reviewRepository.save(any(Review.class))).willReturn(review);

            // when
            ReviewInfoDto result = reviewService.createReview(courseId, reviewRequest, member);

            // then
            assertThat(result).isNotNull();
            assertThat(result.reviewId()).isEqualTo(review.getId());
            assertThat(result.stars()).isEqualTo(reviewRequest.stars());
            assertThat(result.contents()).isEqualTo(reviewRequest.contents());
            assertThat(result.writerNickname()).isEqualTo(member.getNickname());

            verify(courseRepository).findById(courseId);
            verify(reviewRepository).save(any(Review.class));
        }

        @Test
        @DisplayName("리뷰 등록 실패 - 0.5점 단위가 아닌 별점")
        void createReview_fail_invalidStars() {
            // given
            ReviewRequestDto requestWithInvalidStars = new ReviewRequestDto(4.2, VALID_REVIEW_CONTENTS);
            Long courseId = course.getId();

            // when, then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> reviewService.createReview(courseId, requestWithInvalidStars, member));

            assertThat(exception.getResponseCode()).isEqualTo(ResponseCode.INVALID_REVIEW_STARS);
        }

        @Test
        @DisplayName("리뷰 등록 실패 - 존재하지 않는 코스")
        void createReview_fail_courseNotFound() {
            // given
            ReviewRequestDto request = new ReviewRequestDto(VALID_REVIEW_STARS, VALID_REVIEW_CONTENTS);
            Long nonExistentCourseId = 999L;

            given(courseRepository.findById(nonExistentCourseId)).willReturn(Optional.empty());

            // when, then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> reviewService.createReview(nonExistentCourseId, request, member));

            assertThat(exception.getResponseCode()).isEqualTo(ResponseCode.COURSE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("리뷰 요청 DTO 유효성 검사 테스트")
    class ReviewRequestDtoValidationTest {

        private static Validator validator;

        @BeforeAll
        static void setUp() {
            // 테스트 시작 전 Validator를 한 번만 생성
            try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
                validator = factory.getValidator();
            }
        }

        @Test
        @DisplayName("리뷰 요청 DTO 유효성 검증 실패 - 별점이 null")
        void failWhenStarsIsNull() {
            // given
            ReviewRequestDto request = new ReviewRequestDto(null, VALID_REVIEW_CONTENTS);

            // when
            Set<ConstraintViolation<ReviewRequestDto>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations.iterator().next().getMessage()).isEqualTo("별점은 필수 항목입니다.");
        }

        @Test
        @DisplayName("리뷰 요청 DTO 유효성 검증 실패 - 별점이 0.5 미만")
        void failWhenStarsIsTooLow() {
            // given
            ReviewRequestDto request = new ReviewRequestDto(0.4, VALID_REVIEW_CONTENTS);

            // when
            Set<ConstraintViolation<ReviewRequestDto>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations.iterator().next().getMessage()).isEqualTo("별점은 0.5 이상이어야 합니다.");
        }

        @Test
        @DisplayName("리뷰 요청 DTO 유효성 검증 실패 - 별점이 5.0 초과")
        void failWhenStarsIsTooHigh() {
            // given
            ReviewRequestDto request = new ReviewRequestDto(5.1, VALID_REVIEW_CONTENTS);

            // when
            Set<ConstraintViolation<ReviewRequestDto>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations.iterator().next().getMessage()).isEqualTo("별점은 5.0 이하이어야 합니다.");
        }

        @Test
        @DisplayName("리뷰 요청 DTO 유효성 검증 실패 - 리뷰 내용이 blank")
        void failWhenContentsIsBlank() {
            // given
            ReviewRequestDto request = new ReviewRequestDto(VALID_REVIEW_STARS, " ");

            // when
            Set<ConstraintViolation<ReviewRequestDto>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations.iterator().next().getMessage()).isEqualTo("리뷰 내용은 필수 항목입니다.");
        }

        @Test
        @DisplayName("리뷰 요청 DTO 유효성 검증 실패 - 리뷰 내용이 2000자 초과")
        void failWhenContentsIsTooLong() {
            // given
            String longContents = "a".repeat(2001);
            ReviewRequestDto request = new ReviewRequestDto(VALID_REVIEW_STARS, longContents);

            // when
            Set<ConstraintViolation<ReviewRequestDto>> violations = validator.validate(request);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations.iterator().next().getMessage()).isEqualTo("리뷰 내용은 최대 2000자까지 작성할 수 있습니다.");
        }
    }

    @Nested
    @DisplayName("리뷰 조회 테스트")
    class FindAllReviewsByCourseTest {

        @Test
        @DisplayName("리뷰 조회 성공")
        void findAllReviews_success_withReviews() {
            // given
            Long courseId = course.getId();
            
            Review review1 = Review.builder().stars(VALID_REVIEW_STARS).contents(VALID_REVIEW_CONTENTS).build();
            ReflectionTestUtils.setField(review1, "id", 101L);
            review1.setWriter(member);
            review1.setCourse(course);

            Review review2 = Review.builder().stars(VALID_REVIEW_STARS - 1).contents(VALID_REVIEW_CONTENTS).build();
            ReflectionTestUtils.setField(review2, "id", 102L);
            review2.setWriter(member);
            review2.setCourse(course);

            List<Review> reviews = List.of(review1, review2);

            given(courseRepository.existsById(courseId)).willReturn(true);
            given(reviewRepository.findAllByCourseId(courseId)).willReturn(reviews);

            // when
            ReviewInfoListDto result = reviewService.findAllReviewsByCourse(courseId);

            // then
            double expectedAverage = 4.0;
            assertThat(result).isNotNull();
            assertThat(result.starAverage()).isEqualTo(expectedAverage);
            assertThat(result.reviewCount()).isEqualTo(2);
            assertThat(result.reviewInfoDtoList().getFirst().reviewId()).isEqualTo(review1.getId());
            assertThat(result.reviewInfoDtoList().getLast().reviewId()).isEqualTo(review2.getId());

            verify(courseRepository).existsById(courseId);
            verify(reviewRepository).findAllByCourseId(courseId);
        }

        @Test
        @DisplayName("리뷰 조회 성공 - 리뷰 없음")
        void findAllReviews_success_noReviews() {
            // given
            Long courseId = course.getId();

            // Mock 객체 동작 정의
            given(courseRepository.existsById(courseId)).willReturn(true);
            given(reviewRepository.findAllByCourseId(courseId)).willReturn(List.of());

            // when
            ReviewInfoListDto result = reviewService.findAllReviewsByCourse(courseId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.starAverage()).isEqualTo(0.0);
            assertThat(result.reviewCount()).isEqualTo(0);
            assertThat(result.reviewInfoDtoList()).isEmpty();

            verify(courseRepository).existsById(courseId);
            verify(reviewRepository).findAllByCourseId(courseId);
        }

        @Test
        @DisplayName("리뷰 조회 실패 - 존재하지 않는 코스")
        void findAllReviews_fail_courseNotFound() {
            // given
            Long nonExistentCourseId = 999L;

            given(courseRepository.existsById(nonExistentCourseId)).willReturn(false);

            // when, then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> reviewService.findAllReviewsByCourse(nonExistentCourseId));

            assertThat(exception.getResponseCode()).isEqualTo(ResponseCode.COURSE_NOT_FOUND);
            verify(courseRepository).existsById(nonExistentCourseId);
        }
    }
}