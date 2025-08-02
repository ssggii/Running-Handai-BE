package com.server.running_handai.domain.review.repository;

import com.server.running_handai.domain.review.entity.Review;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * courseId로 리뷰 전체 조회
     */
    List<Review> findAllByCourseId(Long courseId);

    /**
     * memberId가 리뷰 작성자의 id와 일치하는지 조회
     */
    boolean existsByIdAndWriterId(Long reviewId, Long writerId);

}
