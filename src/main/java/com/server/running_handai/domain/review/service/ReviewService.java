package com.server.running_handai.domain.review.service;

import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.domain.member.entity.Member;
import com.server.running_handai.domain.review.dto.ReviewInfoDto;
import com.server.running_handai.domain.review.dto.ReviewInfoListDto;
import com.server.running_handai.domain.review.dto.ReviewCreateRequestDto;
import com.server.running_handai.domain.review.dto.ReviewUpdateRequestDto;
import com.server.running_handai.domain.review.entity.Review;
import com.server.running_handai.domain.review.repository.ReviewRepository;
import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.global.response.exception.BusinessException;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CourseRepository courseRepository;

    /**
     * 코스의 리뷰를 생성합니다.
     *
     * @param courseId 리뷰 대상인 코스의 ID
     * @param requestDto 리뷰 등록 요청 DTO
     * @param member 리뷰를 작성한 회원
     * @return 생성된 리뷰 정보를 담은 DTO
     */
    @Transactional
    public ReviewInfoDto createReview(Long courseId, ReviewCreateRequestDto requestDto, Member member) {
        if ((requestDto.stars() * 2) % 1 != 0) {
            throw new BusinessException(ResponseCode.INVALID_REVIEW_STARS);
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ResponseCode.COURSE_NOT_FOUND));

        Review review = Review.builder()
                .stars(requestDto.stars())
                .contents(requestDto.contents())
                .build();

        review.setCourse(course);
        review.setWriter(member);
        return ReviewInfoDto.from(reviewRepository.save(review));
    }

    /**
     * 코스의 리뷰를 전체 조회합니다.
     *
     * @param courseId 리뷰 대상인 코스의 ID
     * @return 조회된 리뷰 목록을 담는 DTO
     */
    public ReviewInfoListDto findAllReviewsByCourse(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new BusinessException(ResponseCode.COURSE_NOT_FOUND);
        }

        List<Review> reviews = reviewRepository.findAllByCourseId(courseId);
        if (reviews.isEmpty()) {
            return ReviewInfoListDto.from(0, Collections.emptyList());
        }

        List<ReviewInfoDto> reviewInfoDtos = reviews.stream().map(ReviewInfoDto::from).toList();
        double averageStars = reviews.stream().mapToDouble(Review::getStars).average().orElse(0.0);
        averageStars = Math.round(averageStars * 10) / 10.0; // 소수 첫째 자리까지 반올림

        return ReviewInfoListDto.from(averageStars, reviewInfoDtos);
    }

    /**
     * 코스의 리뷰를 수정합니다.
     *
     * @param reviewId 수정하려는 리뷰의 ID
     * @param requestDto 리뷰 수정 요청 DTO
     * @param member 리뷰 수정을 요청한 회원
     * @return 수정된 리뷰 정보를 담은 DTO
     */
    @Transactional
    public ReviewInfoDto updateReview(Long reviewId, ReviewUpdateRequestDto requestDto, Member member) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ResponseCode.REVIEW_NOT_FOUND));

        if (!review.getWriter().getId().equals(member.getId())) {
            throw new BusinessException(ResponseCode.ACCESS_DENIED); // 작성자가 아니라면 접근 권한 없음
        }

        Double newStars = requestDto.stars();
        String newContents = requestDto.contents();

        // 별점을 수정하는 경우
        if (newStars != null) {
            if ((newStars * 2) % 1 != 0) {
                throw new BusinessException(ResponseCode.INVALID_REVIEW_STARS);
            }
            review.updateStars(newStars);
        }

        // 리뷰 내용을 수정하는 경우
        if (newContents != null) {
            if (newContents.isBlank()) {
                throw new BusinessException(ResponseCode.EMPTY_REVIEW_CONTENTS);
            }
            review.updateContents(newContents);
        }

        return ReviewInfoDto.from(reviewRepository.save(review));
    }

}
