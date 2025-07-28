package com.server.running_handai.domain.bookmark.repository;

import com.server.running_handai.domain.bookmark.entity.Bookmark;
import com.server.running_handai.domain.course.dto.BookmarkCountDto;
import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.member.entity.Member;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    // memberId와 courseId로 북마크 여부 확인
    boolean existsByCourseIdAndMemberId(Long memberId, Long courseId);

    // 회원과 코스로 북마크 조회
    Optional<Bookmark> findByMemberAndCourse(Member member, Course course);

    // 코스 ID로 북마크 수 조회
    int countByCourseId(Long courseId);

    // 코스 ID별 북마크 수 조회
    @Query("SELECT new com.server.running_handai.domain.course.dto.BookmarkCountDto(b.course.id, COUNT(b)) "
            + "FROM Bookmark b "
            + "WHERE b.course.id "
            + "IN :courseIds "
            + "GROUP BY b.course.id")
    List<BookmarkCountDto> countByCourseIdIn(@Param("courseIds") List<Long> courseIds);

    // 사용자가 북마크한 코스 ID 목록 조회
    @Query("SELECT b.course.id FROM Bookmark b WHERE b.course.id IN :courseIds AND b.member.id = :memberId")
    Set<Long> findBookmarkedCourseIdsByMember(@Param("courseIds") List<Long> courseIds, @Param("memberId") Long memberId);

}
