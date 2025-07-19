package com.server.running_handai.domain.course.dto;

import com.server.running_handai.domain.course.entity.TrackPoint;

public record TrackPointDto(
        double lat,
        double lon,
        double ele
) {
    public static TrackPointDto from(TrackPoint trackPoint) {
        return new TrackPointDto(
                trackPoint.getLat(),
                trackPoint.getLon(),
                trackPoint.getEle()
        );
    }
}
