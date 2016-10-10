package org.verapdf.tools.utils;

import org.verapdf.pdfa.results.TestAssertion;

import java.util.Comparator;

/**
 * @author Maksim Bezrukov
 */
public class TestAssertionContextFreeComparator implements Comparator<TestAssertion>{

	@Override
	public int compare(TestAssertion first, TestAssertion second) {
		return getHashWithoutContext(first).compareTo(getHashWithoutContext(second));
	}

	private static String getHashWithoutContext(TestAssertion assertion) {
		return assertion.getRuleId().getClause() + " " + assertion.getRuleId().getTestNumber() + " " + assertion.getStatus();
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + assertion.getOrdinal();
//		result = prime * result + ((assertion.getMessage() == null) ? 0 : assertion.getMessage().hashCode());
//		result = prime * result + ((assertion.getRuleId() == null) ? 0 : assertion.getRuleId().hashCode());
//		result = prime * result + ((assertion.getStatus() == null) ? 0 : assertion.getStatus().hashCode());
//		return result;
	}
}
