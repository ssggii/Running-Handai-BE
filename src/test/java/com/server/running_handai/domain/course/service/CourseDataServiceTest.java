package com.server.running_handai.domain.course.service;

import com.server.running_handai.domain.course.client.SpotApiClient;
import com.server.running_handai.domain.course.client.SpotLocationApiClient;
import com.server.running_handai.domain.course.dto.SpotApiResponseDto;
import com.server.running_handai.domain.course.dto.SpotLocationApiResponseDto;
import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.entity.Spot;
import com.server.running_handai.domain.course.entity.TrackPoint;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.domain.course.repository.CourseSpotRepository;
import com.server.running_handai.domain.course.repository.SpotRepository;
import com.server.running_handai.domain.course.repository.TrackPointRepository;
import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.global.response.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class CourseDataServiceTest {

    @InjectMocks
    private CourseDataService courseDataService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private TrackPointRepository trackPointRepository;

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private CourseSpotRepository courseSpotRepository;

    @Mock
    private SpotLocationApiClient spotLocationApiClient;

    @Mock
    private SpotApiClient spotApiClient;

    @Mock
    private FileService fileService;

    private static final Long COURSE_ID = 1L;

    // [성공]
    // 1. 모두 새로운 호출을 진행하는 경우
    @Test
    @DisplayName("즐길거리 수정 성공 - 모두 새로운 호출 진행")
    void updateSpots_success_allNewFetch() {
        // given
        Course course = createMockCourse(COURSE_ID);
        TrackPoint startPoint = TrackPoint.builder().lon(127.1).lat(37.1).build();
        TrackPoint endPoint = TrackPoint.builder().lon(127.2).lat(37.2).build();
        Set<String> externalIds = Set.of("externalId1");

        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course));
        given(trackPointRepository.findByCourseIdOrderBySequenceAsc(course.getId())).willReturn(List.of(startPoint, endPoint));

        SpotLocationApiResponseDto spotLocationApiResponseDto = createSpotLocationApiResponse(externalIds);
        given(spotLocationApiClient.fetchSpotLocationData(anyInt(), anyInt(), anyString(), anyDouble(), anyDouble(), anyInt()))
                .willReturn(spotLocationApiResponseDto);

        given(spotRepository.findByExternalIdIn(anySet())).willReturn(Collections.emptyList());
        SpotApiResponseDto spotApiResponseDto = createSpotApiResponse("externalId1");
        given(spotApiClient.fetchSpotData(anyString())).willReturn(spotApiResponseDto);
        given(fileService.uploadFileByUrl(anyString(), eq("spot"))).willReturn("https://mock-s3-url.com/externalId1.png");

        // when
        courseDataService.updateSpots(COURSE_ID);

        // then
        verify(courseRepository).findById(COURSE_ID);
        verify(trackPointRepository).findByCourseIdOrderBySequenceAsc(course.getId());
        verify(spotLocationApiClient, times(4)).fetchSpotLocationData(anyInt(), anyInt(), anyString(), anyDouble(), anyDouble(), anyInt());
        verify(spotRepository).findByExternalIdIn(anySet());
        verify(spotApiClient, times(externalIds.size())).fetchSpotData(anyString());
        verify(fileService, times(externalIds.size())).uploadFileByUrl(anyString(), eq("spot"));
        verify(courseSpotRepository).deleteByCourseId(COURSE_ID);
        verify(spotRepository).saveAll(anyList());
        verify(courseSpotRepository).saveAll(anyList());
    }

    // 2. SpotImage가 Null일 경우
    @Test
    @DisplayName("즐길거리 수정 성공 - SpotImage가 Null")
    void updateSpots_success_noSpotImage() {
        // given
        Course course = createMockCourse(COURSE_ID);
        TrackPoint startPoint = TrackPoint.builder().lon(127.1).lat(37.1).build();
        TrackPoint endPoint = TrackPoint.builder().lon(127.2).lat(37.2).build();
        Set<String> externalIds = Set.of("externalId1");

        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course));
        given(trackPointRepository.findByCourseIdOrderBySequenceAsc(course.getId())).willReturn(List.of(startPoint, endPoint));

        SpotLocationApiResponseDto spotLocationApiResponseDto = createSpotLocationApiResponse(externalIds);
        given(spotLocationApiClient.fetchSpotLocationData(anyInt(), anyInt(), anyString(), anyDouble(), anyDouble(), anyInt()))
                .willReturn(spotLocationApiResponseDto);

        given(spotRepository.findByExternalIdIn(anySet())).willReturn(Collections.emptyList());
        SpotApiResponseDto spotApiResponseDto = createSpotApiResponse("externalId1");
        SpotApiResponseDto.Item item = spotApiResponseDto.getResponse().getBody().getItems().getItemList().getFirst();
        // SpotImage을 생성할 수 있는 URL이 모두 Null이라 설정
        ReflectionTestUtils.setField(item, "spotOriginalImage", null);
        ReflectionTestUtils.setField(item, "spotThumbnailImage", null);
        given(spotApiClient.fetchSpotData(anyString())).willReturn(spotApiResponseDto);

        // when
        courseDataService.updateSpots(COURSE_ID);

        // then
        // uploadFileByUrl이 아예 호출되지 않는지 확인
        verify(fileService, never()).uploadFileByUrl(anyString(), eq("spot"));
        verify(courseSpotRepository).deleteByCourseId(COURSE_ID);
        verify(spotRepository).saveAll(anyList());
        verify(courseSpotRepository).saveAll(anyList());
    }

    // 3. Spot 일부가 DB에 존재하는 경우
    @Test
    @DisplayName("즐길거리 수정 성공 - Spot 일부가 DB에 존재")
    void updateSpots_success_existingSpots() {
        // given
        Course course = createMockCourse(COURSE_ID);
        TrackPoint startPoint = TrackPoint.builder().lon(127.1).lat(37.1).build();
        TrackPoint endPoint = TrackPoint.builder().lon(127.2).lat(37.2).build();
        Set<String> externalIds = Set.of("externalId1", "externalId2");
        Spot existingSpot1 = createMockSpot("externalId1");

        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course));
        given(trackPointRepository.findByCourseIdOrderBySequenceAsc(course.getId())).willReturn(List.of(startPoint, endPoint));

        SpotLocationApiResponseDto spotLocationApiResponseDto = createSpotLocationApiResponse(externalIds);
        given(spotLocationApiClient.fetchSpotLocationData(anyInt(), anyInt(), anyString(), anyDouble(), anyDouble(), anyInt()))
                .willReturn(spotLocationApiResponseDto);

        // externalId1인 기존 Spot이 있다고 설정
        given(spotRepository.findByExternalIdIn(anySet())).willReturn(List.of(existingSpot1));
        SpotApiResponseDto spotApiResponseDto = createSpotApiResponse("externalId2");
        given(spotApiClient.fetchSpotData(anyString())).willReturn(spotApiResponseDto);
        given(fileService.uploadFileByUrl(anyString(), eq("spot"))).willReturn("https://mock-s3-url.com/externalId2.png");

        // when
        courseDataService.updateSpots(COURSE_ID);

        // then
        // externalId2만 공통정보 조회 API 호출하고, externalId1은 호출하지 않는지 확인
        verify(spotApiClient, times(1)).fetchSpotData(eq("externalId2"));
        verify(spotApiClient, never()).fetchSpotData(eq("externalId1"));
        verify(fileService).uploadFileByUrl(anyString(), eq("spot"));
        verify(courseSpotRepository).deleteByCourseId(COURSE_ID);
        verify(spotRepository).saveAll(anyList());
        verify(courseSpotRepository).saveAll(anyList());
    }

    // 4. SpotApiResponseDto가 Null인 경우
    @Test
    @DisplayName("즐길거리 수정 성공 - SpotApiResponseDto가 Null")
    void updateSpots_success_noSpotApiResponseDto() {
        // given
        Course course = createMockCourse(COURSE_ID);
        TrackPoint startPoint = TrackPoint.builder().lon(127.1).lat(37.1).build();
        TrackPoint endPoint = TrackPoint.builder().lon(127.2).lat(37.2).build();
        Set<String> externalIds = Set.of("externalId1");

        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course));
        given(trackPointRepository.findByCourseIdOrderBySequenceAsc(course.getId())).willReturn(List.of(startPoint, endPoint));

        SpotLocationApiResponseDto spotLocationApiResponseDto = createSpotLocationApiResponse(externalIds);
        given(spotLocationApiClient.fetchSpotLocationData(anyInt(), anyInt(), anyString(), anyDouble(), anyDouble(), anyInt()))
                .willReturn(spotLocationApiResponseDto);

        given(spotRepository.findByExternalIdIn(anySet())).willReturn(Collections.emptyList());
        // 공통정보 조회 API 응답값이 Null이라고 설정
        given(spotApiClient.fetchSpotData(anyString())).willReturn(null);

        // when
        courseDataService.updateSpots(COURSE_ID);

        // then
        // 공통정보 조회 API 호출은 되지만, 이미지 업로드는 실행되지 않고, 빈 리스트가 저장되는지 확인
        verify(spotApiClient).fetchSpotData(anyString());
        verify(fileService, never()).uploadFileByUrl(anyString(), eq("spot"));
        verify(courseSpotRepository).deleteByCourseId(COURSE_ID);
        verify(spotRepository).saveAll(argThat(list -> ((Collection<?>) list).isEmpty()));
        verify(courseSpotRepository).saveAll(argThat(list -> ((Collection<?>) list).isEmpty()));
    }

    // 5. SpotLocationApiResponseDto가 Null인 경우
    @Test
    @DisplayName("즐길거리 수정 성공 - SpotLocationApiResponseDto가 Null")
    void updateSpots_success_noSpotLocationApiResponseDto() {
        // given
        Course course = createMockCourse(COURSE_ID);
        TrackPoint startPoint = TrackPoint.builder().lon(127.1).lat(37.1).build();
        TrackPoint endPoint = TrackPoint.builder().lon(127.2).lat(37.2).build();

        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course));
        given(trackPointRepository.findByCourseIdOrderBySequenceAsc(course.getId())).willReturn(List.of(startPoint, endPoint));

        // 위치기반 정보조회 API 응답값이 Null이라고 설정
        given(spotLocationApiClient.fetchSpotLocationData(anyInt(), anyInt(), anyString(), anyDouble(), anyDouble(), anyInt()))
                .willReturn(null);

        given(spotRepository.findByExternalIdIn(anySet())).willReturn(Collections.emptyList());

        // when
        courseDataService.updateSpots(COURSE_ID);

        // then
        // 위치기반 정보조회 API 호출은 되지만, 공통정보 조회 API와 이미지 업로드는 실행되지 않고, 빈 리스트가 저장되는지 확인
        verify(spotLocationApiClient, times(4)).fetchSpotLocationData(anyInt(), anyInt(), anyString(), anyDouble(), anyDouble(), anyInt());
        verify(spotApiClient, never()).fetchSpotData(anyString());
        verify(fileService, never()).uploadFileByUrl(anyString(), eq("spot"));
        verify(courseSpotRepository).deleteByCourseId(COURSE_ID);
        verify(spotRepository).saveAll(argThat(list -> ((Collection<?>) list).isEmpty()));
        verify(courseSpotRepository).saveAll(argThat(list -> ((Collection<?>) list).isEmpty()));
    }

    // [실패]
    // 1. Course가 없는 경우
    @Test
    @DisplayName("즐길거리 수정 실패 - Course가 없음")
    void updateSpots_fail_courseNotFound() {
        // given
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.empty());

        // when, then
        BusinessException exception = assertThrows(BusinessException.class, () -> courseDataService.updateSpots(COURSE_ID));
        assertThat(exception.getResponseCode()).isEqualTo(ResponseCode.COURSE_NOT_FOUND);
    }

    // 헬퍼 메서드
    private Course createMockCourse(Long courseId) {
        Course course = Course.builder().build();
        ReflectionTestUtils.setField(course, "id", courseId);
        return course;
    }

    private Spot createMockSpot(String externalId) {
        Spot spot = Spot.builder().externalId(externalId).build();
        return spot;
    }

    // 수정된 SpotLocationApiResponseDto 생성 메서드
    private SpotLocationApiResponseDto createSpotLocationApiResponse(Set<String> externalIds) {
        // 실제 객체를 생성하고 필요한 데이터만 설정
        SpotLocationApiResponseDto dto = new SpotLocationApiResponseDto();
        SpotLocationApiResponseDto.Response response = new SpotLocationApiResponseDto.Response();
        SpotLocationApiResponseDto.Body body = new SpotLocationApiResponseDto.Body();
        SpotLocationApiResponseDto.Items items = new SpotLocationApiResponseDto.Items();

        List<SpotLocationApiResponseDto.Item> itemList = new ArrayList<>();
        for (String externalId : externalIds) {
            SpotLocationApiResponseDto.Item item = new SpotLocationApiResponseDto.Item();
            ReflectionTestUtils.setField(item, "spotExternalId", externalId);
            itemList.add(item);
        }

        ReflectionTestUtils.setField(items, "itemList", itemList);
        ReflectionTestUtils.setField(body, "items", items);
        ReflectionTestUtils.setField(response, "body", body);
        ReflectionTestUtils.setField(dto, "response", response);

        return dto;
    }

    private SpotApiResponseDto createSpotApiResponse(String externalId) {
        SpotApiResponseDto dto = new SpotApiResponseDto();
        SpotApiResponseDto.Response response = new SpotApiResponseDto.Response();
        SpotApiResponseDto.Body body = new SpotApiResponseDto.Body();
        SpotApiResponseDto.Items items = new SpotApiResponseDto.Items();
        SpotApiResponseDto.Item item = new SpotApiResponseDto.Item();

        // Item 필드 설정
        ReflectionTestUtils.setField(item, "spotExternalId", externalId);
        ReflectionTestUtils.setField(item, "spotName", "Test Spot");
        ReflectionTestUtils.setField(item, "spotAddress", "Test Address");
        ReflectionTestUtils.setField(item, "spotDescription", "Test Description");
        ReflectionTestUtils.setField(item, "spotCategoryNumber", "12");
        ReflectionTestUtils.setField(item, "spotLatitude", "37.123");
        ReflectionTestUtils.setField(item, "spotLongitude", "127.123");
        ReflectionTestUtils.setField(item, "spotOriginalImage", "http://example.com/original.png");
        ReflectionTestUtils.setField(item, "spotThumbnailImage", "http://example.com/thumbnail.png");

        ReflectionTestUtils.setField(items, "itemList", List.of(item));
        ReflectionTestUtils.setField(body, "items", items);
        ReflectionTestUtils.setField(response, "body", body);
        ReflectionTestUtils.setField(dto, "response", response);

        return dto;
    }
}