package org.verapdf;

import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.validation.profiles.*;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ProfileMerger {

    private static final String repositoryName = "veraPDF";
    private static final String branchName = "integration";

    private static final String veraUrl = "https://github.com/" + repositoryName + "/veraPDF-validation-profiles/archive/" + 
            branchName + ".zip";
    private static final String PDFA_FOLDER = "PDF_A/";
    private static final String PDFUA_FOLDER = "PDF_UA/";
    
    private static final String WCAG_MACHINE_PROFILE_NAME = "WCAG-2-2-Machine.xml";
    private static final String PATH = "veraPDF-validation-profiles-" + branchName + "/";
    private static final Set<String> excludedPDFUA1Tags = new HashSet<>();
    static {
        excludedPDFUA1Tags.add("major");
        excludedPDFUA1Tags.add("minor");
        excludedPDFUA1Tags.add("critical");
        excludedPDFUA1Tags.add("cosmetic");
        excludedPDFUA1Tags.add("machine");
    }

    public static void main(String[] args) throws IOException {
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
        new File(PDFA_FOLDER).mkdirs();
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
        new File(PDFUA_FOLDER).mkdirs();
        generateProfile(zipSource, "PDFUA-1.xml", PDFUA_FOLDER, new String[]{"1"}, new String[]{}, Collections.emptyList());
        generateProfile(zipSource, "PDFUA-2.xml", PDFUA_FOLDER, new String[]{"2"}, new String[]{}, Collections.emptyList());
        List<RuleId> excludedTaggedRules = new ArrayList<>(6);
        excludedTaggedRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_32005, "6.2", 1));
        excludedTaggedRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_32005, "6.2", 1656));
        excludedTaggedRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_32005, "6.2", 1657));
        excludedTaggedRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_32005, "6.2", 1658));
        excludedTaggedRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_32005, "6.2", 1659));
        excludedTaggedRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_32005, "6.2", 1660));
        generateProfile(zipSource, "PDFUA-2-ISO32005.xml", PDFUA_FOLDER, new String[]{"2"}, new String[]{"ISO-32005-Tagged.xml"}, excludedTaggedRules);
        generateProfile(zipSource, "WCAG-2-2.xml", PDFUA_FOLDER, new String[]{"WCAG/2.2"}, new String[]{}, Collections.emptyList());
        List<RuleId> excludedWCAGRules = new ArrayList<>(7);
        excludedWCAGRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_1, "5", 1));
        excludedWCAGRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_1, "5", 2));
        excludedWCAGRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_1, "5", 3));
        excludedWCAGRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_1, "5", 4));
        excludedWCAGRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_1, "5", 5));
        excludedWCAGRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_1, "7.4.2", 1));
        excludedWCAGRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_1, "7.18.5", 2));
        generateProfile(zipSource, "WCAG-2-2-Complete.xml", PDFUA_FOLDER, new String[]{"WCAG/2.2", "WCAG/PDF_UA", "1"}, new String[]{}, excludedWCAGRules);
        generateProfile(zipSource, WCAG_MACHINE_PROFILE_NAME, PDFUA_FOLDER, new String[]{"WCAG/2.2", "WCAG/PDF_UA", "1"}, new String[]{}, excludedWCAGRules);
        List<RuleId> excludedWTPDFRules = new ArrayList<>(18);//not for reuse
        excludedWTPDFRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_2, "5", 1));
        excludedWTPDFRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_2, "5", 2));
        excludedWTPDFRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_2, "5", 3));
        excludedWTPDFRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_2, "5", 4));
        excludedWTPDFRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_2, "5", 5));
        generateProfile(zipSource, "WTPDF-1-0-Accessibility.xml", PDFUA_FOLDER, new String[]{"2", "WTPDF/1.0/Accessibility"}, new String[]{}, excludedWTPDFRules);
        excludedWTPDFRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_2, "8.2.5.28.2", 1));
        excludedWTPDFRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_2, "8.2.5.29", 1));
        excludedWTPDFRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_2, "8.4.3", 1));
        excludedWTPDFRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_2, "8.4.3", 2));
        excludedWTPDFRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_2, "8.4.3", 3));
        excludedWTPDFRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_2, "8.7", 1));
        excludedWTPDFRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_2, "8.7", 2));
        excludedWTPDFRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_2, "8.9.2.3", 2));
        excludedWTPDFRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_2, "8.9.2.4.7", 1));
        excludedWTPDFRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_2, "8.9.2.4.8", 1));
        excludedWTPDFRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_2, "8.9.2.4.19", 1));
        excludedWTPDFRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_2, "8.9.2.4.19", 2));
        excludedWTPDFRules.add(Profiles.ruleIdFromValues(PDFAFlavour.Specification.ISO_14289_2, "8.11.2", 1));
        generateProfile(zipSource, "WTPDF-1-0-Reuse.xml", PDFUA_FOLDER, new String[]{"2", "WTPDF/1.0/Reuse"}, new String[]{}, excludedWTPDFRules);
    }

    private static void generateProfile(ZipFile zipSource, String generalProfileName, String folder, String[] folders,
                                        String[] includedProfiles, List<RuleId> excludedRules) {
        String generalProfilePath = PATH + folder + generalProfileName;
        ValidationProfile validationProfile;
        try (InputStream inputStream = zipSource.getInputStream(zipSource.getEntry(generalProfilePath))) {
            validationProfile = Profiles.profileFromXml(inputStream);
        } catch (JAXBException | IOException e) {
            throw new RuntimeException(e);
        }
        SortedSet<Rule> rules = new TreeSet<>(new Profiles.RuleComparator());
        SortedSet<Variable> variables = new TreeSet<>(Comparator.comparing(Variable::getName));
        for (String currentFolder : folders) {
            Set<String> profilesNames = getProfilesNames(PATH + folder + currentFolder + "/", zipSource);
            for (String profilePath : profilesNames) {
                addRulesFromProfile(generalProfileName, zipSource, profilePath, rules, variables, excludedRules);
            }
        }

        for (String profilePath : includedProfiles) {
            addRulesFromProfile(generalProfileName, zipSource, PATH + folder + profilePath, rules, variables, excludedRules);
        }
        ValidationProfile mergedProfile = Profiles.profileFromSortedValues(validationProfile.getPDFAFlavour(),
                validationProfile.getDetails(), validationProfile.getHexSha1Digest(), rules, variables);
        try (OutputStream out = Files.newOutputStream(new File(folder + generalProfileName).toPath())) {
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
        boolean addRules = false;
        for (Rule rule : profile.getRules()) {
            if (!excludedRules.contains(rule.getRuleId())) {
                if (generalProfileName.contains("PDFA-3") && rule.getRuleId().getSpecification() == PDFAFlavour.Specification.ISO_19005_2) {
                    rule = updatePDFA2RuleToPDFA3(rule);
                }
                if (generalProfileName.contains("WTPDF")) {
                    rule = updatePDFUA2RuleToWTPDF(rule);
                }
                if (generalProfileName.contains("PDFUA-1")) {
                    rule = updatePDFUA1RuleTags(rule);
                }
                if (generalProfileName.contains(WCAG_MACHINE_PROFILE_NAME) && !rule.getTagsSet().contains("machine")) {
                    continue;
                }
                rules.add(rule);
                addRules = true;
            }
        }
        if (addRules) {
            variables.addAll(profile.getVariables());
        }
    }

    private static Rule updatePDFUA2RuleToWTPDF(Rule rule) {
        RuleId ruleId = Profiles.ruleIdFromValues(PDFAFlavour.Specification.WTPDF_1_0, rule.getRuleId().getClause(),
                rule.getRuleId().getTestNumber());
        List<Reference> references = new ArrayList<>(rule.getReferences().size());
        for (Reference reference : rule.getReferences()) {
            if (reference.getSpecification().contains("ISO 14289-2:2024")) {
                reference = Profiles.referenceFromValues(reference.getSpecification().replace("ISO 14289-2:2024",
                        "WTPDF 1.0"), reference.getClause());
            }
            references.add(reference);
        }
        String description = rule.getDescription().replace("PDF/UA-2", "WTPDF 1.0")
                .replace("ISO 14289-2", "WTPDF 1.0");
        return Profiles.ruleFromValues(ruleId, rule.getObject(), rule.getDeferred(), rule.getTags(), description,
                rule.getTest(), rule.getError(), references);
        
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

    private static Rule updatePDFUA1RuleTags(Rule rule) {
        String tags = Arrays.stream(rule.getTags().split(",")).filter(tag -> !excludedPDFUA1Tags.contains(tag)).collect(Collectors.joining(","));
        return Profiles.ruleFromValues(rule.getRuleId(), rule.getObject(), rule.getDeferred(), tags, rule.getDescription(),
                rule.getTest(), rule.getError(), rule.getReferences());
    }
}
