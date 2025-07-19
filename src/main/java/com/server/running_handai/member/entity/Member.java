package com.server.running_handai.member.entity;

import com.server.running_handai.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "member",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "provider_id"}),
                @UniqueConstraint(columnNames = {"nickname"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "provider_id", nullable = false)
    private String providerId; // 플랫폼에서 받아오는 회원 식별자

    @Column(name = "email", nullable = false, length = 100)
    private String email; // 이메일

    @Column(name = "nickname", nullable = false, length = 20)
    private String nickname; // 닉네임

    @Column(name = "refresh_token")
    private String refreshToken; // 리프래시 토큰

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private Provider provider; // 인증 제공자

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role; // 역할

    @Builder
    public Member(String providerId, String email, String nickname, Provider provider, Role role) {
        this.providerId = providerId;
        this.email = email;
        this.nickname = nickname;
        this.provider = provider;
        this.role = role;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}