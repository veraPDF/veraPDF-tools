package org.verapdf.tools.tagged;

import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.validation.profiles.*;
import org.verapdf.tools.tagged.enums.ChildrenRelation;
import org.verapdf.tools.tagged.enums.PDFVersion;

import java.util.*;

/**
 * @author Maksim Bezrukov
 */
public class StructureRuleCreator {

	private static Set<String> PDF_1_7_STANDARD_ROLE_TYPES;
	private static Set<String> PDF_2_0_STANDARD_ROLE_TYPES;

	static {
		Set<String> tempSet = new HashSet<>();
		// Common standard structure types for PDF 1.7 and 2.0
		tempSet.add("Document");
		tempSet.add("Part");
		tempSet.add("Div");
		tempSet.add("Caption");
		tempSet.add("THead");
		tempSet.add("TBody");
		tempSet.add("TFoot");
		tempSet.add("H");
		tempSet.add("P");
		tempSet.add("L");
		tempSet.add("LI");
		tempSet.add("Lbl");
		tempSet.add("LBody");
		tempSet.add("Table");
		tempSet.add("TR");
		tempSet.add("TH");
		tempSet.add("TD");
		tempSet.add("Span");
		tempSet.add("Link");
		tempSet.add("Annot");
		tempSet.add("Ruby");
		tempSet.add("Warichu");
		tempSet.add("Figure");
		tempSet.add("Formula");
		tempSet.add("Form");
		tempSet.add("RB");
		tempSet.add("RT");
		tempSet.add("RP");
		tempSet.add("WT");
		tempSet.add("WP");

		Set<String> pdf_1_7 = new HashSet<>(tempSet);

		// Standart structure types present in 1.7
		pdf_1_7.add("Art");
		pdf_1_7.add("Sect");
		pdf_1_7.add("BlockQuote");
		pdf_1_7.add("TOC");
		pdf_1_7.add("TOCI");
		pdf_1_7.add("Index");
		pdf_1_7.add("NonStruct");
		pdf_1_7.add("Private");
		pdf_1_7.add("Quote");
		pdf_1_7.add("Note");
		pdf_1_7.add("Reference");
		pdf_1_7.add("BibEntry");
		pdf_1_7.add("Code");
		pdf_1_7.add("H1");
		pdf_1_7.add("H2");
		pdf_1_7.add("H3");
		pdf_1_7.add("H4");
		pdf_1_7.add("H5");
		pdf_1_7.add("H6");

		Set<String> pdf_2_0 = new HashSet<>(tempSet);

		pdf_2_0.add("DocumentFragment");
		pdf_2_0.add("Aside");
		pdf_2_0.add("Title");
		pdf_2_0.add("FENote");
		pdf_2_0.add("Sub");
		pdf_2_0.add("Em");
		pdf_2_0.add("Strong");
		pdf_2_0.add("Artifact");

		PDF_1_7_STANDARD_ROLE_TYPES = Collections.unmodifiableSet(pdf_1_7);
		PDF_2_0_STANDARD_ROLE_TYPES = Collections.unmodifiableSet(pdf_2_0);
	}

	private static final String HN = "Hn";
	private static final String CONTENT_ITEM = "content item";
	private static final String STRUCT_TREE_ROOT = "StructTreeRoot";

	private static final String STRUCT_TREE_ROOT_OBJECT = "PDStructTreeRoot";
	private static final String STRUCT_ELEM_FORMAT = "SE%s";

	private static final String FORBIDDEN_DESCRIPTION_FORMAT = "%s elem should not contain %s kid";
	private static final String FORBIDDEN_ERROR_FORMAT = "%s elem contains %s kid";

	private static final String AT_LEAST_ONE_DESCRIPTION_FORMAT = "%s elem should contain at least one %s kid";
	private static final String AT_LEAST_ONE_ERROR_FORMAT = "%s does not contain %s kid";

	private static final String ZERO_OR_ONE_DESCRIPTION_FORMAT = "%s elem should contain zero or one %s kid";
	private static final String ZERO_OR_ONE_ERROR_FORMAT = "%s contains more than one %s kid";

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
				"Every structure element should be mapped to standard type",
				"false",
				Profiles.errorFromValues("Structure element does not mapped to standard structure type",
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
		if ((!PDF_1_7_STANDARD_ROLE_TYPES.contains(parent)
		     && !PDF_2_0_STANDARD_ROLE_TYPES.contains(parent)
		     && !parent.equals(HN)
		     && !parent.equals(STRUCT_TREE_ROOT))
		    ||
		    (!PDF_1_7_STANDARD_ROLE_TYPES.contains(child)
		     && !PDF_2_0_STANDARD_ROLE_TYPES.contains(child)
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

		return (PDF_1_7_STANDARD_ROLE_TYPES.contains(parent) || parent.equals(HN) || parent.equals(STRUCT_TREE_ROOT))
		       && (PDF_1_7_STANDARD_ROLE_TYPES.contains(child) || child.equals(HN) || child.equals(CONTENT_ITEM));
	}

	private RuleData getRuleData(ParsedRelationStructure rel) {
		switch (rel.getRelation()) {
			case FORBIDDEN:
				return constructForbidden(rel);
			case AT_LEAST_ONE:
				return constructAtLeastOne(rel);
			case ZERO_OR_ONE:
				return constructZeroOrOne(rel);
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
