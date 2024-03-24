import org.verapdf.pdfa.validation.profiles.Profiles;
import org.verapdf.pdfa.validation.profiles.Reference;
import org.verapdf.pdfa.validation.profiles.Rule;
import org.verapdf.pdfa.validation.profiles.ValidationProfile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProfilesWikiGenerator {

    private static final Logger LOGGER = Logger.getLogger(ProfilesWikiGenerator.class.getCanonicalName());

    public static String inputFileName = "PDFA-4.xml";
    public static String pdfa1_flavour = "PDF/A-1";
    public static String pdfa2_flavour = "PDF/A-2";
    public static String pdfua1_flavour = "PDF/UA-1";
    public static String pdfua2_flavour = "PDF/UA-2";
    public static String pdfa4_flavour = "PDF/A-4";
    public static String wcag2_2_flavour = "WCAG2.2";

    public static String flavour = pdfa4_flavour;
    public static String outputFileName = "wiki_" + flavour.replace("/","") + ".md";
    
    public static String ERROR_ARGUMENT_WARNING = "Error message for rule %s contains error argument";

    public static void main(String[] args) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(inputFileName));
             PrintWriter out = new PrintWriter(outputFileName)) {
            ValidationProfile profile = Profiles.profileFromXml(inputStream);
            SortedSet<Rule> rules = new TreeSet<>(new Profiles.RuleComparator());
            rules.addAll(profile.getRules());
            out.println("# " + flavour + " validation rules");
            for (Rule rule : rules) {
                String ruleNumber = rule.getRuleId().getClause() + "-" + rule.getRuleId().getTestNumber();
                out.println("## Rule " + ruleNumber);
                out.println();
                out.println("### Requirement");
                out.println();
                String description = rule.getDescription().replace(" (*) ", "*\n\n>- *");
                out.println(">*" + description + "*");
                out.println();
                out.println("### Error details");
                out.println();
                out.println(rule.getError().getMessage());
                if (!rule.getError().getArguments().isEmpty()) {
                    LOGGER.log(Level.WARNING, String.format(ERROR_ARGUMENT_WARNING, rule.getRuleId().getClause() + '-' + 
                            rule.getRuleId().getTestNumber()));
                }
                out.println();
                out.println("* Object type: `" + rule.getObject() + "`");
                out.println("* Test condition: `" + rule.getTest() + "`");
                out.println("* Specification: " + getSpecification());
                String levels = getLevels();
                if (levels != null) {
                    out.println("* Levels: " + levels);
                }
                if (!rule.getReferences().isEmpty()) {
                    out.println("* Additional references:");
                    for (Reference reference : rule.getReferences()) {
                        if (reference.getClause().isEmpty()) {
                            out.println("  * " + reference.getSpecification());
                        } else {
                            out.println("  * " + reference.getSpecification() + ", " + reference.getClause());
                        }
                    }
                }
                out.println();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getLevels() {
        if (pdfa4_flavour.equals(flavour)) {
            return "4, 4E, 4F";
        }
        if (pdfa1_flavour.equals(flavour)) {
            return "A, B";
        }
        if (pdfa2_flavour.equals(flavour)) {
            return "A, B, E";
        }
        return null;
    }

    private static String getSpecification() {
        if (pdfa4_flavour.equals(flavour)) {
            return "ISO 19005-4:2020";
        }
        if (pdfa1_flavour.equals(flavour)) {
            return "ISO 19005-1:2005";
        }
        if (pdfa2_flavour.equals(flavour)) {
            return "ISO 19005-2:2011, ISO 19005-3:2012";
        }
        if (pdfua1_flavour.equals(flavour)) {
            return "ISO 14289-1:2014";
        }
        if (pdfua2_flavour.equals(flavour)) {
            return "ISO 14289-2:202X";
        }
        if (wcag2_2_flavour.equals(flavour)) {
            return "WCAG 2.2";
        }
        return null;
    }
}
