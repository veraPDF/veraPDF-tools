package org.verapdf.tools.tagged;

import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.validation.profiles.*;
import org.verapdf.tools.tagged.enums.ChildrenRelation;
import org.verapdf.tools.tagged.enums.PDFVersion;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.*;

/**
 * @author Maksim Bezrukov
 */
final class CliProcessor {

	private final StructureRuleCreator ruleCreator;
	private final String name;
	private final String description;
	private final String creator;

	private CliProcessor(CliArgParser args) {
		this.ruleCreator = new StructureRuleCreator(args.getPdfVersion());
		this.name = args.getName();
		this.description = args.getDescription();
		this.creator = args.getCreator();
	}

	static CliProcessor createProcessorFromArgs(final CliArgParser args) {
		return new CliProcessor(args);
	}

	void process(String csvIn, OutputStream out) throws FileNotFoundException, JAXBException {
		List<ParsedRelationStructure> relations = parseRelations(csvIn);
		SortedSet<Rule> rules = new TreeSet<>(new Profiles.RuleComparator());
		SortedSet<Variable> variables = new TreeSet<>(Comparator.comparing(Variable::getName));

		rules.addAll(ruleCreator.generateRules(relations));

		ProfileDetails det = Profiles.profileDetailsFromValues(name, description, creator, new Date());
		ValidationProfile mergedProfile = Profiles.profileFromSortedValues(ruleCreator.getFlavour(), det, "", rules, variables);
		Profiles.profileToXml(mergedProfile, out, true, false);
	}

	private List<ParsedRelationStructure> parseRelations(String csvIn) throws FileNotFoundException {
		List<ParsedRelationStructure> res = new ArrayList<>();
		try (Scanner sc = new Scanner(new File(csvIn))) {
			// read and check headers
			String[] headers = sc.nextLine().split(",");
			if (!"".equals(headers[0])) {
				throw new IllegalArgumentException("Invalid headers format");
			}

			// scanning children
			int childIndex = 0;
			while (sc.hasNextLine()) {
				++childIndex;
				String[] children = sc.nextLine().split(",");
				// is this line correct, probably add first element check on containing in standard types set
				if (children.length != headers.length || "".equals(children[0])) {
					continue;
				}

				for (int i = 1; i < children.length; ++i) {
					res.add(new ParsedRelationStructure(headers[i], children[0], i, childIndex,
					                                    ChildrenRelation.fromName(children[i])));
				}
			}
			int finalChildIndex = childIndex + 1;
			res.sort(Comparator.comparingInt(a -> (a.getParentIndex() * finalChildIndex + a.getChildIndex())));
		}
		return res;
	}
}
