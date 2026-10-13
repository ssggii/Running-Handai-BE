package com.server.running_handai.global.util;

import java.util.Locale;

/**
 * 중심점에서 반경 안에 있는 점을 빠짐없이 감싸는 위도·경도 사각형입니다.
 * 반경 검색에서 공간 인덱스(MBRContains)로 후보를 먼저 거르는 1차 필터로 사용합니다.
 *
 * 1차 필터는 넓게 틀리면 후보만 늘고 결과는 정확하지만, 좁게 틀리면 결과에서 코스가 조용히 빠집니다.
 * 그래서 정확한 구면 공식에 여유를 더해 넉넉하게 계산합니다.
 */
public record GeoBoundingBox(double minLat, double maxLat, double minLon, double maxLon) {

    // MySQL ST_Distance_Sphere의 기본 지구 반지름 (1차 필터와 최종 거리 판정이 같은 지구 모델을 쓰도록 맞춤)
    public static final double EARTH_RADIUS_METERS = 6_370_986;

    // 반경에 더하는 여유 비율 (소수 계산 오차로 원을 덜 덮지 않도록)
    private static final double MARGIN_RATIO = 0.01;

    /**
     * 중심점에서 반경 안의 점을 모두 포함하는 사각형을 계산합니다.
     *
     * @param lat 중심점 위도
     * @param lon 중심점 경도
     * @param radiusMeters 반경 (미터)
     * @return 반경에 여유를 더해 계산한 사각형
     */
    public static GeoBoundingBox around(double lat, double lon, double radiusMeters) {
        double angularRadius = radiusMeters * (1 + MARGIN_RATIO) / EARTH_RADIUS_METERS; // 라디안
        double latDelta = Math.toDegrees(angularRadius);

        // 원의 동서 끝점은 중심보다 살짝 극 쪽에 있어, 근사식(angularRadius / cos(lat))은 경도를 덜 덮는다
        double sinLonDelta = Math.sin(angularRadius) / Math.cos(Math.toRadians(lat));
        double lonDelta = Math.toDegrees(Math.asin(Math.min(1.0, sinLonDelta)));

        return new GeoBoundingBox(lat - latDelta, lat + latDelta, lon - lonDelta, lon + lonDelta);
    }

    /**
     * SRID 4326 WKT 다각형으로 변환합니다. MySQL은 SRID 4326 WKT를 "위도 경도" 순서로 읽습니다.
     */
    public String toWkt() {
        return String.format(Locale.ROOT,
                "POLYGON((%.7f %.7f, %.7f %.7f, %.7f %.7f, %.7f %.7f, %.7f %.7f))",
                minLat, minLon,
                minLat, maxLon,
                maxLat, maxLon,
                maxLat, minLon,
                minLat, minLon);
    }

    public boolean contains(double lat, double lon) {
        return minLat <= lat && lat <= maxLat && minLon <= lon && lon <= maxLon;
    }
}
