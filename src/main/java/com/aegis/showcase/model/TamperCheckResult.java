package com.aegis.showcase.model;

public record TamperCheckResult(
        String evidenceId,
        String originalChecksum,
        String modifiedChecksum,
        boolean accepted,
        String status,
        String explanation) {
}
