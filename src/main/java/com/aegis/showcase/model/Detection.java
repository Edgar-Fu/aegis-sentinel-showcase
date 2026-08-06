package com.aegis.showcase.model;

public record Detection(String label, double rawConfidence, double calibratedConfidence, BoundingBox box) {
    public Detection {
        if (label == null || label.isBlank()) throw new IllegalArgumentException("Detection label is required.");
        if (!inRange(rawConfidence) || !inRange(calibratedConfidence)) {
            throw new IllegalArgumentException("Detection confidence must be between 0 and 1.");
        }
        if (box == null) throw new IllegalArgumentException("Detection box is required.");
    }

    private static boolean inRange(double value) {
        return Double.isFinite(value) && value >= 0.0 && value <= 1.0;
    }
}
