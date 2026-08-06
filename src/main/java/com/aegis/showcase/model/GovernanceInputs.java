package com.aegis.showcase.model;

public record GovernanceInputs(
        boolean inputValid,
        boolean anyIntegrityFailure,
        int sourceCount,
        boolean detectionsPresent,
        double averageCalibratedConfidence,
        String consensusStatus,
        double minimumIntegrityScore) {
}
