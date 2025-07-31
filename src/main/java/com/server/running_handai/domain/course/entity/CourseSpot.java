package com.server.running_handai.domain.course.entity;

import com.server.running_handai.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "course_spot", uniqueConstraints = {
        @UniqueConstraint(
                name = "course_spot_uk",
                columnNames = {"course_id", "spot_id"}
        )
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseSpot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_spot_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id", nullable = false)
    private Spot spot;

    @Builder
    public CourseSpot(Course course, Spot spot) {
        this.course = course;
        this.spot = spot;
    }
}
