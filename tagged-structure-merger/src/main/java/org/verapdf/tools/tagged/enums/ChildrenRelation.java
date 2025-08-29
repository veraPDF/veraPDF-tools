package org.verapdf.tools.tagged.enums;

/**
 * @author Maksim Bezrukov
 */
public enum ChildrenRelation {
	FORBIDDEN("∅"),
	FORBIDDEN_FOR_NON_GROUPING_CHILD("∅*"),
	ANY_AMOUNT("0..n"),
	ANY_AMOUNT_IF_PARENT_IS_GROUPING("0..n*"),
	AT_LEAST_ONE("1..n"),
	ZERO_OR_ONE("0..1"),
	ZERO_OR_ONE_IF_PARENT_IS_GROUPING("0..1*"),
	DEPENDS_ON_STRUCTURE("‡"),
	RUBY("[a]"),
	WARICHU("[b]"),
	ONE("1");

	private String name;
	private boolean isCommonRule;

	ChildrenRelation(String name) {
		this.name = name;
	}

	public static ChildrenRelation fromName(String name) {
		for (ChildrenRelation relation : ChildrenRelation.values()) {
			if (relation.getName().equals(name)) {
				return relation;
			}
		}
		throw new IllegalArgumentException("Children relation with specified name " + name + " doesn't exist.");
	}

	public String getName() {
		return name;
	}
}
