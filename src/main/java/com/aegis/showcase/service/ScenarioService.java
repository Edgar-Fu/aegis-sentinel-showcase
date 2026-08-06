package com.aegis.showcase.service;

import com.aegis.showcase.model.AnalysisResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScenarioService {
    private final Path sampleRoot;
    private final AnalysisService analysisService;

    public ScenarioService(Path projectRoot, AnalysisService analysisService) {
        this.sampleRoot = projectRoot.resolve("sample_images").normalize();
        this.analysisService = analysisService;
    }

    public AnalysisResult run(String scenario) throws IOException {
        return switch (scenario) {
            case "trusted" -> analysisService.analyzeScenario("trusted", List.of(
                    frame("warehouse-camera-a", "trusted_source_a.png"),
                    frame("warehouse-camera-b", "trusted_source_b.png")));
            case "conflict" -> analysisService.analyzeScenario("conflict", List.of(
                    frame("warehouse-camera-a", "trusted_source_a.png"),
                    frame("warehouse-camera-b", "conflict_source_b.png")));
            case "rejected" -> analysisService.analyzeScenario("rejected", List.of(
                    frame("warehouse-camera-a", "rejected_blur.png")));
            default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
        };
    }

    public Map<String, Object> catalog() {
        Map<String, Object> catalog = new LinkedHashMap<>();
        catalog.put("trusted", Map.of(
                "title", "Corroborated trusted evidence",
                "expectedRoute", "AUTO_EXECUTE",
                "description", "Two synthetic sources agree and pass the public showcase gates."));
        catalog.put("conflict", Map.of(
                "title", "Independent-source conflict",
                "expectedRoute", "ESCALATE",
                "description", "The second source introduces safety-critical conflicting evidence."));
        catalog.put("rejected", Map.of(
                "title", "Integrity rejection",
                "expectedRoute", "REJECT",
                "description", "A blurred source is rejected before any detector evidence is accepted."));
        return catalog;
    }

    private AnalysisService.FrameInput frame(String sourceId, String fileName) throws IOException {
        Path path = sampleRoot.resolve(fileName).normalize();
        if (!path.startsWith(sampleRoot)) throw new IllegalArgumentException("Unsafe sample path.");
        return AnalysisService.FrameInput.fromFile(sourceId, path);
    }
}
