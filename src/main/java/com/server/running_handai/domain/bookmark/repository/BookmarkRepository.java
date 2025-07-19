package com.server.running_handai.domain.bookmark.repository;

import com.server.running_handai.domain.bookmark.entity.Bookmark;
import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    /**
     * 회원이 특정 코스를 북마크했는지 확인
     */
    boolean existsByMemberAndCourse(Member member, Course course);

    /**
     * 회원과 코스로 북마크 조회
     */
    Optional<Bookmark> findByMemberAndCourse(Member member, Course course);

    /**
     * 코스 id를 기준으로 북마크 수 조회
     */
    int countByCourseId(Long courseId);
}
