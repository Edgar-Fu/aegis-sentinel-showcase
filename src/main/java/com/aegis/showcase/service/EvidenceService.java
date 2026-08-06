package com.aegis.showcase.service;

import com.aegis.showcase.model.GovernanceDecision;
import com.aegis.showcase.model.GovernanceInputs;
import com.aegis.showcase.model.ReplayResult;
import com.aegis.showcase.model.SourceObservation;
import com.aegis.showcase.model.TamperCheckResult;
import com.aegis.showcase.util.Hashing;
import com.aegis.showcase.util.Io;
import com.aegis.showcase.util.Json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class EvidenceService {
    private final Path evidenceRoot;
    private final PublicGovernanceService governanceService;

    public EvidenceService(Path projectRoot, PublicGovernanceService governanceService) {
        this.evidenceRoot = projectRoot.resolve("runtime/evidence").normalize();
        this.governanceService = governanceService;
    }

    public EvidenceReceipt persist(
            String analysisId,
            String scenario,
            Instant createdAt,
            List<SourceObservation> observations,
            GovernanceInputs governanceInputs,
            GovernanceDecision decision) throws IOException {
        Map<String, Object> canonical = Json.map(
                "analysisId", analysisId,
                "createdAt", createdAt,
                "decision", decision,
                "observations", observations,
                "policyVersion", PublicPolicy.VERSION,
                "scenario", scenario,
                "showcaseOnly", true);
        byte[] evidenceBytes = (Json.write(canonical) + "\n").getBytes(StandardCharsets.UTF_8);
        String evidenceSha = Hashing.sha256(evidenceBytes);
        String evidenceId = "EVD-" + evidenceSha.substring(0, 16).toUpperCase();
        Path directory = evidenceRoot.resolve(evidenceId);
        Files.createDirectories(directory);

        Properties snapshot = new Properties();
        snapshot.setProperty("analysisId", analysisId);
        snapshot.setProperty("scenario", scenario);
        snapshot.setProperty("createdAt", createdAt.toString());
        snapshot.setProperty("policyVersion", PublicPolicy.VERSION);
        snapshot.setProperty("inputValid", Boolean.toString(governanceInputs.inputValid()));
        snapshot.setProperty("anyIntegrityFailure", Boolean.toString(governanceInputs.anyIntegrityFailure()));
        snapshot.setProperty("sourceCount", Integer.toString(governanceInputs.sourceCount()));
        snapshot.setProperty("detectionsPresent", Boolean.toString(governanceInputs.detectionsPresent()));
        snapshot.setProperty("averageCalibratedConfidence", Double.toString(governanceInputs.averageCalibratedConfidence()));
        snapshot.setProperty("consensusStatus", governanceInputs.consensusStatus());
        snapshot.setProperty("minimumIntegrityScore", Double.toString(governanceInputs.minimumIntegrityScore()));
        snapshot.setProperty("expectedRoute", decision.finalRoute());
        snapshot.setProperty("expectedTrust", Double.toString(decision.trustScore()));
        snapshot.setProperty("expectedReasonsBase64", Base64.getEncoder().encodeToString(
                String.join("\n", decision.reasons()).getBytes(StandardCharsets.UTF_8)));
        byte[] snapshotBytes = storeProperties(snapshot, "Aegis Sentinel public showcase replay snapshot");
        String snapshotSha = Hashing.sha256(snapshotBytes);
        String packageSha = Hashing.sha256(evidenceSha + "\n" + snapshotSha + "\n" + PublicPolicy.VERSION);

        Properties manifest = new Properties();
        manifest.setProperty("evidenceId", evidenceId);
        manifest.setProperty("evidenceSha256", evidenceSha);
        manifest.setProperty("snapshotSha256", snapshotSha);
        manifest.setProperty("packageSha256", packageSha);
        manifest.setProperty("policyVersion", PublicPolicy.VERSION);
        manifest.setProperty("format", "aegis-public-showcase-evidence-v1");
        byte[] manifestBytes = storeProperties(manifest, "Aegis Sentinel public showcase evidence manifest");

        writeImmutable(directory.resolve("evidence.json"), evidenceBytes);
        writeImmutable(directory.resolve("replay.properties"), snapshotBytes);
        writeImmutable(directory.resolve("manifest.properties"), manifestBytes);
        return new EvidenceReceipt(evidenceId, evidenceSha, projectRelative(directory.resolve("evidence.json")));
    }

    public ReplayResult replay(String evidenceId) throws IOException {
        Path directory = safeDirectory(evidenceId);
        Properties manifest = load(directory.resolve("manifest.properties"));
        byte[] evidenceBytes = Files.readAllBytes(directory.resolve("evidence.json"));
        byte[] snapshotBytes = Files.readAllBytes(directory.resolve("replay.properties"));
        String actualEvidenceSha = Hashing.sha256(evidenceBytes);
        String actualSnapshotSha = Hashing.sha256(snapshotBytes);
        String expectedEvidenceSha = required(manifest, "evidenceSha256");
        String expectedSnapshotSha = required(manifest, "snapshotSha256");
        String expectedPackageSha = required(manifest, "packageSha256");
        String actualPackageSha = Hashing.sha256(actualEvidenceSha + "\n" + actualSnapshotSha + "\n" + PublicPolicy.VERSION);

        List<String> notes = new ArrayList<>();
        if (!expectedEvidenceSha.equals(actualEvidenceSha)) notes.add("evidence.json checksum mismatch");
        if (!expectedSnapshotSha.equals(actualSnapshotSha)) notes.add("replay.properties checksum mismatch");
        if (!expectedPackageSha.equals(actualPackageSha)) notes.add("evidence package checksum mismatch");
        if (!notes.isEmpty()) {
            return new ReplayResult(evidenceId, Instant.now(), "FAILED", "NOT_RUN", null, null, 0.0, 0.0, notes);
        }

        Properties snapshot = load(snapshotBytes);
        GovernanceInputs inputs = new GovernanceInputs(
                Boolean.parseBoolean(required(snapshot, "inputValid")),
                Boolean.parseBoolean(required(snapshot, "anyIntegrityFailure")),
                Integer.parseInt(required(snapshot, "sourceCount")),
                Boolean.parseBoolean(required(snapshot, "detectionsPresent")),
                Double.parseDouble(required(snapshot, "averageCalibratedConfidence")),
                required(snapshot, "consensusStatus"),
                Double.parseDouble(required(snapshot, "minimumIntegrityScore")));
        GovernanceDecision replayed = governanceService.evaluate(inputs);
        String expectedRoute = required(snapshot, "expectedRoute");
        double expectedTrust = Double.parseDouble(required(snapshot, "expectedTrust"));
        boolean routeMatch = expectedRoute.equals(replayed.finalRoute());
        boolean trustMatch = Math.abs(expectedTrust - replayed.trustScore()) < 0.0001;
        String replayStatus = routeMatch && trustMatch ? "MATCH" : "DIVERGENCE";
        notes.add(routeMatch ? "Route matched the stored public policy result." : "Route diverged from stored result.");
        notes.add(trustMatch ? "Trust score matched." : "Trust score diverged.");
        return new ReplayResult(evidenceId, Instant.now(), "PASS", replayStatus,
                expectedRoute, replayed.finalRoute(), expectedTrust, replayed.trustScore(), notes);
    }

    public TamperCheckResult tamperCheck(String evidenceId) throws IOException {
        Path directory = safeDirectory(evidenceId);
        Properties manifest = load(directory.resolve("manifest.properties"));
        byte[] original = Files.readAllBytes(directory.resolve("evidence.json"));
        byte[] modified = original.clone();
        if (modified.length > 8) modified[modified.length / 2] ^= 0x01;
        String expected = required(manifest, "evidenceSha256");
        String changed = Hashing.sha256(modified);
        boolean accepted = expected.equals(changed);
        return new TamperCheckResult(evidenceId, expected, changed, accepted,
                accepted ? "UNEXPECTED_MATCH" : "TAMPER_DETECTED",
                accepted
                        ? "The simulated modification unexpectedly preserved the checksum."
                        : "A one-byte modification changed the SHA-256 digest, so replay would be refused.");
    }

    private Path safeDirectory(String evidenceId) {
        if (evidenceId == null || !evidenceId.matches("EVD-[A-F0-9]{16}")) {
            throw new IllegalArgumentException("Invalid evidence ID.");
        }
        Path directory = evidenceRoot.resolve(evidenceId).normalize();
        if (!directory.startsWith(evidenceRoot)) throw new IllegalArgumentException("Invalid evidence path.");
        if (!Files.isDirectory(directory)) throw new IllegalArgumentException("Evidence not found: " + evidenceId);
        return directory;
    }

    private void writeImmutable(Path target, byte[] bytes) throws IOException {
        if (Files.exists(target)) {
            byte[] existing = Files.readAllBytes(target);
            if (!java.util.Arrays.equals(existing, bytes)) {
                throw new IOException("Immutable evidence conflict at " + target.getFileName());
            }
            return;
        }
        Io.atomicWrite(target, bytes);
    }

    private Properties load(Path path) throws IOException {
        return load(Files.readAllBytes(path));
    }

    private Properties load(byte[] bytes) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = new java.io.ByteArrayInputStream(bytes)) {
            properties.load(in);
        }
        return properties;
    }

    private byte[] storeProperties(Properties properties, String comment) throws IOException {
        // Properties.store includes a timestamp, so write sorted deterministic key/value lines ourselves.
        StringBuilder out = new StringBuilder("# ").append(comment).append('\n');
        properties.stringPropertyNames().stream().sorted().forEach(key ->
                out.append(escape(key)).append('=').append(escape(properties.getProperty(key))).append('\n'));
        return out.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    }

    private String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing evidence field: " + key);
        return value;
    }

    private String projectRelative(Path path) {
        Path current = Path.of("").toAbsolutePath().normalize();
        Path absolute = path.toAbsolutePath().normalize();
        return absolute.startsWith(current) ? current.relativize(absolute).toString().replace('\\', '/') : absolute.toString();
    }

    public record EvidenceReceipt(String evidenceId, String sha256, String path) {}
}
