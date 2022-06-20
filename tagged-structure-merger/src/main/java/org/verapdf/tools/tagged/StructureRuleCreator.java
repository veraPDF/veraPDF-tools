package org.verapdf.tools.tagged;

import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.validation.profiles.*;
import org.verapdf.tools.TaggedPDFHelper;
import org.verapdf.tools.tagged.enums.ChildrenRelation;
import org.verapdf.tools.tagged.enums.PDFVersion;

import java.util.*;

/**
 * @author Maksim Bezrukov
 */
public class StructureRuleCreator {

	private static final String HN = "Hn";
	private static final String CONTENT_ITEM = "content item";
	private static final String STRUCT_TREE_ROOT = "StructTreeRoot";

	private static final String STRUCT_TREE_ROOT_OBJECT = "PDStructTreeRoot";
	private static final String STRUCT_ELEM_FORMAT = "SE%s";

	private static final String FORBIDDEN_DESCRIPTION_FORMAT = "<%s> shall not contain <%s>";
	private static final String FORBIDDEN_ERROR_FORMAT = "Invalid parent-child relationship: <%s> contains <%s>";

	private static final String AT_LEAST_ONE_DESCRIPTION_FORMAT = "<%s> shall contain at least one <%s>";
	private static final String AT_LEAST_ONE_ERROR_FORMAT = "<%s> does not contain <%s> elements";

	private static final String ZERO_OR_ONE_DESCRIPTION_FORMAT = "<%s> shall contain at most one <%s>";
	private static final String ZERO_OR_ONE_ERROR_FORMAT = "<%s> contains more than one <%s>";

	private static final String ONE_DESCRIPTION_FORMAT = "<%s> shall contain exactly one <%s>";
	private static final String ONE_ERROR_FORMAT = "<%s> either doesn't contain or contains more than one <%s>";

	private final PDFVersion pdfVersion;
	private final PDFAFlavour flavour;

	public StructureRuleCreator(PDFVersion pdfVersion) {
		this.pdfVersion = pdfVersion;
		switch (this.pdfVersion) {
			case PDF_1_7:
				this.flavour = PDFAFlavour.PDFA_2_U;
				break;
			case PDF_2_0:
				this.flavour = PDFAFlavour.PDFA_4;
				break;
			default:
				throw new IllegalStateException("Unsupported pdf version");
		}
	}

	public PDFAFlavour getFlavour() {
		return this.flavour;
	}

	public List<Rule> generateRules(List<ParsedRelationStructure> relations) {
		List<Rule> res = new ArrayList<>(relations.size());
		int testNumber = 0;

		// standard structure type requirement
		List<Reference> annex_l_reference = Collections.singletonList(Profiles.referenceFromValues(
				this.pdfVersion.getIso(), "Annex_L"));

		res.add(Profiles.ruleFromValues(
				Profiles.ruleIdFromValues(this.flavour.getPart(), "Annex_L", ++testNumber),
				"SENonStandard",
				false,
				"Every structure element should be mapped to a standard structure type",
				"false",
				Profiles.errorFromValues("Structure element is not mapped to the standard structure type",
						Collections.emptyList()),
				annex_l_reference));
		for (ParsedRelationStructure relation : relations) {
			if (shallProcess(relation)) {
				RuleData data = getRuleData(relation);
				if (data == null) {
					System.err.println("Missing rule for " + relation.getDescriptionString());
					continue;
				}
				RuleId id = Profiles.ruleIdFromValues(this.flavour.getPart(), "Annex_L", ++testNumber);
				ErrorDetails error = Profiles.errorFromValues(data.errorMessage, Collections.emptyList());
				res.add(Profiles.ruleFromValues(id, data.object, false, data.description,
				                                data.test, error, annex_l_reference));
			}
		}
		return res;
	}

	private boolean shallProcess(ParsedRelationStructure relation) {
		String parent = relation.getParent();
		String child = relation.getChild();
		// assuming that 2.0 parent->child relations contains all relations from 1.7
		// first apply 2.0 filter
		if ((!TaggedPDFHelper.getPdf17StandardRoleTypes().contains(parent)
		     && !TaggedPDFHelper.getPdf20StandardRoleTypes().contains(parent)
		     && !parent.equals(HN)
		     && !parent.equals(STRUCT_TREE_ROOT))
		    ||
		    (!TaggedPDFHelper.getPdf17StandardRoleTypes().contains(child)
		     && !TaggedPDFHelper.getPdf20StandardRoleTypes().contains(child)
		     && !child.matches(HN)
		     && !child.equals(CONTENT_ITEM))) {
			System.err.println("Invalid relation " + relation.getDescriptionString());
			return false;
		}

		// any amount relation type does not need any validation
		if (relation.getRelation() == ChildrenRelation.ANY_AMOUNT) {
			return false;
		}

		// if this is 2.0 profile, then all checks has been applied
		if (this.pdfVersion == PDFVersion.PDF_2_0) {
			return true;
		}

		return (TaggedPDFHelper.getPdf17StandardRoleTypes().contains(parent) || parent.equals(HN) || parent.equals(STRUCT_TREE_ROOT))
		       && (TaggedPDFHelper.getPdf17StandardRoleTypes().contains(child) || child.equals(HN) || child.equals(CONTENT_ITEM));
	}

	private RuleData getRuleData(ParsedRelationStructure rel) {
		switch (rel.getRelation()) {
			case FORBIDDEN:
				return constructForbidden(rel);
			case AT_LEAST_ONE:
				return constructAtLeastOne(rel);
			case ZERO_OR_ONE:
				return constructZeroOrOne(rel);
			case ONE:
				return constructOne(rel);
			case FORBIDDEN_FOR_NON_GROUPING_CHILD:
			case DEPENDS_ON_STRUCTURE:
			case RUBY:
			case WARICHU:
			case ANY_AMOUNT:
				// ANY_AMOUNT does not need validation rule
				// shall be filtered before this method, so return null
			default:
				// nothing to do here, in normal case we will not get here
				// as all values shall be defined above
				return null;
		}
	}

	private RuleData constructZeroOrOne(ParsedRelationStructure rel) {
		String child = rel.getChild();
		if (child.equals(CONTENT_ITEM)) {
			return null;
		}

		String childTest = constructChildElemAmountPart(child) + " <= 1";
		String testObj;

		String parent = rel.getParent();
		switch (parent) {
			case STRUCT_TREE_ROOT:
				testObj = STRUCT_TREE_ROOT_OBJECT;
				break;
			default:
				testObj = String.format(STRUCT_ELEM_FORMAT, parent);
		}

		return new RuleData(testObj, childTest,
		                    String.format(ZERO_OR_ONE_DESCRIPTION_FORMAT, parent, child),
		                    String.format(ZERO_OR_ONE_ERROR_FORMAT, parent, child));
	}

	private RuleData constructOne(ParsedRelationStructure rel) {
		String child = rel.getChild();
		if (child.equals(CONTENT_ITEM)) {
			return null;
		}

		String childTest = constructChildElemAmountPart(child) + " == 1";
		String testObj;

		String parent = rel.getParent();
		switch (parent) {
			case STRUCT_TREE_ROOT:
				testObj = STRUCT_TREE_ROOT_OBJECT;
				break;
			default:
				testObj = String.format(STRUCT_ELEM_FORMAT, parent);
		}

		return new RuleData(testObj, childTest,
				String.format(ONE_DESCRIPTION_FORMAT, parent, child),
				String.format(ONE_ERROR_FORMAT, parent, child));
	}

	private RuleData constructAtLeastOne(ParsedRelationStructure rel) {
		String parent = rel.getParent();
		String child = rel.getChild();

		String childTest;
		switch (child) {
			case CONTENT_ITEM:
				childTest = "hasContentItems == true";
				break;
			default:
				childTest = constructChildElemAmountPart(child) + " > 0";
		}

		String testObj;
		switch (parent) {
			case STRUCT_TREE_ROOT:
				testObj = STRUCT_TREE_ROOT_OBJECT;
				break;
			default:
				testObj = String.format(STRUCT_ELEM_FORMAT, parent);
		}

		return new RuleData(testObj, childTest,
		                    String.format(FORBIDDEN_DESCRIPTION_FORMAT, parent, child),
		                    String.format(FORBIDDEN_ERROR_FORMAT, parent, child));
	}

	private RuleData constructForbidden(ParsedRelationStructure rel) {
		String parent = rel.getParent();
		String child = rel.getChild();

		String childTest;
		switch (child) {
			case CONTENT_ITEM:
				childTest = "hasContentItems == false";
				break;
			default:
				childTest = constructChildElemAmountPart(child) + " == 0";
		}

		String testObj;
		switch (parent) {
			case STRUCT_TREE_ROOT:
				testObj = STRUCT_TREE_ROOT_OBJECT;
				break;
			default:
				testObj = String.format(STRUCT_ELEM_FORMAT, parent);
		}

		return new RuleData(testObj, childTest,
		                    String.format(FORBIDDEN_DESCRIPTION_FORMAT, parent, child),
		                    String.format(FORBIDDEN_ERROR_FORMAT, parent, child));
	}

	private String constructChildElemAmountPart(String child) {
		switch (child) {
			case HN:
				switch (this.pdfVersion) {
					case PDF_1_7:
						return "kidsStandardTypes.split('&').filter(elem => /^H[1-6]$/.test(elem)).length";
					case PDF_2_0:
						return "kidsStandardTypes.split('&').filter(elem => /^H[1-9][0-9]*$/.test(elem)).length";
					default:
						throw new IllegalStateException("Unknown pdf version");
				}
			default:
				return String.format("kidsStandardTypes.split('&').filter(elem => elem == '%s').length", child);
		}
	}

	private static class RuleData {
		private String object;
		private String test;
		private String description;
		private String errorMessage;

		RuleData(String object, String test, String description, String errorMessage) {
			this.object = object;
			this.test = test;
			this.description = description;
			this.errorMessage = errorMessage;
		}
	}
}
