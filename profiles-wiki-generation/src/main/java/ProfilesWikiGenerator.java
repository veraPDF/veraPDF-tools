import org.verapdf.pdfa.validation.profiles.Profiles;
import org.verapdf.pdfa.validation.profiles.Reference;
import org.verapdf.pdfa.validation.profiles.Rule;
import org.verapdf.pdfa.validation.profiles.ValidationProfile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.SortedSet;
import java.util.TreeSet;

public class ProfilesWikiGenerator {

    public static String inputFileName = "PDFA-4.xml";
    public static String pdfa1_flavour = "PDF/A-1";
    public static String pdfa2_flavour = "PDF/A-2";
    public static String pdfua1_flavour = "PDF/UA-1";
    public static String pdfa4_flavour = "PDF/A-4";
    public static String flavour = pdfa4_flavour;
    public static String outputFileName = "wiki_" + flavour.replace("/","") + ".md";

    public static void main(String[] args) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(inputFileName));
             PrintWriter out = new PrintWriter(outputFileName)) {
            ValidationProfile profile = Profiles.profileFromXml(inputStream);
            SortedSet<Rule> rules = new TreeSet<>(new Profiles.RuleComparator());
            rules.addAll(profile.getRules());
            out.println("# " + flavour + " validation rules");
            for (Rule rule : rules) {
                out.println("## Rule " + rule.getRuleId().getClause() + "-" + rule.getRuleId().getTestNumber());
                out.println();
                out.println("### Requirement");
                out.println();
                String description = rule.getDescription().replace(" (*) ", "*\n\n>- *");
                out.println(">*" + description + "*");
                out.println();
                out.println("### Error details");
                out.println();
                out.println(rule.getError().getMessage());
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
        } else if (pdfa1_flavour.equals(flavour)) {
            return "A, B";
        } else if (pdfa2_flavour.equals(flavour)) {
            return "A, B, E";
        }
        return null;
    }

    private static String getSpecification() {
        if (pdfa4_flavour.equals(flavour)) {
            return "ISO 19005-4:2020";
        } else if (pdfa1_flavour.equals(flavour)) {
            return "ISO 19005-1:2005";
        } else if (pdfa2_flavour.equals(flavour)) {
            return "ISO 19005-2:2011, ISO 19005-3:2012";
        } else if (pdfua1_flavour.equals(flavour)) {
            return "ISO 14289-1:2014";
        }
        return null;
    }
}
