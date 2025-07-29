package com.server.running_handai.domain.review.entity;

import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.member.entity.Member;
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
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@Table(name = "review")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @Column(name = "stars", nullable = false)
    private Double stars;

    @Size(max = 2000)
    @Column(name = "contents", nullable = false, length = 2000)
    private String contents;

    // Course와 다대일 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Course course;

    // Member와 다대일 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Member writer;

    @Builder
    public Review(Double stars, String contents) {
        this.stars = stars;
        this.contents = contents;
    }

    // ==== 연관관계 편의 메서드 ==== //
    public void setCourse(Course course) {
        // 기존 Course와의 연관관계 제거
        if (this.course != null) {
            this.course.getReviews().remove(this);
        }
        // 새로운 Course와의 연관관계 설정
        this.course = course;
        // 새로운 Course의 리뷰 목록에 자신을 추가
        if (course != null) {
            course.getReviews().add(this);
        }
    }

    public void setWriter(Member writer) {
        // 기존 Member와의 연관관계 제거
        if (this.writer != null) {
            this.writer.getReviews().remove(this);
        }
        // 새로운 Member와의 연관관계 설정
        this.writer = writer;
        // 새로운 Member의 리뷰 목록에 자신을 추가
        if (writer != null) {
            writer.getReviews().add(this);
        }
    }

}
