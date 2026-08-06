package com.aegis.showcase;

import com.aegis.showcase.model.AnalysisResult;
import com.aegis.showcase.model.ReplayResult;
import com.aegis.showcase.model.TamperCheckResult;
import com.aegis.showcase.service.AnalysisService;
import com.aegis.showcase.service.ScenarioService;
import com.aegis.showcase.util.Hashing;
import com.aegis.showcase.util.Json;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class ShowcaseSelfTest {
    private int passed;

    public static void main(String[] args) throws Exception {
        Path root = Path.of(System.getProperty("aegis.root", ".")).toAbsolutePath().normalize();
        ShowcaseSelfTest suite = new ShowcaseSelfTest();
        suite.run(root);
        System.out.println("PUBLIC SHOWCASE VERIFICATION PASS - " + suite.passed + " checks");
    }

    private void run(Path root) throws Exception {
        AnalysisService analysis = new AnalysisService(root);
        ScenarioService scenarios = new ScenarioService(root, analysis);

        AnalysisResult trusted = scenarios.run("trusted");
        check("AUTO_EXECUTE".equals(trusted.decision().finalRoute()), "trusted scenario auto-executes");
        check("AGREEMENT".equals(trusted.decision().consensusStatus()), "trusted scenario reaches agreement");
        check(trusted.observations().size() == 2, "trusted scenario uses two sources");
        check(trusted.observations().stream().allMatch(o -> "PASS".equals(o.integrity().status())), "trusted sources pass integrity");
        check(Files.isRegularFile(root.resolve(trusted.evidencePath())), "trusted evidence is persisted");

        ReplayResult replay = analysis.evidenceService().replay(trusted.evidenceId());
        check("PASS".equals(replay.integrityStatus()), "replay verifies evidence integrity");
        check("MATCH".equals(replay.replayStatus()), "replay matches stored decision");

        TamperCheckResult tamper = analysis.evidenceService().tamperCheck(trusted.evidenceId());
        check(!tamper.accepted(), "one-byte modification is refused");
        check("TAMPER_DETECTED".equals(tamper.status()), "tamper result is explicit");

        AnalysisResult conflict = scenarios.run("conflict");
        check("ESCALATE".equals(conflict.decision().finalRoute()), "conflict scenario escalates");
        check("CONFLICT".equals(conflict.decision().consensusStatus()), "conflict is classified separately");
        check(conflict.decision().reasons().contains("SOURCE_CONFLICT"), "conflict reason is preserved");

        AnalysisResult rejected = scenarios.run("rejected");
        check("REJECT".equals(rejected.decision().finalRoute()), "blurred input is rejected");
        check(rejected.observations().get(0).integrity().failures().contains("BLUR"), "blur failure is measured");
        check("SKIPPED_AFTER_INTEGRITY_REJECTION".equals(rejected.observations().get(0).detectorMode()), "detector is skipped after rejection");

        byte[] arbitrary = Files.readAllBytes(root.resolve("sample_images/trusted_source_a.png"));
        AnalysisResult upload = analysis.analyzeUpload("custom-source", "renamed.png", arbitrary);
        check("ESCALATE".equals(upload.decision().finalRoute()), "single-source custom upload escalates");

        String canonicalA = Json.write(Map.of("b", 2, "a", 1));
        String canonicalB = Json.write(Map.of("a", 1, "b", 2));
        check(canonicalA.equals(canonicalB), "JSON map keys are canonicalized");
        check(Hashing.sha256(canonicalA).equals(Hashing.sha256(canonicalB)), "canonical hashes are stable");
    }

    private void check(boolean condition, String description) {
        if (!condition) throw new AssertionError("FAILED: " + description);
        passed++;
        System.out.println("PASS: " + description);
    }
}
