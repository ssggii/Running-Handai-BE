package com.server.running_handai.domain.course.service;

import static com.server.running_handai.global.response.ResponseCode.MEMBER_NOT_FOUND;

import com.server.running_handai.domain.course.dto.GpxCourseDraft;
import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.entity.CourseImage;
import com.server.running_handai.domain.course.entity.RoadCondition;
import com.server.running_handai.domain.course.event.CourseCreatedEvent;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.domain.course.repository.RoadConditionRepository;
import com.server.running_handai.domain.course.repository.TrackPointJdbcRepository;
import com.server.running_handai.domain.member.entity.Member;
import com.server.running_handai.domain.member.repository.MemberRepository;
import com.server.running_handai.global.response.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 코스 생성의 DB 저장만 트랜잭션으로 처리합니다.
 * 외부 API 호출은 이 클래스 밖(트랜잭션 밖)에서 끝내고 결과만 넘겨받습니다. 이 클래스에는 외부 API 호출을 넣지 않습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseCreationSaver {

    private final CourseRepository courseRepository;
    private final RoadConditionRepository roadConditionRepository;
    private final TrackPointJdbcRepository trackPointJdbcRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 코스, 길 상태, 트랙포인트를 저장합니다. (관리자 코스 생성)
     *
     * @param draft 외부 API 호출까지 끝난 코스 정보
     * @return 저장된 코스
     */
    @Transactional
    public Course saveCourse(GpxCourseDraft draft) {
        return persist(draft);
    }

    /**
     * 회원 코스를 저장하고 썸네일을 연결한 뒤 코스 생성 이벤트를 발행합니다.
     * 이벤트는 커밋 후 실행되는 리스너(@TransactionalEventListener)가 받으므로 트랜잭션 안에서 발행합니다.
     *
     * @param memberId 생성한 회원 ID
     * @param draft 외부 API 호출까지 끝난 코스 정보
     * @param thumbnailUrl S3에 업로드한 썸네일 URL
     * @param isInsideBusan 부산 코스 여부 (이벤트로 전달)
     * @return 저장된 코스 ID
     */
    @Transactional
    public Long saveMemberCourse(Long memberId, GpxCourseDraft draft, String thumbnailUrl, boolean isInsideBusan) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND));

        Course course = persist(draft);
        course.setCreator(member);
        course.updateCourseImage(CourseImage.builder().imgUrl(thumbnailUrl).build());

        eventPublisher.publishEvent(new CourseCreatedEvent(course.getId(), isInsideBusan));
        log.info("코스 생성 이벤트 발행. courseId: {}, isInsideBusan: {}", course.getId(), isInsideBusan);
        return course.getId();
    }

    private Course persist(GpxCourseDraft draft) {
        Course course = Course.builder()
                .name(draft.name())
                .distance(draft.distance())
                .duration(draft.duration())
                .level(draft.level())
                .area(draft.area())
                .gpxPath(draft.gpxPath())
                .startPoint(draft.startPoint())
                .maxElevation(draft.maxElevation())
                .minElevation(draft.minElevation())
                .build();
        draft.themes().forEach(course::addTheme);

        courseRepository.save(course);
        log.info("[GPX 코스 생성] Course 저장 완료: ID={}", course.getId());

        List<RoadCondition> roadConditions = draft.roadConditions().stream()
                .map(description -> RoadCondition.builder().course(course).description(description).build())
                .toList();
        roadConditionRepository.saveAll(roadConditions);
        log.info("[GPX 코스 생성] RoadCondition {}개 저장 완료", roadConditions.size());

        trackPointJdbcRepository.batchInsert(course.getId(), draft.trackPoints());
        log.info("[GPX 코스 생성] TrackPoint {}개 저장 완료", draft.trackPoints().size());
        return course;
    }
}
