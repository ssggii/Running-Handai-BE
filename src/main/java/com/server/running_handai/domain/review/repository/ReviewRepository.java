package com.server.running_handai.domain.review.repository;

import com.server.running_handai.domain.review.entity.Review;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * courseId로 리뷰 전체 조회
     */
    List<Review> findAllByCourseId(Long courseId);

    /**
     * memberId가 리뷰 작성자의 id와 일치하는지 조회
     */
    boolean existsByIdAndWriterId(Long reviewId, Long writerId);

    /**
     * courseId로 최신순 리뷰 2개 조회
     */
    @Query(value = "SELECT * FROM review r WHERE r.course_id = :courseId ORDER BY created_at DESC LIMIT 2", nativeQuery = true)
    List<Review> findRecent2ByCourseId(@Param("courseId") Long courseId);

    /**
     * courseId에 해당하는 모든 리뷰의 평점(stars) 평균을 계산
     */
    @Query("SELECT AVG(r.stars) FROM Review r WHERE r.course.id = :courseId")
    Double findAverageStarsByCourseId(@Param("courseId") Long courseId);

    /**
     * courseId로 조회한 리뷰의 전체 개수 조회
     */
    long countByCourseId(Long courseId);

    /**
     * 특정 회원이 작성한 리뷰 조회 (연관 엔티티 동시 조회)
     */
    @Query("SELECT r FROM Review r " +
            "LEFT JOIN FETCH r.course c " +
            "LEFT JOIN FETCH c.courseImage ci " +
            "WHERE r.writer.id = :memberId " +
            "ORDER BY r.createdAt DESC")
    List<Review> findReviewsWithDetailsByMemberId(@Param("memberId") Long memberId);
}
