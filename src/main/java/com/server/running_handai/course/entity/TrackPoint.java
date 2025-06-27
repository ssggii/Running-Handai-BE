package com.server.running_handai.course.entity;

import com.server.running_handai.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "track_point")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackPoint extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "track_point_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "lat", nullable = false)
    private double lat; // 위도

    @Column(name = "lon", nullable = false)
    private double lon; // 경도

    @Column(name = "ele", nullable = false)
    private double ele; // 고도

    @Column(name = "sequence", nullable = false)
    private int sequence; // 좌표 순서

    @Builder
    public TrackPoint(double lat, double lon, double ele, int sequence) {
        this.lat = lat;
        this.lon = lon;
        this.ele = ele;
        this.sequence = sequence;
    }

    // ==== 연관관계 편의 메서드 ==== //
    public void setCourse(Course course) {
        this.course = course;
        course.getTrackPoints().add(this);
    }
}
