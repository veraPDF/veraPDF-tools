package org.verapdf.tools.utils;

import org.verapdf.pdfa.results.TestAssertion;
import org.verapdf.pdfa.validation.profiles.RuleId;

import java.util.Comparator;

/**
 * @author Maksim Bezrukov
 */
public class TestAssertionContextFreeComparator implements Comparator<TestAssertion>{

	@Override
	public int compare(TestAssertion first, TestAssertion second) {
		return ruleIDWithStatus(first).compareTo(ruleIDWithStatus(second));
	}

	private static String ruleIDWithStatus(TestAssertion assertion) {
		RuleId ruleId = assertion.getRuleId();
		return ruleId.getSpecification().toString() + " " + ruleId.getClause() + " " + ruleId.getTestNumber() + " " + assertion.getStatus();
	}
}
