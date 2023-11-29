package org.verapdf;

import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.validation.profiles.*;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ProfileMerger {

    private static final String repositoryName = "veraPDF";
    private static final String branchName = "integration";

    private static final String veraUrl = "https://github.com/" + repositoryName + "/veraPDF-validation-profiles/archive/" + 
            branchName + ".zip";
    private static final String PDFA_FOLDER = "veraPDF-validation-profiles-" + branchName + "/PDF_A/";
    private static final String PDFUA_FOLDER = "veraPDF-validation-profiles-" + branchName + "/PDF_UA/";

    public static void main(String[] args) throws IOException {
        new File("output").mkdirs();
        File zipFile;
        try {
            zipFile = org.verapdf.CorpusDownload.createTempFileFromCorpus(URI.create(veraUrl).toURL(), "validationProfiles");
        } catch (IOException excep) {
            throw new IllegalStateException(excep);
        }
        ZipFile zipSource = new ZipFile(zipFile);
        if ("pdfa".equals(args[0])) {
            updatePDFAProfiles(zipSource);
        } else {
            updatePDFUAProfiles(zipSource);
        }
        zipSource.close();
    }
    
    private static void updatePDFAProfiles(ZipFile zipSource) {
        List<RuleId> excludedPDFA3Rules = new ArrayList<>(1);
        excludedPDFA3Rules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_19005_2, "6.8", 5));
        List<RuleId> excludedPDFA4Rules = new ArrayList<>(1);
        excludedPDFA4Rules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_19005_4, "6.9", 3));
        generateProfile(zipSource, "PDFA-1A.xml", PDFA_FOLDER, new String[]{"1a", "1b"}, new String[]{}, Collections.emptyList());
        generateProfile(zipSource, "PDFA-1B.xml", PDFA_FOLDER, new String[]{"1b"}, new String[]{}, Collections.emptyList());
        generateProfile(zipSource, "PDFA-2A.xml", PDFA_FOLDER, new String[]{"2a", "2u", "2b"}, new String[]{}, Collections.emptyList());
        generateProfile(zipSource, "PDFA-2B.xml", PDFA_FOLDER, new String[]{"2b"}, new String[]{}, Collections.emptyList());
        generateProfile(zipSource, "PDFA-2U.xml", PDFA_FOLDER, new String[]{"2u", "2b"}, new String[]{}, Collections.emptyList());
        generateProfile(zipSource, "PDFA-3A.xml", PDFA_FOLDER, new String[]{"3a", "3u", "3b", "2a", "2u", "2b"}, new String[]{}, excludedPDFA3Rules);
        generateProfile(zipSource, "PDFA-3B.xml", PDFA_FOLDER, new String[]{"3b", "2b"}, new String[]{}, excludedPDFA3Rules);
        generateProfile(zipSource, "PDFA-3U.xml", PDFA_FOLDER, new String[]{"3u", "3b", "2u", "2b"}, new String[]{}, excludedPDFA3Rules);
        generateProfile(zipSource, "PDFA-4.xml", PDFA_FOLDER, new String[]{"4"}, new String[]{}, Collections.emptyList());
        generateProfile(zipSource, "PDFA-4E.xml", PDFA_FOLDER, new String[]{"4e", "4"}, new String[]{}, excludedPDFA4Rules);
        generateProfile(zipSource, "PDFA-4F.xml", PDFA_FOLDER, new String[]{"4f", "4"}, new String[]{}, excludedPDFA4Rules);
    }
    
    private static void updatePDFUAProfiles(ZipFile zipSource) {
        generateProfile(zipSource, "PDFUA-1.xml", PDFUA_FOLDER, new String[]{"1"}, new String[]{}, Collections.emptyList());
        List<RuleId> excludedWCAGRules = new ArrayList<>(1);
        excludedWCAGRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_1, "5.1", 1));
        excludedWCAGRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_1, "5.1", 2));
        excludedWCAGRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_1, "5.1", 3));
        excludedWCAGRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_1, "5.1", 4));
        excludedWCAGRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_1, "5.1", 5));
        generateProfile(zipSource, "WCAG-21-Complete.xml", PDFUA_FOLDER, new String[]{"1"}, new String[]{"WCAG-21.xml"}, excludedWCAGRules);
    }

    private static void generateProfile(ZipFile zipSource, String generalProfileName, String folder, String[] folders,
                                        String[] includedProfiles, List<RuleId> excludedRules) {
        String generalProfilePath = folder + generalProfileName;
        ValidationProfile validationProfile;
        try (InputStream inputStream = zipSource.getInputStream(zipSource.getEntry(generalProfilePath))) {
            validationProfile = Profiles.profileFromXml(inputStream);
        } catch (JAXBException | IOException e) {
            throw new RuntimeException(e);
        }
        SortedSet<Rule> rules = new TreeSet<>(new Profiles.RuleComparator());
        SortedSet<Variable> variables = new TreeSet<>(Comparator.comparing(Variable::getName));
        for (String currentFolder : folders) {
            Set<String> profilesNames = getProfilesNames(folder + currentFolder + "/", zipSource);
            for (String profilePath : profilesNames) {
                addRulesFromProfile(generalProfileName, zipSource, profilePath, rules, variables, excludedRules);
            }
        }

        for (String profilePath : includedProfiles) {
            addRulesFromProfile(generalProfileName, zipSource, folder + profilePath, rules, variables, excludedRules);
        }
        ValidationProfile mergedProfile = Profiles.profileFromSortedValues(validationProfile.getPDFAFlavour(),
                validationProfile.getDetails(), validationProfile.getHexSha1Digest(), rules, variables);
        try (OutputStream out = Files.newOutputStream(new File("output/" + generalProfileName).toPath())) {
            Profiles.profileToXml(mergedProfile, out, true, false);
        } catch (JAXBException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static Set<String> getProfilesNames(String folderName, ZipFile zipSource) {
        Enumeration<? extends ZipEntry> entries = zipSource.entries();
        Set<String> result = new HashSet<>();
        while (entries.hasMoreElements()) {
            String entryName = entries.nextElement().getName();
            if (entryName.contains(folderName) && entryName.endsWith(".xml")) {
                result.add(entryName);
            }
        }
        return result;
    }
    
    private static void addRulesFromProfile(String generalProfileName, ZipFile zipSource, String profilePath, 
                                            SortedSet<Rule> rules, Set<Variable> variables, List<RuleId> excludedRules) {
        try (InputStream inputStream = zipSource.getInputStream(zipSource.getEntry(profilePath))) {
            ValidationProfile profile = Profiles.profileFromXml(inputStream);
            addRules(generalProfileName, profile, rules, variables, excludedRules);
        } catch (JAXBException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static void addRules(String generalProfileName, ValidationProfile profile, SortedSet<Rule> rules, 
                                 Set<Variable> variables, List<RuleId> excludedRules) {
        for (Rule rule : profile.getRules()) {
            if (!excludedRules.contains(rule.getRuleId())) {
                if (generalProfileName.contains("PDFA-3") && rule.getRuleId().getSpecification() == PDFAFlavour.Specification.ISO_19005_2) {
                    rule = updatePDFA2RuleToPDFA3(rule);
                }
                rules.add(rule);
            }
        }
        variables.addAll(profile.getVariables());
    }
    
    private static Rule updatePDFA2RuleToPDFA3(Rule rule) {
        RuleId ruleId = Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_19005_3, rule.getRuleId().getClause(), 
                rule.getRuleId().getTestNumber());
        List<Reference> references = new ArrayList<>(rule.getReferences().size());
        for (Reference reference : rule.getReferences()) {
            if (reference.getSpecification().contains("ISO 19005-2:2011")) {
                reference = Profiles.referenceFromValues(reference.getSpecification().replace("19005-2:2011", 
                        "19005-3:2012"), reference.getClause());
            }
            references.add(reference);
        }
        String description = rule.getDescription().replace("PDF/A-2", "PDF/A-3")
                .replace("ISO 19005-2", "ISO 19005-3");
        return Profiles.ruleFromValues(ruleId, rule.getObject(), rule.getDeferred(), rule.getTags(), description, 
                rule.getTest(), rule.getError(), references);
    }
}
