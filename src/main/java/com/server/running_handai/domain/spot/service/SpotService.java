package com.server.running_handai.domain.spot.service;

import static com.server.running_handai.domain.course.entity.SpotStatus.COMPLETED;
import static com.server.running_handai.domain.course.entity.SpotStatus.FAILED;
import static com.server.running_handai.global.response.ResponseCode.COURSE_NOT_FOUND;

import com.server.running_handai.domain.course.dto.CourseSummaryDto;
import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.domain.spot.dto.SpotDetailDto;
import com.server.running_handai.domain.spot.dto.SpotInfoDto;
import com.server.running_handai.domain.spot.entity.Spot;
import com.server.running_handai.domain.spot.repository.SpotRepository;
import com.server.running_handai.global.response.exception.BusinessException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpotService {
    private final CourseRepository courseRepository;
    private final SpotRepository spotRepository;

    /**
     * 코스에 해당되는 즐길거리를 전체 조회합니다.
     *
     * @param courseId 조회하려는 코스의 ID
     * @return 조회된 즐길거리 목록 DTO
     */
    public SpotDetailDto getSpotDetails(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(COURSE_NOT_FOUND));

        // 즐길거리 초기화 완료 시, 즐길거리 조회 결과 반환
        if (course.getSpotStatus() == COMPLETED) {
            List<Spot> spots = spotRepository.findByCourseIdWithSpotImage(courseId);
            List<SpotInfoDto> spotInfoDtos = spots.stream()
                    .map(SpotInfoDto::from)
                    .toList();
            return SpotDetailDto.from(course, spotInfoDtos);
        }

        // 즐길거리 초기화 실패한 경우, 로그만 남기고 빈 리스트 반환
        if (course.getSpotStatus() == FAILED) {
            log.warn("[즐길거리 전체 조회] 즐길거리 초기화에 실패한 코스입니다. courseId: {}", courseId);
        }

        // 그 외의 경우, 상태값과 함께 빈 리스트 반환
        return SpotDetailDto.from(course, Collections.emptyList());
    }
}
