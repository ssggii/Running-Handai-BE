package com.server.running_handai.domain.review.service;

import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.domain.member.entity.Member;
import com.server.running_handai.domain.member.repository.MemberRepository;
import com.server.running_handai.domain.review.dto.ReviewInfoDto;
import com.server.running_handai.domain.review.dto.ReviewRequestDto;
import com.server.running_handai.domain.review.entity.Review;
import com.server.running_handai.domain.review.repository.ReviewRepository;
import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.global.response.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CourseRepository courseRepository;

    /**
     * 새로운 리뷰를 생성합니다.
     *
     * @param courseId 리뷰 대상인 코스의 ID
     * @param reviewRequest 리뷰 등록 요청 DTO
     * @param member 리뷰를 작성한 회원
     * @return 생성된 리뷰 정보를 담은 DTO
     */
    public ReviewInfoDto createReview(Long courseId, ReviewRequestDto reviewRequest, Member member) {
        if ((reviewRequest.stars() * 2) % 1 != 0) {
            throw new BusinessException(ResponseCode.INVALID_REVIEW_STARS);
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ResponseCode.COURSE_NOT_FOUND));

        Review review = Review.builder()
                .stars(reviewRequest.stars())
                .contents(reviewRequest.contents())
                .build();

        review.setCourse(course);
        review.setWriter(member);
        return ReviewInfoDto.from(reviewRepository.save(review));
    }

}
