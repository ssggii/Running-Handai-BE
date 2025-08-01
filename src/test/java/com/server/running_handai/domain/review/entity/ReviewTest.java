package com.server.running_handai.domain.review.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.server.running_handai.domain.course.entity.Area;
import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.entity.CourseLevel;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.domain.member.entity.Member;
import com.server.running_handai.domain.member.entity.Provider;
import com.server.running_handai.domain.member.entity.Role;
import com.server.running_handai.domain.member.repository.MemberRepository;
import com.server.running_handai.domain.review.repository.ReviewRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ReviewTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CourseRepository courseRepository;

    private static final String POINT = "POINT(127.027621 37.497928)";

    private Point creatPoint() throws ParseException {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        WKTReader wktReader = new WKTReader(geometryFactory);
        return (Point) wktReader.read(POINT);
    }

    @Test
    @DisplayName("Review의 contents가 2000자를 초과하면 예외가 발생한다")
    void contentsLengthValidation() {
        // given
        String longContents = "a".repeat(2001); // 2001자의 contents
        Double stars = 3.5; // 별점 3.5
        Review review = Review.builder().contents(longContents).stars(stars).build();

        // when, then
        assertThrows(ConstraintViolationException.class, () -> {
            // @Size(max=2000)에 의해 ConstraintViolationException 발생
            // DB 레벨에서는 DataIntegrityViolationException 발생
            reviewRepository.saveAndFlush(review);
        });
    }

    @Test
    @DisplayName("회원이 삭제되어도 해당 회원이 작성한 리뷰는 삭제되지 않는다")
    void reviewRemainsWhenWriterIsDeleted() throws ParseException {
        // given
        Member savedWriter = memberRepository.save(Member.builder()
                .providerId("providerId1")
                .provider(Provider.GOOGLE)
                .email("member1@email.com")
                .nickname("nickname1")
                .role(Role.USER)
                .build());

        Course savedCourse = courseRepository.save(Course.builder()
                .name("courseName1")
                .distance(10)
                .duration(120)
                .level(CourseLevel.MEDIUM)
                .area(Area.HAEUN_GWANGAN)
                .gpxPath("gpxPath1")
                .maxElevation(128.99)
                .minElevation(-0.9)
                .startPoint(creatPoint())
                .build());

        Review review = Review.builder().contents("contents").stars(3.5).build();
        review.setWriter(savedWriter);
        review.setCourse(savedCourse);
        reviewRepository.saveAndFlush(review);

        Long reviewId = review.getId();

        // when
        review.setWriter(null);
        reviewRepository.save(review);

        memberRepository.delete(savedWriter);
        memberRepository.flush();

        // then
        Review foundReview = reviewRepository.findById(reviewId).orElse(null);
        assertThat(foundReview.getWriter()).isNull(); // Review의 작성자는 null이어야 함 (삭제된 회원)
        assertThat(foundReview).isNotNull(); // Review는 여전히 존재해야 함
    }

    @Test
    @DisplayName("코스가 삭제되면 코스의 리뷰도 삭제된다.")
    void reviewDeletedWhenCourseIsDeleted() throws ParseException {
        // given
        Member writer = Member.builder()
                .providerId("providerId1")
                .provider(Provider.GOOGLE)
                .email("member1@email.com")
                .nickname("nickname1")
                .role(Role.USER)
                .build();
        Member savedWriter = memberRepository.save(writer);

        Course course = Course.builder()
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
        Course savedCourse = courseRepository.save(course);

        Review review = Review.builder().contents("contents").stars(3.5).build();
        review.setWriter(savedWriter);
        review.setCourse(savedCourse);
        reviewRepository.saveAndFlush(review);

        // when
        courseRepository.delete(savedCourse);

        // then
        assertThat(reviewRepository.findById(review.getId())).isEmpty();
    }
}