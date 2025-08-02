package com.server.running_handai.domain.course.entity;

import com.server.running_handai.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "spot_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "spot_img_id")
    private Long spotImageId;

    @Column(name = "img_url", nullable = false)
    private String imgUrl; // s3 url

    @Column(name = "original_url", nullable = false)
    private String originalUrl; // [국문 관광정보 API]에서 제공하는 이미지 url

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id", unique = true, nullable = false)
    private Spot spot;

    @Builder
    public SpotImage(String imgUrl, String originalUrl) {
        this.imgUrl = imgUrl;
        this.originalUrl = originalUrl;
    }

    // ==== 연관관계 편의 메서드 ==== //
    protected void setSpot(Spot spot) {
        this.spot = spot;
    }
}
