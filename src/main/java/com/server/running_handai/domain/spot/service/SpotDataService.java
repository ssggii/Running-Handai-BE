package com.server.running_handai.domain.spot.service;

import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.entity.TrackPoint;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.domain.course.repository.TrackPointRepository;
import com.server.running_handai.domain.course.service.FileService;
import com.server.running_handai.domain.spot.client.SpotApiClient;
import com.server.running_handai.domain.spot.client.SpotLocationApiClient;
import com.server.running_handai.domain.spot.dto.SpotApiResponseDto;
import com.server.running_handai.domain.spot.dto.SpotLocationApiResponseDto;
import com.server.running_handai.domain.spot.entity.SpotCategory;
import com.server.running_handai.domain.spot.entity.CourseSpot;
import com.server.running_handai.domain.spot.entity.Spot;
import com.server.running_handai.domain.spot.entity.SpotImage;
import com.server.running_handai.domain.spot.repository.CourseSpotRepository;
import com.server.running_handai.domain.spot.repository.SpotRepository;
import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.global.response.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotDataService {
    private final SpotLocationApiClient spotLocationApiClient;
    private final SpotApiClient spotApiClient;
    private final CourseRepository courseRepository;
    private final TrackPointRepository trackPointRepository;
    private final SpotRepository spotRepository;
    private final CourseSpotRepository courseSpotRepository;
    private final FileService fileService;

    /**
     * 코스에 맞는 즐길거리 정보를 [국문 관광정보]의 위치기반 관광정보 API와 공통정보조회 API를 통해 가져옵니다.
     *
     * @param courseId 코스 id
     */
    @Transactional
    public void updateSpots(Long courseId) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new BusinessException(ResponseCode.COURSE_NOT_FOUND));

        List<TrackPoint> trackPoints = trackPointRepository.findByCourseIdOrderBySequenceAsc(course.getId());
        TrackPoint startPoint = trackPoints.getFirst();
        TrackPoint endPoint = trackPoints.getLast();

        // 1. 장소 externalId 수집
        // 조회 조건: 시작점, 출발점, 관광지(12), 음식점(39)
        Set<String> externalIds = new HashSet<>();

        externalIds.addAll(fetchSpotsByLocation(startPoint.getLon(), startPoint.getLat(), 12));
        externalIds.addAll(fetchSpotsByLocation(endPoint.getLon(), endPoint.getLat(), 12));
        externalIds.addAll(fetchSpotsByLocation(startPoint.getLon(), startPoint.getLat(), 39));
        externalIds.addAll(fetchSpotsByLocation(endPoint.getLon(), endPoint.getLat(), 39));

        log.info("[즐길거리 수정] 수집된 고유 externalId 개수: {}", externalIds.size());

        // 2. 수집된 externalId로 장소 정보 수집
        // 이미 externalId에 해당하는 Spot 정보가 있을 경우, 해당 정보를 가져옴
        List<Spot> existingSpots = spotRepository.findByExternalIdIn(externalIds);
        List<Spot> spots = new ArrayList<>(existingSpots);

        Set<String> existingIds = existingSpots.stream()
                .map(Spot::getExternalId)
                .collect(Collectors.toSet());
        externalIds.removeAll(existingIds);

        // 정보가 없는 externalId만 보아 공통정보 조회 API 호출
        for (String externalId : externalIds) {
            fetchSpot(externalId).ifPresent(item -> {
                Spot spot = createSpot(item);
                SpotImage spotImage;
                spotImage = createSpotImage(item);
                if (spotImage != null) {
                    spot.setSpotImage(spotImage);
                }
                spots.add(spot);
            });
        }

        // 3. Course와 Spot의 연관관계 초기화
        courseSpotRepository.deleteByCourseId(courseId);
        log.info("[즐길거리 수정] 기존 즐길거리 데이터 삭제 완료: courseId={}", courseId);

        // 4. Spot, CourseSpot DB 저장
        List<Spot> newSpots = spotRepository.saveAll(spots);

        List<CourseSpot> courseSpots = newSpots.stream()
                .map(spot -> new CourseSpot(course, spot))
                .collect(Collectors.toList());

        courseSpotRepository.saveAll(courseSpots);
        log.info("[즐길거리 수정] DB에 즐길거리 정보 갱신 완료: courseId={}, 개수={}", courseId, spots.size());
    }

    /**
     * [국문 관광정보] 위치기반 관광정보 조회 API를 요청해 장소의 externalId를 수집합니다.
     *
     * @param lon 경도 (x)
     * @param lat 위도 (y)
     * @param contentTypeId 관광 타입 (12: 관광지, 39: 음식점)
     * @return externalId의 Set
     */
    private Set<String> fetchSpotsByLocation(double lon, double lat, int contentTypeId) {
        SpotLocationApiResponseDto spotLocationApiResponseDto = spotLocationApiClient.fetchSpotLocationData(1, 5, "E", lon, lat, contentTypeId);

        if (spotLocationApiResponseDto == null || spotLocationApiResponseDto.getResponse() == null ||
                spotLocationApiResponseDto.getResponse().getBody() == null || spotLocationApiResponseDto.getResponse().getBody().getItems() == null) {
            return Collections.emptySet();
        }

        List<SpotLocationApiResponseDto.Item> items = spotLocationApiResponseDto.getResponse().getBody().getItems().getItemList();

        if (items == null || items.isEmpty()) {
            return Collections.emptySet();
        }

        return items.stream()
                .map(SpotLocationApiResponseDto.Item::getSpotExternalId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * externalId에 대해 상세 SpotApiResponseDto.Item을 조회해 반환합니다.
     *
     * @param externalId 장소 고유번호
     * @return SpotApiResponseDto.Item 정보, 없으면 Optional.empty()
     */
    public Optional<SpotApiResponseDto.Item> fetchSpot(String externalId) {
        SpotApiResponseDto spotApiResponseDto = spotApiClient.fetchSpotData(externalId);

        if (spotApiResponseDto == null || spotApiResponseDto.getResponse() == null ||
                spotApiResponseDto.getResponse().getBody() == null || spotApiResponseDto.getResponse().getBody().getItems() == null) {
            return Optional.empty();
        }

        List<SpotApiResponseDto.Item> items = spotApiResponseDto.getResponse().getBody().getItems().getItemList();

        if (items == null || items.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(items.getFirst());
    }

    /**
     * Spot 객체를 생성합니다.
     *
     * @param item 공통정보 조회 API로부터 받은 응답
     * @return 새로 생성된 Spot 객체
     */
    private Spot createSpot(SpotApiResponseDto.Item item) {
        return Spot.builder()
                .externalId(item.getSpotExternalId())
                .name(item.getSpotName())
                .address(item.getSpotAddress())
                .description(item.getSpotDescription())
                .spotCategory(SpotCategory.findByCategoryNumber(item.getSpotCategoryNumber())
                        .orElse(SpotCategory.UNKNOWN))
                .lat(Double.parseDouble(item.getSpotLatitude()))
                .lon(Double.parseDouble(item.getSpotLongitude()))
                .build();
    }

    /**
     * SpotImage 객체를 생성합니다.
     * 원본을 우선 저장하고, 원본이 없는 경우 썸네일을 저장합니다.
     *
     *  @param item 공통정보 조회 API로부터 받은 응답
     *  @return 새로 생성된 SpotImage 객체, 없으면 null
     */
    private SpotImage createSpotImage(SpotApiResponseDto.Item item) {
        String originalImage = item.getSpotOriginalImage();
        String thumbnailImage = item.getSpotThumbnailImage();

        if (originalImage != null && !originalImage.isBlank()) {
            String s3FileUrl = fileService.uploadFileByUrl(originalImage, "spot");
            return SpotImage.builder()
                    .imgUrl(s3FileUrl)
                    .originalUrl(originalImage)
                    .build();
        } else if (thumbnailImage != null && !thumbnailImage.isBlank()) {
            String s3FileUrl = fileService.uploadFileByUrl(thumbnailImage, "spot");
            return SpotImage.builder()
                    .imgUrl(s3FileUrl)
                    .originalUrl(thumbnailImage)
                    .build();
        }
        return null;
    }
}
