package com.server.running_handai.global.util;

import com.server.running_handai.domain.course.dto.TrackPointDto;
import com.server.running_handai.domain.course.entity.TrackPoint;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;

import java.util.Arrays;
import java.util.List;

public class TrackPointSimplificationUtil {
    /**
     * RDP 알고리즘을 사용하여 트랙 포인트 리스트를 단순화합니다.
     *
     * @param trackPoints 단순화할 트랙 포인트 리스트
     * @param tolerance RDP 알고리즘의 허용 오차값
     * @param geometryFactory 기하학 객체 팩토리
     * @return 단순화된 트랙 포인트 DTO 리스트
     */
    public static List<TrackPointDto> simplifyTrackPoints(
            List<TrackPoint> trackPoints,
            double tolerance,
            GeometryFactory geometryFactory
    ) {
        // TrackPoint를 Coordinate[]로 변환
        Coordinate[] coordinates = trackPoints.stream()
                .map(point -> new Coordinate(point.getLon(), point.getLat(), point.getEle()))
                .toArray(Coordinate[]::new);

        return applyRdpAlgorithm(coordinates, tolerance, geometryFactory);
    }

    /**
     * RDP 알고리즘을 사용하여 트랙 포인트 DTO 리스트를 단순화합니다.
     *
     * @param trackPointDtos 단순화할 트랙 포인트 DTO 리스트
     * @param tolerance RDP 알고리즘의 허용 오차값
     * @param geometryFactory 기하학 객체 팩토리
     * @return 단순화된 트랙 포인트 DTO 리스트
     */
    public static List<TrackPointDto> simplifyTrackPointDtos(
            List<TrackPointDto> trackPointDtos,
            double tolerance,
            GeometryFactory geometryFactory
    ) {
        // TrackPoint DTO를 Coordinate[]로 변환
        Coordinate[] coordinates = trackPointDtos.stream()
                .map(point -> new Coordinate(point.lon(), point.lat(), point.ele()))
                .toArray(Coordinate[]::new);

        return applyRdpAlgorithm(coordinates, tolerance, geometryFactory);
    }

    /**
     * 좌표 정보 배열로 변환된 트랙 포인트 리스트를 RDP 알고리즘을 사용하여 단순화하고 DTO로 변환합니다.
     *
     * @param coordinates 좌표 정보 배열
     * @param tolerance RDP 알고리즘의 허용 오차값
     * @param geometryFactory 기하학 객체 팩토리
     * @return 단순화된 트랙 포인트 DTO 리스트
     */
    private static List<TrackPointDto> applyRdpAlgorithm(
            Coordinate[] coordinates,
            double tolerance,
            GeometryFactory geometryFactory
    ) {
        // LineString 생성 및 RDP 알고리즘 적용
        LineString originalLine = geometryFactory.createLineString(coordinates);
        DouglasPeuckerSimplifier simplifier = new DouglasPeuckerSimplifier(originalLine);
        simplifier.setDistanceTolerance(tolerance);
        LineString simplifiedLine = (LineString) simplifier.getResultGeometry();

        // 단순화된 좌표를 DTO로 변환
        return Arrays.stream(simplifiedLine.getCoordinates())
                .map(TrackPointDto::from)
                .toList();
    }
}