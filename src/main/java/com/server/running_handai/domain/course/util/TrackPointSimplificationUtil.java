package com.server.running_handai.domain.course.util;

import com.server.running_handai.domain.course.dto.SequenceTrackPointDto;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TrackPointSimplificationUtil {
    /**
     * RDP 알고리즘을 사용하여 트랙 포인트 리스트를 단순화합니다.
     *
     * @param trackPoints 단순화할 트랙 포인트 리스트
     * @param tolerance RDP 알고리즘의 허용 오차값
     * @param geometryFactory 기하학 객체 팩토리
     * @return 단순화된 트랙 포인트 리스트 (sequence 재부여됨)
     */
    public static List<SequenceTrackPointDto> simplifyTrackPoints(
            List<SequenceTrackPointDto> trackPoints,
            double tolerance,
            GeometryFactory geometryFactory
    ) {
        // Dto를 Coordinate[] 변환
        Coordinate[] coordinates = trackPoints.stream()
                .map(dto -> new Coordinate(dto.lon(), dto.lat(), dto.ele()))
                .toArray(Coordinate[]::new);

        // LineString 생성 및 RDP 알고리즘 적용
        LineString originalLine = geometryFactory.createLineString(coordinates);
        DouglasPeuckerSimplifier simplifier = new DouglasPeuckerSimplifier(originalLine);
        simplifier.setDistanceTolerance(tolerance);
        LineString simplifiedLine = (LineString) simplifier.getResultGeometry();

        // 단순화된 좌표를 DTO로 변환하며 sequence 재부여
        AtomicInteger index = new AtomicInteger(1);
        return Arrays.stream(simplifiedLine.getCoordinates())
                .map(c -> SequenceTrackPointDto.sequenceTrackPointDto(c, index.getAndIncrement()))
                .toList();
    }
}