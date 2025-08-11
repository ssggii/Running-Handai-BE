package com.server.running_handai.domain.spot.service;

import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.domain.spot.dto.SpotDetailDto;
import com.server.running_handai.domain.spot.dto.SpotInfoDto;
import com.server.running_handai.domain.spot.entity.Spot;
import com.server.running_handai.domain.spot.entity.SpotImage;
import com.server.running_handai.domain.spot.repository.SpotRepository;
import com.server.running_handai.global.response.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.server.running_handai.global.response.ResponseCode.COURSE_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class SpotServiceTest {
    @InjectMocks
    private SpotService spotService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private SpotRepository spotRepository;

    private static final Long COURSE_ID = 1L;
    private Course course;

    @BeforeEach
    void setUp() {
        course = createMockCourse(COURSE_ID);
    }

    /**
     * [즐길거리 전체 조회] 성공
     * 1. Course에 해당되는 Spot이 존재하는 경우
     */
    @Test
    @DisplayName("즐길거리 전체 조회 - Spot이 존재")
    void getSpotDetails_success_SpotExists() {
        // given
        // 2개의 Spot 중 1개만 SpotImage가 있다고 가정
        SpotImage spotImage = createMockSpotImage("http://mock-image-url");
        Spot spot1 = createMockSpot(101L, "Spot1", "Description1", spotImage);
        Spot spot2 = createMockSpot(102L, "Spot2", "Description2", null);
        List<Spot> spots = List.of(spot1, spot2);

        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course));
        given(spotRepository.findByCourseId(COURSE_ID)).willReturn(spots);

        // when
        SpotDetailDto result = spotService.getSpotDetails(COURSE_ID);

        // then
        assertThat(result.courseId()).isEqualTo(COURSE_ID);
        assertThat(result.spotCount()).isEqualTo(spots.size());
        assertThat(result.spots()).hasSize(2);

        SpotInfoDto spotInfo1 = result.spots().get(0);
        assertThat(spotInfo1.spotId()).isEqualTo(spot1.getId());
        assertThat(spotInfo1.name()).isEqualTo(spot1.getName());
        assertThat(spotInfo1.description()).isEqualTo(spot1.getDescription());
        assertThat(spotInfo1.imageUrl()).isEqualTo("http://mock-image-url");

        SpotInfoDto spotInfo2 = result.spots().get(1);
        assertThat(spotInfo2.spotId()).isEqualTo(spot2.getId());
        assertThat(spotInfo2.name()).isEqualTo(spot2.getName());
        assertThat(spotInfo2.description()).isEqualTo(spot2.getDescription());
        assertThat(spotInfo2.imageUrl()).isNull();

        verify(courseRepository).findById(COURSE_ID);
        verify(spotRepository).findByCourseId(COURSE_ID);
    }

    /**
     * [즐길거리 전체 조회] 성공
     * 2. Course에 해당되는 Spot이 존재하지 않는 경우
     */
    @Test
    @DisplayName("즐길거리 전체 조회 - Spot이 존재하지 않음")
    void getSpotDetails_success_noSpot() {
        // given
        // Spot이 존재하지 않으면 빈 리스트로 응답해야 함
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course));
        given(spotRepository.findByCourseId(COURSE_ID)).willReturn(Collections.emptyList());

        // when
        SpotDetailDto result = spotService.getSpotDetails(COURSE_ID);

        // then
        assertThat(result.courseId()).isEqualTo(COURSE_ID);
        assertThat(result.spotCount()).isEqualTo(0);
        assertThat(result.spots()).isEmpty();

        verify(courseRepository).findById(COURSE_ID);
        verify(spotRepository).findByCourseId(course.getId());
    }

    /**
     * [즐길거리 전체 조회] 실패
     * 1. Course가 존재하지 않을 경우
     */
    @Test
    @DisplayName("즐길거리 전체 조회 - 존재하지 않는 코스")
    void getSpotDetails_fail_courseNotFound() {
        // given
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.empty());

        // when, then
        BusinessException exception = assertThrows(BusinessException.class, () -> spotService.getSpotDetails(COURSE_ID));
        assertThat(exception.getResponseCode()).isEqualTo(COURSE_NOT_FOUND);
    }

    // 헬퍼 메서드
    private Course createMockCourse(Long courseId) {
        Course course = Course.builder().build();
        ReflectionTestUtils.setField(course, "id", courseId);
        return course;
    }

    private SpotImage createMockSpotImage(String imageUrl) {
        return SpotImage.builder().imgUrl(imageUrl).build();
    }

    private Spot createMockSpot(Long spotId, String name, String description, SpotImage spotImage) {
        Spot spot = Spot.builder().name(name).description(description).build();
        ReflectionTestUtils.setField(spot, "id", spotId);
        ReflectionTestUtils.setField(spot, "spotImage", spotImage);
        return spot;
    }
}
