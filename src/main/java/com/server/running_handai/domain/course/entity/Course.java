package com.server.running_handai.domain.course.entity;

import com.server.running_handai.domain.member.entity.Member;
import com.server.running_handai.domain.spot.entity.CourseSpot;
import com.server.running_handai.domain.review.entity.Review;
import com.server.running_handai.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.locationtech.jts.geom.Point;

@Entity
@Getter
@Table(name = "course")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long id;

    @Column(name = "external_id", unique = true)
    private String externalId; // 두루누비 API의 코스 식별자

    @Column(name = "name", unique = true, nullable = false)
    private String name; // 코스 이름

    @Column(name = "distance", nullable = false)
    private double distance; // 코스 전체 거리(km)

    @Column(name = "duration", nullable = false)
    private int duration; // 소요 시간(분)

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private CourseLevel level; // 난이도

    @Enumerated(EnumType.STRING)
    @Column(name = "area", nullable = false)
    private Area area; // 지역

    @Column(name = "gpx_path", nullable = false)
    private String gpxPath; // gpx 파일

    @Setter
    @Column(columnDefinition = "GEOMETRY", name = "start_point", nullable = false)
    private Point startPoint; // 시작 포인트

    @Column(name = "max_ele", nullable = false)
    private Double maxElevation; // 최대 고도

    @Column(name = "min_ele", nullable = false)
    private Double minElevation; // 최소 고도

    @ElementCollection(fetch = FetchType.LAZY)
    @Column(name = "theme")
    @Enumerated(EnumType.STRING)
    private List<Theme> themes = new ArrayList<>(); // 테마

    // CourseImage와 일대일 관계
    @OneToOne(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private CourseImage courseImage; // 썸네일 이미지

    // CourseRoadCondition과 일대다 관계
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoadCondition> roadConditions = new ArrayList<>(); // 길 상태

    // TrackPoint와 일대다 관계
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrackPoint> trackPoints = new ArrayList<>();

    // Review와 일대다 관계
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    // CourseSpot과 일대다 관계
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseSpot> courseSpots = new ArrayList<>();

    // Member와 다대일 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Member creator;

    @Builder
    public Course(String externalId, String name, double distance, int duration,
                  CourseLevel level, Area area, String gpxPath,
                  Point startPoint, Double maxElevation, Double minElevation) {
        this.externalId = externalId;
        this.name = name;
        this.distance = distance;
        this.duration = duration;
        this.level = level;
        this.area = area;
        this.gpxPath = gpxPath;
        this.startPoint = startPoint;
        this.maxElevation = maxElevation;
        this.minElevation = minElevation;
    }

    /**
     * API 데이터와 비교하여 Course 엔티티의 필드를 업데이트합니다.
     * 변경이 발생했을 경우에만 true를 반환합니다.
     * @param source 비교 대상이 되는, API 응답으로부터 변환된 Course 객체
     * @return 내용 변경이 있었는지 여부
     */
    public boolean syncWith(Course source) {
        boolean isUpdated = false;

        if (!this.name.equals(source.getName())) {
            this.name = source.getName();
            isUpdated = true;
        }
        if (this.distance != source.getDistance()) {
            this.distance = source.getDistance();
            isUpdated = true;
        }
        if (this.duration != source.getDuration()) {
            this.duration = source.getDuration();
            isUpdated = true;
        }
        if (this.level != source.getLevel()) {
            this.level = source.getLevel();
            isUpdated = true;
        }
        if (this.area != source.getArea()) {
            this.area = source.getArea();
            isUpdated = true;
        }

        if (this.gpxPath != null && !this.gpxPath.equals(source.getGpxPath())) {
            this.gpxPath = source.getGpxPath();
            isUpdated = true;
        }
        return isUpdated;
    }

    // ==== 연관관계 편의 메서드 ==== //
    public void updateCourseImage(CourseImage courseImage) {
        this.courseImage = courseImage;
        if (courseImage != null) {
            courseImage.setCourse(this);
        }
    }

    public void updateElevation(Double minElevation, Double maxElevation) {
        this.minElevation = minElevation;
        this.maxElevation = maxElevation;
    }

    public void addTheme(Theme theme) {
        this.themes.add(theme);
    }

    public void removeTheme(Theme theme) {
        this.themes.remove(theme);
    }

    public void setCreator(Member creator) {
        // 기존 Member와의 연관관계 제거
        if (this.creator != null) {
            this.creator.getCourses().remove(this);
        }
        // 새로운 Member와의 연관관계 설정
        this.creator = creator;
        // 새로운 Member의 코스 목록에 자신을 추가
        if (creator != null) {
            creator.getCourses().add(this);
        }
    }

    public void removeCreator() {
        if (this.creator != null) {
            // 기존 creator의 courses 리스트에서 현재 Course 제거
            this.creator.getCourses().remove(this);
            // 현재 Course의 creator 필드를 null로 설정
            this.creator = null;
        }
    }

}
