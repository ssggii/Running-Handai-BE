package com.server.running_handai.domain.bookmark.repository;

import com.server.running_handai.domain.bookmark.dto.BookmarkedCourseInfoDto;
import com.server.running_handai.domain.bookmark.entity.Bookmark;
import com.server.running_handai.domain.bookmark.dto.BookmarkCountDto;
import com.server.running_handai.domain.course.entity.Area;
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
    boolean existsByCourseIdAndMemberId(Long courseId, Long memberId);

    // 회원과 코스로 북마크 조회
    Optional<Bookmark> findByMemberAndCourse(Member member, Course course);

    // 코스 ID로 북마크 수 조회
    int countByCourseId(Long courseId);

    // 회원 ID로 북마크 수 조회
    int countByMemberId(Long memberId);

    // 코스 ID별 북마크 수 조회
    @Query("SELECT new com.server.running_handai.domain.bookmark.dto.BookmarkCountDto(b.course.id, COUNT(b)) "
            + "FROM Bookmark b "
            + "WHERE b.course.id "
            + "IN :courseIds "
            + "GROUP BY b.course.id")
    List<BookmarkCountDto> countByCourseIdIn(@Param("courseIds") List<Long> courseIds);

    // 코스 목록 중에서 사용자가 북마크한 코스 조회
    @Query("SELECT b.course.id FROM Bookmark b WHERE b.course.id IN :courseIds AND b.member.id = :memberId")
    Set<Long> findBookmarkedCourseIdsByMember(@Param("courseIds") List<Long> courseIds, @Param("memberId") Long memberId);

    // 사용자가 북마크한 모든 코스 조회
    @Query("SELECT "
            + "b.id AS bookmarkId, "
            + "c.id AS courseId, "
            + "ci.imgUrl AS thumbnailUrl, "
            + "c.distance AS rawDistance, "
            + "c.duration AS duration, "
            + "c.maxElevation AS rawMaxElevation, "
            + "true AS isBookmarked, "
            + "(SELECT count(b2.id) FROM Bookmark b2 WHERE b2.course.id = c.id) AS bookmarkCount " // 총 북마크 수 계산
            + "FROM Bookmark b "
            + "LEFT JOIN b.course c "
            + "LEFT JOIN c.courseImage ci "
            + "WHERE b.member.id = :memberId "
            + "ORDER BY b.createdAt DESC "
    )
    List<BookmarkedCourseInfoDto> findBookmarkedCoursesByMemberId(Long memberId);

    // 사용자가 북마크한 코스 중에서 특정 지역의 코스만 조회
    @Query("SELECT "
            + "b.id AS bookmarkId, "
            + "c.id AS courseId, "
            + "ci.imgUrl AS thumbnailUrl, "
            + "c.distance AS rawDistance, "
            + "c.duration AS duration, "
            + "c.maxElevation AS rawMaxElevation, "
            + "true AS isBookmarked, "
            + "(SELECT count(b2.id) FROM Bookmark b2 WHERE b2.course.id = c.id) AS bookmarkCount " // 총 북마크 수 계산
            + "FROM Bookmark b "
            + "LEFT JOIN b.course c "
            + "LEFT JOIN c.courseImage ci "
            + "WHERE b.member.id = :memberId "
            + "AND c.area = :area "
            + "ORDER BY b.createdAt DESC "
    )
    List<BookmarkedCourseInfoDto> findBookmarkedCoursesByMemberIdAndArea(Long memberId, Area area);

}
