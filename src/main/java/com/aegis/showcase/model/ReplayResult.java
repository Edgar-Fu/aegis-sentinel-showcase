package com.aegis.showcase.model;

import java.time.Instant;
import java.util.List;

public record ReplayResult(
        String evidenceId,
        Instant replayedAt,
        String integrityStatus,
        String replayStatus,
        String expectedRoute,
        String replayedRoute,
        double expectedTrustScore,
        double replayedTrustScore,
        List<String> notes) {
    public ReplayResult {
        notes = List.copyOf(notes);
    }
}
