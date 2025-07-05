package com.server.running_handai.course.entity;

import com.server.running_handai.global.entity.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
    private int distance; // 코스 전체 거리(km)

    @Column(name = "duration", nullable = false)
    private int duration; // 소요 시간(분)

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private CourseLevel level; // 난이도

    @Column(name = "tour_point", columnDefinition = "TEXT")
    private String tourPoint; // 코스 내 주요 관광지 정보

    @Enumerated(EnumType.STRING)
    @Column(name = "area", nullable = false)
    private Area area;

    @Column(name = "gpx_path", nullable = false)
    private String gpxPath; // gpx 파일

    @Setter
    @Column(columnDefinition = "POINT SRID 4326", name = "start_point")
    private Point startPoint; // 시작 포인트

    @Column(name = "max_ele")
    private Double maxElevation; // 최대 고도

    @Column(name = "min_ele")
    private Double minElevation; // 최소 고도

    // CourseRoadCondition과 일대다 관계
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoadCondition> roadConditions = new ArrayList<>(); // 길 상태


    // TrackPoint와 일대다 관계
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrackPoint> trackPoints = new ArrayList<>();

    // CourseImage와 일대일 관계
    @OneToOne(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private CourseImage courseImage; // 썸네일 이미지

    @Builder
    public Course(String externalId, String name, int distance, int duration,
                  CourseLevel level, String tourPoint, Area area, String gpxPath,
                  Point startPoint, Double maxElevation, Double minElevation) {
        this.externalId = externalId;
        this.name = name;
        this.distance = distance;
        this.duration = duration;
        this.level = level;
        this.tourPoint = tourPoint;
        this.area = area;
        this.gpxPath = gpxPath;
        this.startPoint = startPoint;
        this.maxElevation = maxElevation;
        this.minElevation = minElevation;
    }

    /**
     * gpx 파일 경로를 제외하고, API 데이터와 비교하여 Course 엔티티의 필드를 업데이트합니다.
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
        if (this.tourPoint != null && !this.tourPoint.equals(source.getTourPoint())) {
            this.tourPoint = source.getTourPoint();
            isUpdated = true;
        }
        if (this.area != source.getArea()) {
            this.area = source.getArea();
            isUpdated = true;
        }
        return isUpdated;
    }

    /**
     * API 데이터와 비교하여 Course 엔티티의 gpx 파일 경로를 업데이트합니다.
     * 변경이 발생했을 경우에만 true를 반환합니다.
     * @param source 비교 대상이 되는 API 응답으로 받은 코스의 gpx 파일 경로
     * @return 내용 변경이 있었는지 여부
     */
    public boolean syncGpxPathWith(String source) {
        boolean isUpdated = false;
        if (!this.gpxPath.equals(source)) {
            this.gpxPath = source;
        }
        return isUpdated;
    }

    public void updateElevation(Double minElevation, Double maxElevation) {
        this.minElevation = minElevation;
        this.maxElevation = maxElevation;
    }

    public void updateGpxPath(String gpxPath) {
        this.gpxPath = gpxPath;
    }

    // ==== 연관관계 편의 메서드 ==== //
    public void updateCourseImage(CourseImage courseImage) {
        this.courseImage = courseImage;
        if (courseImage != null) {
            courseImage.setCourse(this);
        }
    }
}
