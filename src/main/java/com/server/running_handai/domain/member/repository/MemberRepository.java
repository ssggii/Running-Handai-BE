package com.server.running_handai.domain.member.repository;

import com.server.running_handai.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {
    /**
     * Provider Id로 사용자를 조회합니다.
     */
    Optional<Member> findByProviderId(String providerId);

    /**
     * 리프래시 토큰으로 사용자를 조회합니다.
     */
    Optional<Member> findByRefreshToken(String refreshToken);

    /**
     * 닉네임 중복 여부를 확인합니다.
     */
    boolean existsByNickname(String nickname);
}
