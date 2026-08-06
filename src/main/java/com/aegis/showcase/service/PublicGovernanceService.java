package com.aegis.showcase.service;

import com.aegis.showcase.model.GovernanceDecision;
import com.aegis.showcase.model.GovernanceInputs;
import com.aegis.showcase.model.TraceStep;

import java.util.ArrayList;
import java.util.List;

public final class PublicGovernanceService {
    public GovernanceDecision evaluate(GovernanceInputs input) {
        List<String> reasons = new ArrayList<>();
        List<TraceStep> trace = new ArrayList<>();
        int order = 1;

        trace.add(step(order++, "INPUT_VALIDATION", input.inputValid() ? "PASS" : "FAIL",
                input.inputValid() ? "Input bytes decoded successfully." : "Input could not be decoded."));
        if (!input.inputValid()) reasons.add("INVALID_INPUT");

        trace.add(step(order++, "INTEGRITY_GATE", input.anyIntegrityFailure() ? "FAIL" : "PASS",
                input.anyIntegrityFailure() ? "At least one source failed public image-integrity checks." : "All sources passed integrity checks."));
        if (input.anyIntegrityFailure()) reasons.add("INTEGRITY_REJECTION");

        trace.add(step(order++, "DETECTION_EVIDENCE", input.detectionsPresent() ? "PASS" : "REVIEW",
                input.detectionsPresent() ? "The public fixture adapter returned evidence." : "No public model evidence is available for this upload."));
        if (!input.detectionsPresent()) reasons.add("DETECTION_EVIDENCE_UNAVAILABLE");

        String consensusOutcome = switch (input.consensusStatus()) {
            case "AGREEMENT" -> "PASS";
            case "CONFLICT" -> "FAIL";
            default -> "REVIEW";
        };
        trace.add(step(order++, "SOURCE_CONSENSUS", consensusOutcome,
                "Consensus status: " + input.consensusStatus() + "."));
        if ("CONFLICT".equals(input.consensusStatus())) reasons.add("SOURCE_CONFLICT");
        if (input.sourceCount() < 2 && !input.anyIntegrityFailure()) reasons.add("SINGLE_SOURCE_ONLY");

        double integrity = clamp01(input.minimumIntegrityScore() / 100.0);
        double consensus = switch (input.consensusStatus()) {
            case "AGREEMENT" -> 1.0;
            case "CONFLICT" -> 0.20;
            case "NOT_EVALUATED" -> 0.0;
            default -> 0.55;
        };
        double trust = round3(0.45 * integrity + 0.35 * input.averageCalibratedConfidence() + 0.20 * consensus);
        boolean trustPass = trust >= PublicPolicy.MIN_AUTO_TRUST
                && input.averageCalibratedConfidence() >= PublicPolicy.MIN_AUTO_CONFIDENCE;
        trace.add(step(order++, "TRUST_GATE", trustPass ? "PASS" : "REVIEW",
                "Illustrative public trust score " + trust + "; auto threshold " + PublicPolicy.MIN_AUTO_TRUST + "."));
        if (!trustPass && !input.anyIntegrityFailure()) reasons.add("TRUST_BELOW_PUBLIC_AUTO_THRESHOLD");

        String status;
        String route;
        if (!input.inputValid() || input.anyIntegrityFailure()) {
            status = "STOP";
            route = "REJECT";
        } else if ("CONFLICT".equals(input.consensusStatus()) || !input.detectionsPresent()
                || input.sourceCount() < 2 || !trustPass) {
            status = "REVIEW";
            route = "ESCALATE";
        } else {
            status = "PASS";
            route = "AUTO_EXECUTE";
        }
        if (reasons.isEmpty()) reasons.add("PUBLIC_POLICY_REQUIREMENTS_SATISFIED");
        trace.add(step(order++, "ROUTE_SELECTION", status, "Final public showcase route: " + route + "."));
        trace.add(step(order, "EVIDENCE_SEAL", "PENDING", "Evidence is canonicalized and sealed after the decision."));

        return new GovernanceDecision(status, route, input.consensusStatus(), trust, reasons, trace);
    }

    private static TraceStep step(int order, String rule, String outcome, String detail) {
        return new TraceStep(order, rule, outcome, detail);
    }

    private static double clamp01(double value) { return Math.max(0.0, Math.min(1.0, value)); }
    private static double round3(double value) { return Math.round(value * 1000.0) / 1000.0; }
}
