package org.verapdf.tools.cli;

import org.verapdf.pdfa.validation.profiles.Rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Rules {
	private final List<Rule> rules;
		
	public Rules(Set<Rule> rules) {
		this.rules = new ArrayList<>(rules);
		this.rules.sort(new RuleComparator());
	}
		
	public List<Rule> getRules() {
			return rules;
		}
}
