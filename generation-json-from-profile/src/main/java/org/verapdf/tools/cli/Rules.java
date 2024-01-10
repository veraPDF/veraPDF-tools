package org.verapdf.tools.cli;

import org.verapdf.pdfa.validation.profiles.Profiles;
import org.verapdf.pdfa.validation.profiles.Rule;

import java.util.*;

public class Rules {
	private final SortedSet<Rule> rules;
		
	public Rules(Set<Rule> rules) {
		this.rules = new TreeSet<>(new Profiles.RuleComparator());
		this.rules.addAll(rules);
	}
		
	public SortedSet<Rule> getRules() {
			return rules;
		}
}
