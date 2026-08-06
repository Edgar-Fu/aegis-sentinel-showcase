package com.aegis.showcase.model;

import java.util.List;

public record SourceObservation(
        String sourceId,
        String fileName,
        String sha256,
        IntegrityReport integrity,
        String detectorMode,
        List<Detection> detections) {
    public SourceObservation {
        detections = List.copyOf(detections);
    }
}
