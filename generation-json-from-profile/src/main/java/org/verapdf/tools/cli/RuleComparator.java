package org.verapdf.tools.cli;

import org.verapdf.pdfa.validation.profiles.Rule;
import org.verapdf.pdfa.validation.profiles.RuleId;

import java.util.Comparator;

public class RuleComparator implements Comparator<Rule> {

	@Override
	public int compare(Rule rule1, Rule rule2) {
		RuleId id1 = rule1.getRuleId();
		RuleId id2 = rule2.getRuleId();
		int c = id1.getSpecification().getId().compareTo(id2.getSpecification().getId());
		if (c != 0) {
			return c;
		}
		c = id1.getClause().compareTo(id2.getClause());
		if (c != 0) {
			return c;
		}
		return id1.getTestNumber() - id2.getTestNumber();
	}
}
