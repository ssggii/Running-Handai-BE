package com.server.running_handai.bookmark.service;

import com.server.running_handai.bookmark.entity.Bookmark;
import com.server.running_handai.bookmark.repository.BookmarkRepository;
import com.server.running_handai.course.entity.Course;
import com.server.running_handai.course.repository.CourseRepository;
import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.global.response.exception.BusinessException;
import com.server.running_handai.member.entity.Member;
import com.server.running_handai.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final MemberRepository memberRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public void registerBookmark(Long memberId, Long courseId) {
        // 회원과 코스 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ResponseCode.COURSE_NOT_FOUND));

        // 이미 북마크한 코스인지 확인
        if (bookmarkRepository.existsByMemberAndCourse(member, course)) {
            throw new BusinessException(ResponseCode.ALREADY_BOOKMARKED);
        }

        // 북마크 생성 및 저장
        Bookmark bookmark = Bookmark.builder().member(member).course(course).build();
        bookmarkRepository.save(bookmark);
    }

    @Transactional
    public void deleteBookmark(Long memberId, Long courseId) {
        // 회원과 코스 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ResponseCode.COURSE_NOT_FOUND));

        // 삭제할 북마크 조회
        Bookmark bookmark = bookmarkRepository.findByMemberAndCourse(member, course)
                .orElseThrow(() -> new BusinessException(ResponseCode.BOOKMARK_NOT_FOUND));

        // 북마크 삭제
        bookmarkRepository.delete(bookmark);
    }
}
