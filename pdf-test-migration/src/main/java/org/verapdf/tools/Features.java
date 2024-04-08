package org.verapdf.tools;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.pdfbox.pdmodel.PDDocument;

public class Features {
    private Map<OptionFeature, String> features = new HashMap<OptionFeature, String>();
    protected Boolean isPreset = false;

    public Features(CommandLine commandLine) {
        for (Option option : commandLine.getOptions()) {
            OptionFeature optionFeature = OptionFeature.fromOption(option.getOpt());

            features.put(optionFeature, option.getValue());
        }
    }

    protected void modifyFeature(OptionFeature optionFeature, String argument) {
        features.put(optionFeature, argument);
    }

    protected void removeFeature(OptionFeature optionFeature) {
        features.remove(optionFeature);
    }

    public Boolean isFeatureEnabled(OptionFeature feature) {
        return features.containsKey(feature);
    }

    public void checkPresets(TestMigration testMigration) {
        for (OptionFeature feature : OptionFeature.getOptionsByTag(FeatureTag.PRESET)) {
            if (!isFeatureEnabled(feature)) {
                continue;
            }

            feature.preset(testMigration, features.get(feature));

            if (isPreset) {
                return;
            }
        }
    }

    public void runPreFeatures(TestMigration testMigration) {
        for (OptionFeature feature : OptionFeature.getOptionsByTag(FeatureTag.PRE_PROCESS)) {
            if (!isFeatureEnabled(feature)) {
                continue;
            }

            feature.preFeature(testMigration, features.get(feature));
        }
    }

    public void runFeatures(PDDocument document) throws Exception {
        for (OptionFeature feature : OptionFeature.getOptionsByTag(FeatureTag.PROCESS)) {
            if (!isFeatureEnabled(feature)) {
                continue;
            }

            feature.feature(document, features.get(feature));
        }
    }

    public void runPostFeatures(TestMigration testMigration, String targetFilePath) throws Exception {
        for (OptionFeature feature : OptionFeature.getOptionsByTag(FeatureTag.POST_PROCESS)) {
            if (!isFeatureEnabled(feature)) {
                continue;
            }

            feature.postFeature(testMigration, targetFilePath, features.get(feature));
        }
    }
}
