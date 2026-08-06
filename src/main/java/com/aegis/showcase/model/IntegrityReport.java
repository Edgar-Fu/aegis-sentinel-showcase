package com.aegis.showcase.model;

import java.util.List;
import java.util.Map;

public record IntegrityReport(
        String status,
        double score,
        IntegrityMetrics metrics,
        List<String> failures,
        Map<String, Double> components) {
    public IntegrityReport {
        failures = List.copyOf(failures);
        components = Map.copyOf(components);
    }
}
