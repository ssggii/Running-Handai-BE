package com.server.running_handai.member.repository;

import com.server.running_handai.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByProviderId(String providerId);
    boolean existsByNickname(String nickname);
    Optional<Member> findByRefreshToken(String refreshToken);
}
