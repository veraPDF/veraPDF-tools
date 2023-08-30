package org.verapdf.tools.policy.generator;

import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.validation.profiles.Profiles;
import org.verapdf.pdfa.validation.profiles.RuleId;

public class RuleInfo implements Comparable<RuleInfo> {
    private final RuleId ruleId;
    private final Integer failedChecks;

    public RuleInfo(String clause, Integer testNumber, Integer failedChecks) {
        this.ruleId = Profiles.ruleIdFromValues(PDFAFlavour.Specification.NO_STANDARD, clause, testNumber);
        this.failedChecks = failedChecks;
    }

    public RuleId getRuleId() {
        return ruleId;
    }

    public Integer getFailedChecks() {
        return failedChecks;
    }

    public int compareTo(RuleInfo ruleInfo) {
        return new Profiles.RuleIdComparator().compare(ruleId, ruleInfo.ruleId);
    }
}
