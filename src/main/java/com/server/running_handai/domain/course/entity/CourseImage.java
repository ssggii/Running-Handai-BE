package com.server.running_handai.domain.course.entity;

import com.server.running_handai.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "course_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_img_id")
    private Long courseImageId;

    @Column(name = "img_url", nullable = false)
    private String imgUrl; // s3 url

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", unique = true, nullable = false)
    private Course course;

    @Builder
    public CourseImage(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public void updateImageUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    // ==== 연관관계 편의 메서드 ==== //
    protected void setCourse(Course course) {
        this.course = course;
    }
}
