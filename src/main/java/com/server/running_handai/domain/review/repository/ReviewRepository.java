package com.server.running_handai.domain.review.repository;

import com.server.running_handai.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
}
