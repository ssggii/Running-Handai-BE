package com.server.running_handai.domain.spot.service;

import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.entity.TrackPoint;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.domain.course.repository.TrackPointRepository;
import com.server.running_handai.domain.course.service.FileService;
import com.server.running_handai.domain.spot.client.SpotApiClient;
import com.server.running_handai.domain.spot.client.SpotLocationApiClient;
import com.server.running_handai.domain.spot.client.SpotSyncApiClient;
import com.server.running_handai.domain.spot.dto.SpotApiResponseDto;
import com.server.running_handai.domain.spot.dto.SpotLocationApiResponseDto;
import com.server.running_handai.domain.spot.dto.SpotSyncApiResponseDto;
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
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotDataService {
    private final SpotLocationApiClient spotLocationApiClient;
    private final SpotApiClient spotApiClient;
    private final SpotSyncApiClient spotSyncApiClient;
    private final CourseRepository courseRepository;
    private final TrackPointRepository trackPointRepository;
    private final SpotRepository spotRepository;
    private final CourseSpotRepository courseSpotRepository;
    private final FileService fileService;

    // [국문 관광정보] 관광 타입
    private static final int TOURIST_SPOT_TYPE = 12;
    private static final int RESTAURANT_TYPE = 39;

    // [국문 관광정보] 지역 코드
    private static final int BUSAN_AREA_CODE = 6;

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
        Set<String> externalIds = fetchSpotsByLocationInParallel(startPoint, endPoint);
        log.info("[즐길거리 수정] 수집된 고유 externalId 개수: {}", externalIds.size());

        // 2. 수집된 externalId로 장소 정보 수집
        // 이미 externalId에 해당하는 Spot 정보가 있을 경우, 해당 정보를 가져옴
        List<Spot> existingSpots = spotRepository.findByExternalIdIn(externalIds);
        List<Spot> spots = new ArrayList<>(existingSpots);

        Set<String> existingIds = existingSpots.stream()
                .map(Spot::getExternalId)
                .collect(Collectors.toSet());
        externalIds.removeAll(existingIds);

        // 정보가 없는 externalId만 모아 공통정보 조회 API를 병렬로 호출
        List<SpotApiResponseDto.Item> items = fetchSpotsInParallel(externalIds);

        // Spot, SpotImage 객체 생성
        for (SpotApiResponseDto.Item item : items) {
            Optional<Spot> spotOptional = createSpot(item);
            spotOptional.ifPresent(spot -> {
                SpotImage spotImage = createSpotImage(item);
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
                .map(spot ->
                        CourseSpot.builder().course(course).spot(spot).build())
                .collect(Collectors.toList());

        courseSpotRepository.saveAll(courseSpots);
        log.info("[즐길거리 수정] DB에 즐길거리 정보 갱신 완료: courseId={}, 개수={}", courseId, spots.size());
    }

    /**
     * DB에 저장된 즐길거리 정보를 [국문 관광정보]의 관광정보 동기화 목록 조회 API 호출을 통해 동기화합니다.
     * 변경된 정보가 있을 경우, 공통정보 조회 API를 호출하여 DB와 비교한 후 업데이트합니다.
     *
     * @param date 국문 관광정보 API에서 정보가 수정된 날짜 (YYYYMMDD)
     */
    @Transactional
    public void syncSpots(String date) {
        // 1. 변경된 장소 정보 수집
        List<SpotSyncApiResponseDto.Item> modifiedItems = fetchSpotsSync(date);

        if (modifiedItems.isEmpty()) {
            log.info("[즐길거리 동기화] 변경된 장소 데이터가 없습니다.");
            return;
        }

        // 2. 현재 DB에 있는 External Id만 필터링
        Set<String> modifiedExternalIds = modifiedItems.stream()
                .map(SpotSyncApiResponseDto.Item::getSpotExternalId)
                .collect(Collectors.toSet());

        Set<String> existingExternalIds = spotRepository.findExternalIdsByExternalIdIn(modifiedExternalIds);

        if (existingExternalIds.isEmpty()) {
            log.info("[즐길거리 동기화] DB에 존재하는 장소 데이터 변경 사항이 없어 종료합니다.");
            return;
        }

        log.info("[즐길거리 동기화] 변경된 장소 중 DB에 저장된 장소: {}개", existingExternalIds.size());

        // 3. showflag별로 분류
        // 표출(1)일 경우 업데이트하고, 비표출(0)일 경우 DB에서 삭제
        Set<String> toDelete = modifiedItems.stream()
                .filter(item -> existingExternalIds.contains(item.getSpotExternalId()))
                .filter(item -> "0".equals(item.getSpotShowflag()))
                .map(SpotSyncApiResponseDto.Item::getSpotExternalId)
                .collect(Collectors.toSet());

        Set<String> toUpdate = modifiedItems.stream()
                .filter(item -> existingExternalIds.contains(item.getSpotExternalId()))
                .filter(item -> "1".equals(item.getSpotShowflag()))
                .map(SpotSyncApiResponseDto.Item::getSpotExternalId)
                .collect(Collectors.toSet());

        // 4. DB 삭제 혹은 업데이트
        List<Spot> updatedSpots = new ArrayList<>();
        int deletedSpots = 0;

        if (!toDelete.isEmpty()) {
            for (String externalId : toDelete) {
                Spot spot = spotRepository.findByExternalId(externalId);
                if (spot != null) {
                    spotRepository.delete(spot);
                    log.info("[즐길거리 동기화] 장소 삭제: externalId={}, 이전 이미지 주소={}", externalId, spot.getSpotImage().getImgUrl());
                    deletedSpots++;
                }
            }
        }

        if (!toUpdate.isEmpty()) {
            // 공통정보 조회 API 호출하여 최신 정보 가져오기
            List<SpotApiResponseDto.Item> items = fetchSpotsInParallel(toUpdate);

            for (SpotApiResponseDto.Item item : items) {
                Optional<Spot> newSpot = createSpot(item);
                if (newSpot.isPresent()) {
                    Spot spot = spotRepository.findByExternalId(item.getSpotExternalId());
                    if (spot != null) {
                        boolean isUpdated = spot.syncWith(newSpot.get());
                         updateSpotImage(spot, item);
                        if (isUpdated) {
                            updatedSpots.add(spot);
                        }
                    }
                }
            }
        }

        log.info("[즐길거리 동기화] 완료: 업데이트 대상={}, 삭제 대상={}, 실제 업데이트={}, 실제 삭제={}", toUpdate.size(), toDelete.size(), updatedSpots.size(), deletedSpots);
    }

    @Transactional
    public void syncSpotsByLocation() {

    }

    /**
     * [국문 관광정보] 위치기반 관광정보 조회 API를 요청해 장소의 externalId를 수집합니다.
     * API 응답값의 spotExternalId가 유효하지 않을 경우, Set에 포함하지 않습니다.
     *
     * @param lon 경도 (x)
     * @param lat 위도 (y)
     * @param contentTypeId 관광 타입 (12: 관광지, 39: 음식점)
     * @return externalId Set
     */
    private Set<String> fetchSpotsByLocation(double lon, double lat, int contentTypeId) {
        try {
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
                    .filter(item -> isFieldValid(item.getSpotExternalId(), "spotExternalId", null))
                    .map(SpotLocationApiResponseDto.Item::getSpotExternalId)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("[즐길거리 수정] 위치기반 관광정보 조회 API 응답 파싱 오류: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * [국문 관광정보] 위치기반 관광정보 조회 API를 요청을 병렬로 요청해 4가지 조건(시작점, 도착점, 관광지, 음식점)의 externalId를 수집합니다.
     *
     * @param startPoint 시작점
     * @param endPoint 도착점
     * @return externalId Set
     */
    private Set<String> fetchSpotsByLocationInParallel(TrackPoint startPoint, TrackPoint endPoint) {
        List<Callable<Collection<String>>> tasks = List.of(
                () -> fetchSpotsByLocation(startPoint.getLon(), startPoint.getLat(), TOURIST_SPOT_TYPE),
                () -> fetchSpotsByLocation(endPoint.getLon(), endPoint.getLat(), TOURIST_SPOT_TYPE),
                () -> fetchSpotsByLocation(startPoint.getLon(), startPoint.getLat(), RESTAURANT_TYPE),
                () -> fetchSpotsByLocation(endPoint.getLon(), endPoint.getLat(), RESTAURANT_TYPE)
        );

        return tasks.parallelStream()
                // 각 task 실행 후 결과 수집
                .map(task -> {
                    try {
                        return task.call();
                    } catch (Exception e) {
                        log.error("[즐길거리 수정] 위치기반 관광정보 조회 API 호출 중 오류 발생: error={}", e.getMessage());
                        return Collections.<String>emptyList();
                    }
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    /**
     * [국문 관광정보] 공통정보 조회 API를 요청해 externalId에 대한 SpotApiResponseDto.Item을 반환합니다.
     *
     * @param externalId 장소 고유번호
     * @return SpotApiResponseDto.Item 정보, 없으면 Optional.empty()
     */
    private Optional<SpotApiResponseDto.Item> fetchSpot(String externalId) {
        try {
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
        } catch (Exception e){
            log.warn("[즐길거리 수정] 공통정보 조회 API 응답 파싱 오류: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * [국문 관광정보] 공통정보 조회 API를 요청을 병렬로 요청해 externalId의 관광 정보를 수집합니다.
     *
     * @param externalIds 처리할 externalId Set
     * @return 생성된 Spot 객체들의 List
     */
    private List<SpotApiResponseDto.Item> fetchSpotsInParallel(Set<String> externalIds) {
        return externalIds.parallelStream()
                .map(externalId -> {
                    try {
                        return fetchSpot(externalId);
                    } catch (Exception e) {
                        log.error("[즐길거리 수정] 공통정보 조회 API 호출 중 오류 발생: externalId={}, error={}", externalId, e.getMessage());
                        return Optional.<SpotApiResponseDto.Item>empty();
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * [국문 관광정보] 관광정보 동기화 목록 조회 API를 요청해 변경된 데이터의 SpotSyncApiResponseDto.Item을 수집합니다.
     *
     * @param date 국문 관광정보 API에서 정보가 수정된 날짜 (YYYYMMDD)
     * @return SpotSyncApiResponseDto.Item List
     */
    private List<SpotSyncApiResponseDto.Item> fetchSpotsSync(String date) {
        try {
            SpotSyncApiResponseDto spotSyncApiResponseDto = spotSyncApiClient.fetchSpotSyncData(BUSAN_AREA_CODE, date);

            if (spotSyncApiResponseDto == null || spotSyncApiResponseDto.getResponse() == null || spotSyncApiResponseDto.getResponse().getBody() == null) {
                return Collections.emptyList();
            }

            SpotSyncApiResponseDto.Body body = spotSyncApiResponseDto.getResponse().getBody();

            // 변경 사항이 없는 경우, items = "" 형태로 응답되기 때문에 totalCount 기준으로 분기 처리
            if (body.getTotalCount() == 0) {
                return Collections.emptyList();
            }

            if (body.getItems() == null) {
                return Collections.emptyList();
            }

            List<SpotSyncApiResponseDto.Item> items = body.getItems().getItemList();

            if (items == null || items.isEmpty()) {
                return Collections.emptyList();
            }

            return items.stream()
                    .filter(item -> isFieldValid(item.getSpotExternalId(), "spotExternalId", null))
                    .filter(item -> isFieldValid(item.getSpotShowflag(), "spotShowflag", null))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("[즐길거리 동기화] 관광정보 동기화 목록 조회 API 응답 파싱 오류: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Spot 객체를 생성합니다.
     * 이때 SpotApiResponse,Item의 필드를 검사하여 유효할 경우에만 Spot을 생성합니다.
     *
     * @param item 공통정보 조회 API로부터 받은 응답
     * @return 생성된 Optional Spot, 유효하지 않으면 Optional.empty
     */
    private Optional<Spot> createSpot(SpotApiResponseDto.Item item) {
        if (!isFieldValid(item.getSpotExternalId(), "spotExternalId", null)) {
            return Optional.empty();
        }

        if (!isFieldValid(item.getSpotName(), "spotName", item.getSpotExternalId())) {
            return Optional.empty();
        }

        if (!isFieldValid(item.getSpotDescription(), "spotDescription", item.getSpotExternalId())) {
            return Optional.empty();
        }

        if (!isFieldValid(item.getSpotAddress(), "spotAddress", item.getSpotExternalId())) {
            return Optional.empty();
        }

        if (!isFieldValid(item.getSpotCategoryNumber(), "spotCategoryNumber", item.getSpotExternalId())) {
            return Optional.empty();
        }

        if (!isFieldValid(item.getSpotLongitude(), "spotLongitude", item.getSpotExternalId())
                || !isFieldDouble(item.getSpotLongitude(), "spotLongitude", item.getSpotExternalId())) {
            return Optional.empty();
        }

        if (!isFieldValid(item.getSpotLatitude(), "spotLatitude", item.getSpotExternalId())
                || !isFieldDouble(item.getSpotLatitude(), "spotLatitude", item.getSpotExternalId()))  {
            return Optional.empty();
        }

        Spot spot = Spot.builder()
                .externalId(item.getSpotExternalId())
                .name(item.getSpotName())
                .address(item.getSpotAddress())
                .description(item.getSpotDescription())
                .spotCategory(SpotCategory.findByCategoryNumber(item.getSpotCategoryNumber())
                        .orElse(SpotCategory.UNKNOWN))
                .lat(Double.parseDouble(item.getSpotLatitude()))
                .lon(Double.parseDouble(item.getSpotLongitude()))
                .build();

        return Optional.of(spot);
    }

    /**
     * SpotImage 객체를 생성합니다.
     * originalImage를 우선 저장하고, originalImage이 없는 경우 thumbnailImage를 저장합니다.
     *
     *  @param item 공통정보 조회 API로부터 받은 응답
     *  @return 새로 생성된 SpotImage 객체, 없으면 null
     */
    private SpotImage createSpotImage(SpotApiResponseDto.Item item) {
        String originalImage = item.getSpotOriginalImage();
        String thumbnailImage = item.getSpotThumbnailImage();

        if (isFieldValid(originalImage, "spotOriginalImage", item.getSpotExternalId())) {
            String s3FileUrl = fileService.uploadFileByUrl(originalImage, "spot");
            return SpotImage.builder()
                    .imgUrl(s3FileUrl)
                    .originalUrl(originalImage)
                    .build();
        } else if (isFieldValid(thumbnailImage, "spotThumbnailImage", item.getSpotExternalId())) {
            String s3FileUrl = fileService.uploadFileByUrl(thumbnailImage, "spot");
            return SpotImage.builder()
                    .imgUrl(s3FileUrl)
                    .originalUrl(thumbnailImage)
                    .build();
        }

        return null;
    }

    /**
     * API 데이터와 비교하여 Spot 엔티티의 이미지를 업데이트합니다.
     * 기존 이미지 URL과 새로운 이미지 URL들이 모두 다른 경우에만 S3 업로드 및 업데이트를 수행합니다.
     *
     * @param spot DB에서 조회한 기존 Spot 엔티티
     * @param item 공통정보 조회 API로부터 받은 응답
     */
    private void updateSpotImage(Spot spot, SpotApiResponseDto.Item item) {
        String oldOriginalUrl = spot.getSpotImage() != null ? spot.getSpotImage().getOriginalUrl() : null;

        String newOriginalImage = item.getSpotOriginalImage();
        String newThumbnailImage = item.getSpotThumbnailImage();

        if (!Objects.equals(oldOriginalUrl, newOriginalImage) && !Objects.equals(oldOriginalUrl, newThumbnailImage)) {
            SpotImage spotImage = createSpotImage(item);
            if (spotImage != null) {
                spot.setSpotImage(spotImage);
                log.info("[즐길거리 동기화] 이미지 업데이트: externalId={}, 이전 이미지 주소={}", item.getSpotExternalId(), spot.getSpotImage().getImgUrl());
            }
        }
    }

    /**
     * API 응답값의 필드가 null이거나 빈 문자열인지 검사합니다.
     * null 또는 빈 문자열일 경우 객체를 생성하지 않고 넘어갑니다.
     *
     * @param value 확인할 문자열
     * @param fieldName 필드 이름 (로그용)
     * @param externalId 장소 고유번호 (로그용)
     * @return null 혹은 빈 문자열인 경우 false, 아니면 true
     */
    private boolean isFieldValid(String value, String fieldName, String externalId) {
        if (value == null || value.isBlank()) {
            log.warn("[즐길거리 수정] API 응답값 필드가 null 또는 빈 문자열이어서 건너뜀: externalId={}, fieldName={}", externalId, fieldName);
            return false;
        }

        return true;
    }

    /**
     * API 응답값에서 문자열로 들어오지만 Double로 저장되어야 하는 필드가 제대로 변환되는지 검사합니다.
     * 변환되지 않는 경우 객체를 생성하지 않고 넘어갑니다.
     *
     * @param doubleString 확인할 문자열
     * @param fieldName 필드 이름 (로그용)
     * @param externalId 장소 고유번호 (로그용)
     * @return Double로 변환되면 true, 아니면 false
     */
    private boolean isFieldDouble(String doubleString, String fieldName, String externalId) {
        try {
            Double.parseDouble(doubleString);
            return true;
        } catch (NumberFormatException e) {
            log.warn("[즐길거리 수정] API 응답값 필드가 Double로 변환되지 않아 건너뜀: externalId={}, fieldName={}", externalId, fieldName);
            return false;
        }
    }
}
