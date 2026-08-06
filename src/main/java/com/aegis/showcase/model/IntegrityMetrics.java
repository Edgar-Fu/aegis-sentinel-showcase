package com.aegis.showcase.model;

public record IntegrityMetrics(
        double sharpness,
        double brightness,
        double overexposedRatio,
        double underexposedRatio,
        int width,
        int height) {
}
