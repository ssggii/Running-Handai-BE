package com.server.running_handai.domain.course.service;

import static com.server.running_handai.global.response.ResponseCode.MEMBER_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.server.running_handai.domain.course.dto.GpxCourseDraft;
import com.server.running_handai.domain.course.entity.Area;
import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.entity.CourseLevel;
import com.server.running_handai.domain.course.event.CourseCreatedEvent;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.domain.course.repository.RoadConditionRepository;
import com.server.running_handai.domain.course.repository.TrackPointJdbcRepository;
import com.server.running_handai.domain.member.entity.Member;
import com.server.running_handai.domain.member.entity.Provider;
import com.server.running_handai.domain.member.entity.Role;
import com.server.running_handai.domain.member.repository.MemberRepository;
import com.server.running_handai.global.response.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CourseCreationSaverTest {

    @InjectMocks
    private CourseCreationSaver courseCreationSaver;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private RoadConditionRepository roadConditionRepository;

    @Mock
    private TrackPointJdbcRepository trackPointJdbcRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final GpxCourseDraft draft = new GpxCourseDraft("광안리-해운대", 5.0, 33, CourseLevel.MEDIUM, Area.HAEUN_GWANGAN,
            "https://s3/gpx/course.gpx", null, 60.0, 10.0, List.of(), List.of("설명1", "설명2"), List.of());

    @Test
    @DisplayName("회원 코스를 저장하면 작성자와 썸네일을 연결하고 코스 생성 이벤트를 발행한다")
    void saveMemberCourse_publishesEvent() {
        // given
        Member member = Member.builder().nickname("nickname1").providerId("providerId1")
                .provider(Provider.GOOGLE).email("email1").role(Role.USER).build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        when(courseRepository.save(courseCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        courseCreationSaver.saveMemberCourse(1L, draft, "https://s3/image/thumb.jpg", true);

        // then
        Course saved = courseCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("광안리-해운대");
        assertThat(saved.getCreator()).isEqualTo(member);
        assertThat(saved.getCourseImage().getImgUrl()).isEqualTo("https://s3/image/thumb.jpg");

        ArgumentCaptor<CourseCreatedEvent> eventCaptor = ArgumentCaptor.forClass(CourseCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().isInsideBusan()).isTrue();
        verify(trackPointJdbcRepository).batchInsert(saved.getId(), draft.trackPoints());
    }

    @Test
    @DisplayName("회원이 없으면 저장하지 않고 이벤트도 발행하지 않는다")
    void saveMemberCourse_memberNotFound() {
        // given
        when(memberRepository.findById(999L)).thenReturn(Optional.empty());

        // when, then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> courseCreationSaver.saveMemberCourse(999L, draft, "https://s3/image/thumb.jpg", false));
        assertThat(exception.getResponseCode()).isEqualTo(MEMBER_NOT_FOUND);
        verify(courseRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
