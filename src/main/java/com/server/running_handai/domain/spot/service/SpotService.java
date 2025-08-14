package com.server.running_handai.domain.spot.service;

import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.domain.spot.dto.SpotDetailDto;
import com.server.running_handai.domain.spot.dto.SpotInfoDto;
import com.server.running_handai.domain.spot.entity.Spot;
import com.server.running_handai.domain.spot.repository.SpotRepository;
import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.global.response.exception.BusinessException;
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
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new BusinessException(ResponseCode.COURSE_NOT_FOUND));
        List<Spot> spots = spotRepository.findByCourseIdWithSpotImage(course.getId());

        List<SpotInfoDto> spotInfoDtos = spots.stream()
                .map(spot -> new SpotInfoDto(
                        spot.getId(),
                        spot.getName(),
                        spot.getDescription(),
                        spot.getSpotImage() != null ? spot.getSpotImage().getImgUrl() : null
                ))
                .toList();

        return new SpotDetailDto(courseId, spots.size(), spotInfoDtos);
    }
}
