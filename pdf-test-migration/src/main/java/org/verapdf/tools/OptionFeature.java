package org.verapdf.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.verapdf.pdfa.flavours.PDFAFlavour;

public enum OptionFeature {
    UA2_PRESET("ua2_preset", "ua2", "Default preset for pdf-ua2 migration", false, false, FeatureTag.PRESET) {
        @Override
        public void preset(TestMigration testMigration, String argument) {
            Features features = testMigration.getFeatures();
            features.isPreset = true;

            features.modifyFeature(TARGET_FLAVOUR, "ua2");
            features.modifyFeature(FIX_DEPRECATED, "");
            features.modifyFeature(FIX_METADATA, "");
            features.modifyFeature(SET_VERSION, "2.0");
            features.modifyFeature(REMOVE_OPENACTION, "");
            features.modifyFeature(ADD_NAMESPACE, "http://iso.org/pdf2/ssn");
        }
    },
    INPUT_FOLDER("parent_input_folder", "pif", "<arg> - path to parent input folder", true, false,
            FeatureTag.PRE_PROCESS) {
        @Override
        public void preFeature(TestMigration testMigration, String argument) {
            if (argument.endsWith("/")) {
                testMigration.inputParentFolderPath = argument;
            } else {
                testMigration.inputParentFolderPath = argument + "/";
            }
        }
    },
    TARGET_FOLDER("parent_target_folder", "ptf", "<arg> - path to parent target folder", true, false,
            FeatureTag.PRE_PROCESS) {
        @Override
        public void preFeature(TestMigration testMigration, String argument) {
            if (argument.endsWith("/")) {
                testMigration.targetParentFolderPath = argument;
            } else {
                testMigration.targetParentFolderPath = argument + "/";
            }
        }
    },
    CSV_FILE("csv_file", "csv",
            "<arg> - .csv file with <INPUT_FOLDER>;<INPUT_VERSION>;<OUTPUT_FOLDER>;<OUTPUT_VERSION>;<OUTLINE_HEADER> format",
            true, false, FeatureTag.PRE_PROCESS) {
        @Override
        public void preFeature(TestMigration testMigration, String argument) {
            testMigration.setCSVFile(argument);
        }
    },
    TARGET_FLAVOUR("flavour", "f", "Sets target flavour to <arg>", true, false, FeatureTag.PRE_PROCESS) {
        @Override
        public void preFeature(TestMigration testMigration, String argument) {
            testMigration.targetFlavour = PDFAFlavour.byFlavourId(argument);
        }
    },
    SET_OUTLINEHEADER("outline_header", "oh", "Replaces first outline with <arg>", true, false, FeatureTag.PROCESS) {
        @Override
        public void feature(PDDocument document, String argument) throws Exception {
            if (argument == null || argument.length() == 0) {
                return;
            }

            ArrayList<String> oldOutlines = new ArrayList<String>();
            Map<String, List<String>> childs = new HashMap<String, List<String>>();
            PDOutlineItem currentOutlineItem = document.getDocumentCatalog().getDocumentOutline().getFirstChild();

            currentOutlineItem = currentOutlineItem.getNextSibling();
            while (currentOutlineItem != null) {
                oldOutlines.add(currentOutlineItem.getTitle());
                PDOutlineItem childOutline = currentOutlineItem.getFirstChild();

                List<String> subChilds = new ArrayList<String>();

                while (childOutline != null) {
                    subChilds.add(childOutline.getTitle());
                    childOutline = childOutline.getNextSibling();
                }

                childs.put(currentOutlineItem.getTitle(), subChilds);
                currentOutlineItem = currentOutlineItem.getNextSibling();
            }

            PDDocumentOutline outlines = new PDDocumentOutline();
            currentOutlineItem = new PDOutlineItem();
            currentOutlineItem.setTitle(argument);

            outlines.addFirst(currentOutlineItem);

            for (String line : oldOutlines) {
                PDOutlineItem newOutline = new PDOutlineItem();
                newOutline.setTitle(line);

                if (childs.get(line).size() > 0) {
                    for (String subChild : childs.get(line)) {
                        PDOutlineItem subChildOutline = new PDOutlineItem();
                        subChildOutline.setTitle(subChild);

                        newOutline.addFirst(subChildOutline);
                    }
                }

                currentOutlineItem.insertSiblingAfter(newOutline);
                currentOutlineItem = newOutline;
            }

            document.getDocumentCatalog().setDocumentOutline(outlines);
        }
    },
    SET_LANGUAGE("language", "l", "Set pdf language to <arg>", true, false, FeatureTag.PROCESS) {
        @Override
        public void feature(PDDocument document, String argument) throws Exception {
            if (argument == null) {
                return;
            }

            document.getDocumentCatalog().setLanguage(argument);
        }
    },
    SET_VERSION("version", "v", "Set pdf version to <arg>", true, false, FeatureTag.PROCESS) {
        @Override
        public void feature(PDDocument document, String argument) throws Exception {
            if (argument == null) {
                return;
            }

            try {
                Float version = Float.parseFloat(argument);

                document.getDocument().setVersion(version);
            } catch (NumberFormatException exception) {
                return;
            }
        }
    },
    REMOVE_OPENACTION("remove_openaction", "ro", "Removes open action from document catalog", false, false,
            FeatureTag.PROCESS) {
        @Override
        public void feature(PDDocument document, String argument) throws Exception {
            document.getDocumentCatalog().setOpenAction(null);
        }
    },
    ADD_NAMESPACE("namespace", "ns", "Set <arg> namespace for document", true, false, FeatureTag.PROCESS) {
        @Override
        public void feature(PDDocument document, String argument) throws Exception {
            if (argument == null || argument.length() == 0) {
                return;
            }

            PDStructureTreeRoot root = document.getDocumentCatalog().getStructureTreeRoot();

            if (root != null) {
                COSArray namespaces = new COSArray();
                COSDictionary NS = new COSDictionary();

                NS.setName("Type", "Namespace");
                NS.setString("NS", "http://iso.org/pdf2/ssn");

                namespaces.add(NS);
                root.getCOSObject().setItem("Namespaces", namespaces);

                COSBase base = root.getK();
                if (base instanceof COSDictionary) {
                    COSDictionary dictionary = (COSDictionary) base;

                    dictionary.setItem("NS", NS);

                    return;
                }

                root.getKids().forEach((child) -> {
                    if (child instanceof PDStructureElement) {
                        COSDictionary dictionary = ((PDStructureElement) child).getCOSObject();

                        dictionary.setItem("NS", NS);
                    }
                });
            }
        }
    },
    FIX_DEPRECATED("fix_deprecated", "fd", "Fixes deprecated files", false, false, FeatureTag.PRE_PROCESS, FeatureTag.POST_PROCESS) {
        @Override
        public void preFeature(TestMigration testMigration, String argument) {
            testMigration.getTempDir().toFile().mkdirs();
        }

        @Override
        public void postFeature(TestMigration testMigration, String targetFilePath, String argument) throws Exception {
            String[] args = { targetFilePath };

            DeprecatedFinderCli.main(args);

            File originalFile = new File(targetFilePath);
            File fixedFile = testMigration.getTempDir().resolve("fix_" + originalFile.getName()).toFile();

            if (!fixedFile.exists()) {
                return;
            }

            originalFile.delete();
            fixedFile.renameTo(new File(targetFilePath));
        }
    },
    FIX_METADATA("fix_metadata", "fm", "Fixes metadata", false, false, FeatureTag.PRE_PROCESS, FeatureTag.POST_PROCESS) {
        @Override
        public void preFeature(TestMigration testMigration, String argument) {
            testMigration.getTempDir().toFile().mkdirs();
        }

        @Override
        public void postFeature(TestMigration testMigration, String targetFilePath, String argument) throws Exception {
            File originalFile = new File(targetFilePath);
            File temp = testMigration.getTempDir().resolve(originalFile.getName()).toFile();
            originalFile.renameTo(temp);

            String[] args = { temp.getAbsolutePath(), targetFilePath, testMigration.targetFlavour.toString() };
            FixMetadataTool.main(args);

            temp.delete();
        }
    };

    private final String option;
    private final String short_form;
    private final String description;
    private final Boolean hasArgs;
    private final Boolean required;
    private final Integer featureTags;

    public void preset(TestMigration testMigration, String argument) {

    }

    public void preFeature(TestMigration testMigration, String argument) {

    }

    public void feature(PDDocument document, String argument) throws Exception {

    }

    public void postFeature(TestMigration testMigration, String targetFilePath, String argument) throws Exception {

    }

    private OptionFeature(String option, String short_form, String description, Boolean hasArgs, Boolean required, FeatureTag... tags) {
        this.option = option;
        this.short_form = short_form;
        this.description = description;
        this.hasArgs = hasArgs;
        this.required = required;

        this.featureTags = FeatureTag.getInteger(tags);
    }

    public Boolean isRequired() {
        return required;
    }

    public String getShortOption() {
        return short_form;
    }

    public Boolean hasTag(FeatureTag tag) {
        return tag.checkTag(featureTags);
    }

    public static OptionFeature fromOption(String shortFormOption) {
        return features.getOrDefault(shortFormOption, null);
    }

    public static List<OptionFeature> getOptionsByTag(FeatureTag tag) {
        return featuresByTag.getOrDefault(tag, Arrays.asList());
    }

    private static Map<String, OptionFeature> features = new HashMap<String, OptionFeature>();
    private static Map<FeatureTag, List<OptionFeature>> featuresByTag = new HashMap<FeatureTag, List<OptionFeature>>();
    static {
        for (FeatureTag tag : FeatureTag.values()) {
            featuresByTag.put(tag, new ArrayList<OptionFeature>());
        }

        for (OptionFeature optionFeature : OptionFeature.values()) {
            features.put(optionFeature.short_form, optionFeature);

            for (FeatureTag tag : FeatureTag.fromInteger(optionFeature.featureTags)) {
                featuresByTag.get(tag).add(optionFeature);
            }
        }
    }

    public static Options generateOptions() {
        Options options = new Options();

        for (OptionFeature optionFeature : OptionFeature.values()) {
            Option option = new Option(optionFeature.short_form, optionFeature.option, optionFeature.hasArgs,
                    optionFeature.description);
            option.setRequired(optionFeature.required);

            options.addOption(option);
        }

        return options;
    }
}
