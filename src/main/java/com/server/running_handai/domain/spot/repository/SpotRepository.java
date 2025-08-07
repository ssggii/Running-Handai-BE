package com.server.running_handai.domain.spot.repository;

import com.server.running_handai.domain.spot.entity.Spot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SpotRepository extends JpaRepository<Spot, Long> {
    List<Spot> findByExternalIdIn(Collection<String> externalIds);
}