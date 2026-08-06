package com.aegis.showcase.service;

import com.aegis.showcase.model.AnalysisResult;
import com.aegis.showcase.model.Detection;
import com.aegis.showcase.model.GovernanceDecision;
import com.aegis.showcase.model.GovernanceInputs;
import com.aegis.showcase.model.IntegrityReport;
import com.aegis.showcase.model.SourceObservation;
import com.aegis.showcase.util.Hashing;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AnalysisService {
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private static final Set<String> SAFETY_CRITICAL_LABELS = Set.of("spill", "blocked_exit");

    private final ImageIntegrityService integrityService = new ImageIntegrityService();
    private final FixtureDetectionProvider detectionProvider = new FixtureDetectionProvider();
    private final PublicGovernanceService governanceService;
    private final EvidenceService evidenceService;

    public AnalysisService(Path projectRoot) {
        this.governanceService = new PublicGovernanceService();
        this.evidenceService = new EvidenceService(projectRoot, governanceService);
    }

    public AnalysisResult analyzeScenario(String scenario, List<FrameInput> frames) throws IOException {
        if (frames == null || frames.isEmpty()) throw new IllegalArgumentException("At least one frame is required.");
        Instant createdAt = Instant.now();
        List<SourceObservation> observations = new ArrayList<>();
        for (FrameInput frame : frames) observations.add(analyzeFrame(frame));

        String consensusStatus = consensus(observations);
        boolean anyIntegrityFailure = observations.stream().anyMatch(o -> !"PASS".equals(o.integrity().status()));
        List<Detection> allDetections = observations.stream().flatMap(o -> o.detections().stream()).toList();
        double averageConfidence = allDetections.stream().mapToDouble(Detection::calibratedConfidence).average().orElse(0.0);
        double minimumIntegrity = observations.stream().mapToDouble(o -> o.integrity().score()).min().orElse(0.0);
        GovernanceInputs inputs = new GovernanceInputs(
                true,
                anyIntegrityFailure,
                observations.size(),
                !allDetections.isEmpty(),
                round3(averageConfidence),
                consensusStatus,
                round2(minimumIntegrity));
        GovernanceDecision decision = governanceService.evaluate(inputs);
        String seed = scenario + createdAt + observations.stream().map(SourceObservation::sha256).reduce("", String::concat);
        String analysisId = Hashing.shortId("ANL", seed);
        EvidenceService.EvidenceReceipt receipt = evidenceService.persist(
                analysisId, scenario, createdAt, observations, inputs, decision);
        return new AnalysisResult(analysisId, scenario, createdAt, PublicPolicy.VERSION,
                observations, decision, receipt.evidenceId(), receipt.sha256(), receipt.path(), true);
    }

    public AnalysisResult analyzeUpload(String sourceId, String fileName, byte[] bytes) throws IOException {
        return analyzeScenario("custom-upload", List.of(new FrameInput(sourceId, fileName, bytes)));
    }

    public EvidenceService evidenceService() {
        return evidenceService;
    }

    private SourceObservation analyzeFrame(FrameInput frame) throws IOException {
        if (frame.bytes() == null || frame.bytes().length == 0) throw new IllegalArgumentException("Image bytes are empty.");
        if (frame.bytes().length > MAX_IMAGE_BYTES) throw new IllegalArgumentException("Image exceeds the 10 MB public-demo limit.");
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(frame.bytes()));
        if (image == null) throw new IllegalArgumentException("The uploaded bytes are not a supported PNG or JPEG image.");
        String sha = Hashing.sha256(frame.bytes());
        IntegrityReport integrity = integrityService.verify(image);
        FixtureDetectionProvider.DetectionResult detected = "PASS".equals(integrity.status())
                ? detectionProvider.detect(sha)
                : new FixtureDetectionProvider.DetectionResult("SKIPPED_AFTER_INTEGRITY_REJECTION", List.of());
        return new SourceObservation(safeText(frame.sourceId(), "source"), safeText(frame.fileName(), "upload.bin"),
                sha, integrity, detected.mode(), detected.detections());
    }

    private String consensus(List<SourceObservation> observations) {
        if (observations.stream().anyMatch(o -> !"PASS".equals(o.integrity().status()))) return "NOT_EVALUATED";
        if (observations.size() < 2) return "SINGLE_SOURCE";
        Set<String> baseline = labels(observations.get(0));
        boolean criticalDifference = false;
        double minimumJaccard = 1.0;
        for (int i = 1; i < observations.size(); i++) {
            Set<String> current = labels(observations.get(i));
            Set<String> union = new HashSet<>(baseline);
            union.addAll(current);
            Set<String> intersection = new HashSet<>(baseline);
            intersection.retainAll(current);
            double jaccard = union.isEmpty() ? 1.0 : intersection.size() / (double) union.size();
            minimumJaccard = Math.min(minimumJaccard, jaccard);
            Set<String> difference = new HashSet<>(union);
            difference.removeAll(intersection);
            if (difference.stream().anyMatch(SAFETY_CRITICAL_LABELS::contains)) criticalDifference = true;
        }
        return criticalDifference || minimumJaccard < 0.50 ? "CONFLICT" : "AGREEMENT";
    }

    private Set<String> labels(SourceObservation observation) {
        Set<String> labels = new HashSet<>();
        observation.detections().forEach(d -> labels.add(d.label()));
        return labels;
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String cleaned = value.replaceAll("[^A-Za-z0-9._-]", "-");
        return cleaned.length() > 80 ? cleaned.substring(0, 80) : cleaned;
    }

    private static double round2(double value) { return Math.round(value * 100.0) / 100.0; }
    private static double round3(double value) { return Math.round(value * 1000.0) / 1000.0; }

    public record FrameInput(String sourceId, String fileName, byte[] bytes) {
        public static FrameInput fromFile(String sourceId, Path path) throws IOException {
            return new FrameInput(sourceId, path.getFileName().toString(), Files.readAllBytes(path));
        }
    }
}
