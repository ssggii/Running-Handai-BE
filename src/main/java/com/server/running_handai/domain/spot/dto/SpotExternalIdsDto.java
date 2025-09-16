package com.server.running_handai.domain.spot.dto;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public record SpotExternalIdsDto(
        Long courseId,
        String externalIds
) {
    public Set<String> getSpotExternalIds() {
        if (externalIds == null || externalIds.isEmpty()) {
            return Collections.emptySet();
        }

        return Arrays.stream(externalIds.split(","))
                .map(String::trim)
                .filter(externalId -> !externalId.isEmpty())
                .collect(Collectors.toSet());
    }
}