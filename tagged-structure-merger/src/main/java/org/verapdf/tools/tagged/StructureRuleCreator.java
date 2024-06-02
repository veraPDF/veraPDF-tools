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

	private static final String CONTENT_ITEMS = CONTENT_ITEM + "s";
	private static final String CONTENT_ITEM_S = CONTENT_ITEM + "(s)";


	private static final String STRUCT_TREE_ROOT = "StructTreeRoot";

	private static final String STRUCT_TREE_ROOT_OBJECT = "PDStructTreeRoot";
	private static final String STRUCT_ELEM_FORMAT = "SE%s";

	private static final String FORBIDDEN_DESCRIPTION_FORMAT = "%s shall not contain %s";
	private static final String FORBIDDEN_ERROR_FORMAT = "%s contains %s";

	private static final String AT_LEAST_ONE_DESCRIPTION_FORMAT = "%s shall contain at least one %s";
	private static final String AT_LEAST_ONE_ERROR_FORMAT = "%s does not contain %s elements";

	private static final String ZERO_OR_ONE_DESCRIPTION_FORMAT = "%s shall contain at most one %s";
	private static final String ZERO_OR_ONE_ERROR_FORMAT = "%s contains more than one %s";

	private static final String ONE_DESCRIPTION_FORMAT = "%s shall contain exactly one %s";
	private static final String ONE_ERROR_FORMAT = "%s either doesn't contain or contains more than one %s";

	private final PDFVersion pdfVersion;
	private final PDFAFlavour flavour;

	public StructureRuleCreator(PDFVersion pdfVersion) {
		this.pdfVersion = pdfVersion;
		switch (this.pdfVersion) {
			case PDF_1_7:
				this.flavour = PDFAFlavour.PDFUA_1;
				break;
			case PDF_2_0:
				this.flavour = PDFAFlavour.PDFUA_2;
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

		res.add(getRuleAboutNotRemappedNonStandardType(annex_l_reference, ++testNumber));

		for (ParsedRelationStructure relation : relations) {
			if (shallProcess(relation)) {
				RuleData data = getRuleData(relation);
				if (data == null) {
					System.err.println("Missing rule for " + relation.getDescriptionString());
					continue;
				}
				RuleId id = Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_32005, "6.2", ++testNumber);
				ErrorDetails error = Profiles.errorFromValues(data.errorMessage, Collections.emptyList());
				res.add(Profiles.ruleFromValues(id, data.object, null, StructureTag.getTags(relation), data.description,
				                                data.test, error, annex_l_reference));
			}
		}
		res.add(getRuleAboutCircularMapping(annex_l_reference, ++testNumber));
		res.add(getRuleAboutRemappedStandardType(annex_l_reference, ++testNumber));
		res.add(getRuleAboutStructTreeRoot(annex_l_reference, ++testNumber));
		res.add(getRuleAboutStructElementParent(annex_l_reference, ++testNumber));
		res.add(getRuleAboutMathMLParent(annex_l_reference, ++testNumber));
		return res;
	}
	
	private Rule getRuleAboutNotRemappedNonStandardType(List<Reference> annex_l_reference, int testNumber) {
		return Profiles.ruleFromValues(
				Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_32005, "6.2", testNumber),
				"SENonStandard",
				null,
				StructureTag.STRUCTURE_TAG.getTag(),
				"Every structure type should be mapped to a standard structure type",
				"isNotMappedToStandardType == false",
				Profiles.errorFromValues("Non-standard structure type %1 is not mapped to a standard type",
						Collections.singletonList(ErrorArgumentImpl.fromValues("namespaceAndTag", null, null))),
				annex_l_reference);
	}

	private Rule getRuleAboutCircularMapping(List<Reference> annex_l_reference, int testNumber) {
		return Profiles.ruleFromValues(
				Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_32005, "6.2", testNumber),
				"SENonStandard",
				null,
				StructureTag.STRUCTURE_TAG.getTag(),
				"A circular mapping shall not exist",
				"circularMappingExist != true",
				Profiles.errorFromValues("A circular mapping exists for %1 structure type",
						Collections.singletonList(ErrorArgumentImpl.fromValues("namespaceAndTag", null, null))),
				annex_l_reference);
	}

	private Rule getRuleAboutRemappedStandardType(List<Reference> annex_l_reference, int testNumber) {
		return Profiles.ruleFromValues(
				Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_32005, "6.2", testNumber),
				"SENonStandard",
				null,
				StructureTag.STRUCTURE_TAG.getTag(),
				"Standard tags shall not be remapped to a non-standard type",
				"remappedStandardType == null",
				Profiles.errorFromValues("The standard structure type %1 is remapped to a non-standard type",
						Collections.singletonList(ErrorArgumentImpl.fromValues("remappedStandardType", null, null))),
				annex_l_reference);
	}
	
	private Rule getRuleAboutStructElementParent(List<Reference> annex_l_reference, int testNumber) {
		return Profiles.ruleFromValues(
				Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_32005, "6.2", testNumber),
				"PDStructElem",
				null,
				StructureTag.STRUCTURE_TAG.getTag(),
				"A structure element dictionary shall contain the P (parent) entry according to ISO 32000-2:2020, 14.7.2, Table 323",
				"containsParent == true",
				Profiles.errorFromValues("A structure element dictionary does not contain the P (parent) entry", Collections.emptyList()),
				annex_l_reference);
	}

	private Rule getRuleAboutMathMLParent(List<Reference> annex_l_reference, int testNumber) {
		return Profiles.ruleFromValues(
				Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_32005, "6.2", testNumber),
				"SEMathMLStructureElement",
				null,
				StructureTag.STRUCTURE_TAG.getTag(),
				"The math structure type shall occur only as a child of a Formula structure element",
				"parentStandardType == 'Formula' || parentStandardType == 'MathML'",
				Profiles.errorFromValues("The math structure type is nested within %1 tag instead of Formula", 
						Collections.singletonList(ErrorArgumentImpl.fromValues("parentStandardType", null, null))),
				annex_l_reference);
	}

	private Rule getRuleAboutStructTreeRoot(List<Reference> annex_l_reference, int testNumber) {
		return Profiles.ruleFromValues(
				Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_32005, "6.2", testNumber),
				"PDDocument",
				null,
				StructureTag.STRUCTURE_TAG.getTag(),
				"The logical structure of the conforming file shall be described by a structure hierarchy rooted " + 
						"in the StructTreeRoot entry of the document catalog dictionary, as described in ISO 32000-2:2020, 14.7",
				"StructTreeRoot_size == 1",
				Profiles.errorFromValues("StructTreeRoot entry is not present in the document catalog", Collections.emptyList()),
				annex_l_reference);
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
				getDescriptionOrErrorMessage(ZERO_OR_ONE_DESCRIPTION_FORMAT, parent, child),
				getDescriptionOrErrorMessage(ZERO_OR_ONE_ERROR_FORMAT, parent, child));
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
				getDescriptionOrErrorMessage(ONE_DESCRIPTION_FORMAT, parent, child),
				getDescriptionOrErrorMessage(ONE_ERROR_FORMAT, parent, child));
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
				getDescriptionOrErrorMessage(AT_LEAST_ONE_DESCRIPTION_FORMAT, parent, child),
				getDescriptionOrErrorMessage(AT_LEAST_ONE_ERROR_FORMAT, parent, child));
	}

	private RuleData constructForbidden(ParsedRelationStructure rel) {
		String parent = rel.getParent();
		String child = rel.getChild();

		String childTest;
		String testObj;
		switch (child) {
			case CONTENT_ITEM:
				childTest = "hasContentItems == false";
				testObj = String.format(STRUCT_ELEM_FORMAT, parent);
				break;
			default:
				if (HN.equals(parent)) {
					childTest = "/^H[1-9][0-9]*$/.test(parentStandardType) == false";
				} else {
					childTest = "parentStandardType != '" + parent + '\'';
				}
				testObj = String.format(STRUCT_ELEM_FORMAT, child);
		}

		return new RuleData(testObj, childTest,
				getDescriptionOrErrorMessage(FORBIDDEN_DESCRIPTION_FORMAT, parent, child),
				getDescriptionOrErrorMessage(FORBIDDEN_ERROR_FORMAT, parent, child));
	}

	private String getDescriptionOrErrorMessage(String description, String parent, String child) {
		String stringChild = getStringRepresentation(child);
		if (CONTENT_ITEM.equals(stringChild)) {
			if (FORBIDDEN_DESCRIPTION_FORMAT.equals(description)) {
				stringChild = CONTENT_ITEMS;
			} else if (FORBIDDEN_ERROR_FORMAT.equals(description)) {
				stringChild = CONTENT_ITEM_S;
			}
		}
		return String.format(description, getStringRepresentation(parent), stringChild);
	}

	private String getStringRepresentation(String string) {
		if (STRUCT_TREE_ROOT.equals(string) || CONTENT_ITEM.equals(string)) {
			return string;
		}
		return "<" + string + ">";
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
