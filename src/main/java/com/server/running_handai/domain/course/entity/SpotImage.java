package com.server.running_handai.domain.course.entity;

import com.server.running_handai.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id", unique = true, nullable = false)
    private Spot spot;
}
