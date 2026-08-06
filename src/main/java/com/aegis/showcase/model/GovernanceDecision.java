package com.aegis.showcase.model;

import java.util.List;

public record GovernanceDecision(
        String governanceStatus,
        String finalRoute,
        String consensusStatus,
        double trustScore,
        List<String> reasons,
        List<TraceStep> trace) {
    public GovernanceDecision {
        reasons = List.copyOf(reasons);
        trace = List.copyOf(trace);
    }
}
