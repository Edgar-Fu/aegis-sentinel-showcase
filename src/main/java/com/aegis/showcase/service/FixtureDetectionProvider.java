package com.aegis.showcase.service;

import com.aegis.showcase.model.BoundingBox;
import com.aegis.showcase.model.Detection;

import java.util.List;
import java.util.Map;

/**
 * Reproducible detector adapter for the bundled synthetic fixtures.
 * It intentionally does not expose the private real-model adapter.
 */
public final class FixtureDetectionProvider {
    private static final String TRUSTED_A = "7ad0e7c0aad079f4e2ff8823740ae2cf68249cdff700f00cbdb2d0469959fc06";
    private static final String TRUSTED_B = "a33d06ab6a4a2653e4b906e9e14245362482af8ffe9141e535ac5dcc1ec2a8b3";
    private static final String CONFLICT_B = "74a9ae723168351dfe6a11d6a7c2eaf70c7ae93dde82c1d55b825adef3a7c58a";

    private final PublicCalibrationService calibration = new PublicCalibrationService();

    public DetectionResult detect(String sha256) {
        List<RawDetection> raw = switch (sha256) {
            case TRUSTED_A -> List.of(
                    new RawDetection("worker", 0.93, new BoundingBox(0.27, 0.31, 0.08, 0.47)),
                    new RawDetection("forklift", 0.89, new BoundingBox(0.40, 0.24, 0.25, 0.58)),
                    new RawDetection("safety_barrier", 0.84, new BoundingBox(0.68, 0.61, 0.19, 0.21)),
                    new RawDetection("exit_clear", 0.82, new BoundingBox(0.85, 0.25, 0.11, 0.24)));
            case TRUSTED_B -> List.of(
                    new RawDetection("worker", 0.91, new BoundingBox(0.26, 0.31, 0.08, 0.47)),
                    new RawDetection("forklift", 0.87, new BoundingBox(0.41, 0.24, 0.25, 0.58)),
                    new RawDetection("safety_barrier", 0.83, new BoundingBox(0.69, 0.61, 0.19, 0.21)),
                    new RawDetection("exit_clear", 0.81, new BoundingBox(0.85, 0.25, 0.11, 0.24)));
            case CONFLICT_B -> List.of(
                    new RawDetection("forklift", 0.90, new BoundingBox(0.41, 0.24, 0.25, 0.58)),
                    new RawDetection("spill", 0.86, new BoundingBox(0.64, 0.71, 0.17, 0.18)),
                    new RawDetection("blocked_exit", 0.88, new BoundingBox(0.85, 0.25, 0.11, 0.24)));
            default -> List.of();
        };
        List<Detection> detections = raw.stream()
                .map(item -> new Detection(item.label(), item.confidence(), calibration.calibrate(item.confidence()), item.box()))
                .toList();
        String mode = detections.isEmpty() ? "NO_PUBLIC_MODEL_ADAPTER" : "CONTROLLED_FIXTURE_ADAPTER";
        return new DetectionResult(mode, detections);
    }

    private record RawDetection(String label, double confidence, BoundingBox box) {}
    public record DetectionResult(String mode, List<Detection> detections) {}
}
