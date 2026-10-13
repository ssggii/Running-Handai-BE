package com.server.running_handai.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GeoBoundingBoxTest {

    private static final double RADIUS_METERS = 5000;

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "해운대해수욕장, 35.1587, 129.1604",
            "서울시청, 37.5665, 126.9780",
            "제주시, 33.4996, 126.5312"
    })
    @DisplayName("반경 경계 위의 모든 방향 점이 사각형 안에 들어간다")
    void containsEveryPointOnCircle(String place, double lat, double lon) {
        GeoBoundingBox box = GeoBoundingBox.around(lat, lon, RADIUS_METERS);

        for (int bearing = 0; bearing < 360; bearing++) {
            double[] point = destination(lat, lon, bearing, RADIUS_METERS);
            assertThat(box.contains(point[0], point[1]))
                    .as("%s에서 방위 %d도, 반경 %.0fm 지점 (%f, %f)", place, bearing, RADIUS_METERS, point[0], point[1])
                    .isTrue();
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "해운대해수욕장, 35.1587, 129.1604",
            "서울시청, 37.5665, 126.9780"
    })
    @DisplayName("사각형은 반경보다 여유(1%) 이상으로 넓지 않다")
    void isNotTooLoose(String place, double lat, double lon) {
        GeoBoundingBox box = GeoBoundingBox.around(lat, lon, RADIUS_METERS);

        double exactLatDelta = Math.toDegrees(RADIUS_METERS / GeoBoundingBox.EARTH_RADIUS_METERS);
        double latDelta = box.maxLat() - lat;

        assertThat(latDelta).isGreaterThan(exactLatDelta).isLessThanOrEqualTo(exactLatDelta * 1.0101);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"해운대해수욕장, 35.1587, 129.1604"})
    @DisplayName("WKT 다각형은 위도 경도 순서로, 닫힌 다각형으로 만든다")
    void toWktUsesLatLonOrder(String place, double lat, double lon) {
        GeoBoundingBox box = GeoBoundingBox.around(lat, lon, RADIUS_METERS);

        String wkt = box.toWkt();

        String firstPoint = String.format(java.util.Locale.ROOT, "%.7f %.7f", box.minLat(), box.minLon());
        assertThat(wkt).startsWith("POLYGON((" + firstPoint + ", ").endsWith(", " + firstPoint + "))");
    }

    /**
     * 구 위에서 시작점으로부터 방위(bearing)와 거리만큼 이동한 지점 (MySQL ST_Distance_Sphere와 같은 지구 반지름)
     */
    private static double[] destination(double lat, double lon, double bearingDegrees, double distanceMeters) {
        double angular = distanceMeters / GeoBoundingBox.EARTH_RADIUS_METERS;
        double bearing = Math.toRadians(bearingDegrees);
        double lat1 = Math.toRadians(lat);
        double lon1 = Math.toRadians(lon);

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(angular)
                + Math.cos(lat1) * Math.sin(angular) * Math.cos(bearing));
        double lon2 = lon1 + Math.atan2(Math.sin(bearing) * Math.sin(angular) * Math.cos(lat1),
                Math.cos(angular) - Math.sin(lat1) * Math.sin(lat2));

        return new double[]{Math.toDegrees(lat2), Math.toDegrees(lon2)};
    }
}
