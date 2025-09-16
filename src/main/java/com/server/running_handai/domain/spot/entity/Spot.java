package com.server.running_handai.domain.spot.entity;

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
    private SpotCategory spotCategory; // 카테고리

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
                SpotCategory spotCategory, double lat, double lon) {
        this.externalId = externalId;
        this.name = name;
        this.address = address;
        this.description = description;
        this.spotCategory = spotCategory;
        this.lat = lat;
        this.lon = lon;
    }

    /**
     * API 데이터와 비교하여 Spot 엔티티의 필드를 업데이트합니다.
     * 변경이 발생했을 경우에만 true를 반환합니다.
     *
     * @param source 비교 대상이 되는, API 응답으로부터 변환된 Spot 객체
     * @return 내용 변경이 있었는지 여부
     */
    public boolean syncWith(Spot source) {
        boolean isUpdated = false;

        if (!this.name.equals(source.getName())) {
            this.name = source.getName();
            isUpdated = true;
        }
        if (!this.address.equals(source.getAddress())) {
            this.address = source.getAddress();
            isUpdated = true;
        }
        if (!this.description.equals(source.getDescription())) {
            this.description = source.getDescription();
            isUpdated = true;
        }
        if (this.spotCategory != source.getSpotCategory()) {
            this.spotCategory = source.getSpotCategory();
            isUpdated = true;
        }
        if (this.lat != source.getLat()) {
            this.lat = source.getLat();
            isUpdated = true;
        }
        if (this.lon != source.getLon()) {
            this.lon = source.getLon();
            isUpdated = true;
        }

        return isUpdated;
    }

    // ==== 연관관계 편의 메서드 ==== //
    public void setSpotImage(SpotImage spotImage) {
        this.spotImage = spotImage;
        if (spotImage != null) {
            spotImage.setSpot(this);
        }
    }
}