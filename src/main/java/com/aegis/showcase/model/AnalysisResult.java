package com.aegis.showcase.model;

import java.time.Instant;
import java.util.List;

public record AnalysisResult(
        String analysisId,
        String scenario,
        Instant createdAt,
        String publicPolicyVersion,
        List<SourceObservation> observations,
        GovernanceDecision decision,
        String evidenceId,
        String evidenceSha256,
        String evidencePath,
        boolean showcaseOnly) {
    public AnalysisResult {
        observations = List.copyOf(observations);
    }
}
