package com.server.running_handai.domain.course.dto;

import com.server.running_handai.domain.course.entity.TrackPoint;
import org.locationtech.jts.geom.Coordinate;

public record SequenceTrackPointDto(
        double lat,
        double lon,
        double ele,
        int sequence
) {
    public static SequenceTrackPointDto sequenceTrackPointDto(TrackPoint trackPoint) {
        return new SequenceTrackPointDto(
                trackPoint.getLat(),
                trackPoint.getLon(),
                trackPoint.getEle(),
                trackPoint.getSequence()
        );
    }

    public static SequenceTrackPointDto sequenceTrackPointDto(Coordinate coordinate, int sequence) {
        return new SequenceTrackPointDto(coordinate.getY(), coordinate.getX(), coordinate.getZ(), sequence);
    }
}
