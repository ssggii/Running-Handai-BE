package com.server.running_handai.domain.course.entity;

import com.server.running_handai.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "spot")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Spot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "spot_id")
    private Long id;

    @Column(name = "external_id", unique = true, nullable = false)
    private String externalId; // 국문 관광정보 API의 장소 식별자

    @Column(name = "name", nullable = false)
    private String name; // 이름

    @Column(name = "address", nullable = false)
    private String address; // 주소

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // 설명

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Category category; // 카테고리

    @Column(name = "lat", nullable = false)
    private double lat; // 위도

    @Column(name = "lon", nullable = false)
    private double lon; // 경도

    // CourseSpot과 일대다 관계
    @OneToMany(mappedBy = "spot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseSpot> courseSpots = new ArrayList<>();

    // SpotImage와 일대일 관계
    @OneToOne(mappedBy = "spot", cascade = CascadeType.ALL, orphanRemoval = true)
    private SpotImage spotImage;

    @Builder
    public Spot(String externalId, String name, String address, String description,
                Category category, double lat, double lon) {
        this.externalId = externalId;
        this.name = name;
        this.address = address;
        this.description = description;
        this.category = category;
        this.lat = lat;
        this.lon = lon;
    }

    // ==== 연관관계 편의 메서드 ==== //
    public void setSpotImage(SpotImage spotImage) {
        this.spotImage = spotImage;
        if (spotImage != null) {
            spotImage.setSpot(this);
        }
    }
}