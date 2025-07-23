package com.server.running_handai.domain.course.dto;

import com.server.running_handai.domain.course.entity.TrackPoint;
import org.locationtech.jts.geom.Coordinate;

public record TrackPointDto(
        double lat,
        double lon,
        double ele
) {
    public static TrackPointDto from(TrackPoint trackPoint) {
        return new TrackPointDto(trackPoint.getLat(), trackPoint.getLon(), trackPoint.getEle()
        );
    }

    public static TrackPointDto from(Coordinate coordinate) {
        return new TrackPointDto(coordinate.getY(), coordinate.getX(), coordinate.getZ());
    }
}
