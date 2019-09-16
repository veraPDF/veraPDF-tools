package org.verapdf.tools.tagged;

import org.verapdf.tools.tagged.enums.ChildrenRelation;

import java.util.Objects;

/**
 * @author Maksim Bezrukov
 */
public class ParsedRelationStructure {

	private String parent;
	private String child;
	// parent index and child index are used only to sort relations according to file.
	// So they are not needed in equals and hashcode
	private int parentIndex;
	private int childIndex;
	private ChildrenRelation relation;

	public ParsedRelationStructure(String parent, String child, int parentIndex, int childIndex, ChildrenRelation relation) {
		this.parent = parent;
		this.child = child;
		this.parentIndex = parentIndex;
		this.childIndex = childIndex;
		this.relation = relation;
	}

	public String getParent() {
		return parent;
	}

	public String getChild() {
		return child;
	}

	public int getParentIndex() {
		return parentIndex;
	}

	public int getChildIndex() {
		return childIndex;
	}

	public ChildrenRelation getRelation() {
		return relation;
	}

	public String getDescriptionString() {
		return parent + "-['" + relation.getName() + "']->" + child;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ParsedRelationStructure)) {
			return false;
		}
		ParsedRelationStructure that = (ParsedRelationStructure) o;
		return Objects.equals(parent, that.parent) &&
		       Objects.equals(child, that.child) &&
		       relation == that.relation;
	}

	@Override
	public int hashCode() {
		return Objects.hash(parent, child, relation);
	}
}
