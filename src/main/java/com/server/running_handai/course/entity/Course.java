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

    @Column(name = "name", nullable = false)
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

    @Column(name = "road_condition", columnDefinition = "TEXT")
    private String roadCondition; // 길 상태

    @Column(name = "district", nullable = false)
    private String district; // 행정구역

    @Column(name = "gpx_path", nullable = false)
    private String gpxPath; // gpx 파일

    // TrackPoint와 일대다 관계
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrackPoint> trackPoints = new ArrayList<>();

    // CourseImage와 일대일 관계
    @OneToOne(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private CourseImage courseImage;

    @Builder
    public Course(String externalId, String name, int distance, int duration,
                  CourseLevel level, String tourPoint, String roadCondition,
                  String district, String gpxPath) {
        this.externalId = externalId;
        this.name = name;
        this.distance = distance;
        this.duration = duration;
        this.level = level;
        this.tourPoint = tourPoint;
        this.roadCondition = roadCondition;
        this.district = district;
        this.gpxPath = gpxPath;
    }

    /**
     * API 데이터와 비교하여 Course 엔티티의 내용을 업데이트합니다.
     * 변경이 발생했을 경우에만 true를 반환합니다.
     *
     * @param source 비교 대상이 되는, API 응답으로부터 변환된 Course 객체
     * @return 내용 변경이 있었는지 여부
     */
    public boolean syncWith(Course source) {
        boolean isUpdated = false;

        // 이름, 거리, 시간, 난이도 등 주요 정보가 변경되었는지 확인하고 업데이트
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
        if (!this.district.equals(source.getDistrict())) {
            this.district = source.getDistrict();
            isUpdated = true;
        }
        if (!this.gpxPath.equals(source.getGpxPath())) {
            this.gpxPath = source.getGpxPath();
            isUpdated = true;
        }

        return isUpdated;
    }

    // ==== 연관관계 편의 메서드 ==== //
    public void setCourseImage(CourseImage courseImage) {
        this.courseImage = courseImage;
        if (courseImage != null) {
            courseImage.setCourse(this);
        }
    }
}
