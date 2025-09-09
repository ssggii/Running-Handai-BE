package com.server.running_handai.domain.spot.service;

import static com.server.running_handai.domain.course.entity.SpotStatus.COMPLETED;
import static com.server.running_handai.global.response.ResponseCode.COURSE_NOT_FOUND;
import static com.server.running_handai.global.response.ResponseCode.SPOT_INITIALIZATION_FAILED;

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

        // 즐길거리 초기화 완료가 안되었다면 빈 리스트 반환
        if (course.getSpotStatus() != COMPLETED) {
            return SpotDetailDto.from(course, Collections.emptyList());
        }

        // 초기화 완료된 경우에만 즐길거리 조회
        List<Spot> spots = spotRepository.findByCourseIdWithSpotImage(courseId);
        List<SpotInfoDto> spotInfoDtos = spots.stream()
                .map(SpotInfoDto::from)
                .toList();

        return SpotDetailDto.from(course, spotInfoDtos);
    }

    /**
     * 코스의 즐길거리 초기화 완료 시 즐길거리 데이터를 반환하고, 그 외에 경우 빈 리스트를 반환합니다.
     *
     * @param courseId 조회하려는 코스의 ID
     * @return 즐길거리 조회용 DTO 리스트 (초기화 상태 포함)
     * @throws BusinessException 즐길거리 초기화 실패
     */
    public SpotDetailDto getSpotsByStatus(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(COURSE_NOT_FOUND));
        
        List<SpotInfoDto> spots = (course.getSpotStatus() == COMPLETED) ?
                spotRepository.findRandom3ByCourseId(courseId) : Collections.emptyList();

        return SpotDetailDto.from(course, spots);
    }
}
