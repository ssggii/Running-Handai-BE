package com.server.running_handai.bookmark.repository;

import com.server.running_handai.bookmark.entity.Bookmark;
import com.server.running_handai.course.entity.Course;
import com.server.running_handai.member.entity.Member;
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

}
