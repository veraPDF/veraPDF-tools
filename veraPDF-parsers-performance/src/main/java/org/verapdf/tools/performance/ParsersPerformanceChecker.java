package org.verapdf.tools.performance;

import org.verapdf.core.EncryptedPdfException;
import org.verapdf.core.ModelParsingException;
import org.verapdf.core.ValidationException;
import org.verapdf.features.FeatureExtractionResult;
import org.verapdf.features.FeatureExtractorConfig;
import org.verapdf.features.FeatureFactory;
import org.verapdf.features.FeatureObjectType;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.MetadataFixerResult;
import org.verapdf.pdfa.results.TestAssertion;
import org.verapdf.pdfa.results.ValidationResult;
import org.verapdf.pdfa.validation.profiles.ValidationProfile;
import org.verapdf.pdfa.validation.validators.ValidatorFactory;
import org.verapdf.tools.factory.ModelParserFactory;
import org.verapdf.tools.utils.TestAssertionContextFreeComparator;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * @author Maksim Bezrukov
 */
public class ParsersPerformanceChecker {

    private static FeatureExtractorConfig featuresConfig = FeatureFactory.createConfig(EnumSet.allOf(FeatureObjectType.class));

    private Map<ModelParserType, ModelParserResults> parsers = new EnumMap<ModelParserType, ModelParserResults>(ModelParserType.class);
    private ValidationProfile profile = null;
    private boolean logPassed = true;
    private int maxFail = -1;

    private ParsersPerformanceChecker(){
    }

    public static ParsersPerformanceChecker createCheckerWithProfile(InputStream toLoad, ValidationProfile profile, boolean logPassed, int maxFail) throws IOException, ModelParsingException, EncryptedPdfException {
        return createCheckerWithProfileAndFlavour(toLoad, profile, PDFAFlavour.NO_FLAVOUR, logPassed, maxFail);
    }

    public static ParsersPerformanceChecker createCheckerWithFlavour(InputStream toLoad, PDFAFlavour flavour, boolean logPassed, int maxFail) throws IOException, ModelParsingException, EncryptedPdfException {
        return createCheckerWithProfileAndFlavour(toLoad, null, flavour, logPassed, maxFail);
    }

    private static ParsersPerformanceChecker createCheckerWithProfileAndFlavour(InputStream toLoad,
                                                                                ValidationProfile profile,
                                                                                PDFAFlavour flavour,
                                                                                boolean logPassed,
                                                                                int maxFail)
            throws IOException, ModelParsingException, EncryptedPdfException {
        if (toLoad == null) {
            throw new IllegalArgumentException("Can not create Parsers Performance Checker without input stream of a file");
        }
        if (profile == null && flavour == null) {
            throw new IllegalArgumentException("Cannot create Parsers Performance Checker without both validation profile and flavour");
        }
        ParsersPerformanceChecker checker = new ParsersPerformanceChecker();
        checker.profile = profile;
        checker.logPassed = logPassed;
        checker.maxFail = maxFail;
        File temp = generateTempFile(toLoad);
        temp.deleteOnExit();
        PDFAFlavour pdfaFlavour = checker.profile == null ? flavour : checker.profile.getPDFAFlavour();
        for (ModelParserType type : ModelParserType.values()) {
            InputStream is = new FileInputStream(temp);
            PDFAParser parser = ModelParserFactory.createModelParser(type, is, pdfaFlavour);
            ModelParserResults modelParserResults = new ModelParserResults(parser, temp);
            checker.parsers.put(type, modelParserResults);
        }
        return checker;
    }

    private static File generateTempFile(InputStream toLoad) throws IOException {
        File res = Files.createTempFile("performance", "").toFile();
        OutputStream outStream = new FileOutputStream(res);

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = toLoad.read(buffer)) != -1) {
            outStream.write(buffer, 0, bytesRead);
        }
        return res;
    }

    public boolean doesValidationResultsEquals() throws ValidationException, ModelParsingException {
        boolean res = true;
        boolean isFirstNotNull = true;
        ValidationResult validationResult = null;

        for (Map.Entry<ModelParserType, ModelParserResults> entry : this.parsers.entrySet()) {
            if (entry.getValue().getValidationResult() == null) {
                validate(entry.getKey());
            }

            if (validationResult == null) {
                validationResult = entry.getValue().getValidationResult();
                if (validationResult == null) {
                    isFirstNotNull = false;
                }
            } else {
                res &= validationResultsEquals(validationResult, entry.getValue().getValidationResult());
            }
        }

        return res && (validationResult == null || isFirstNotNull);
    }

    private static boolean validationResultsEquals(ValidationResult first, ValidationResult second) {
        if (first == null) {
            return second == null;
        } else {
            return first.isCompliant() == second.isCompliant()
                    && first.getPDFAFlavour() == second.getPDFAFlavour()
                    && first.getTotalAssertions() == second.getTotalAssertions()
                    && testAssertionsEquals(first.getTestAssertions(), second.getTestAssertions());
        }
    }

    private static boolean testAssertionsEquals(Set<TestAssertion> first, Set<TestAssertion> second) {
        if (first.size() != second.size()) {
            return false;
        }
        TestAssertionContextFreeComparator comparator = new TestAssertionContextFreeComparator();
        List<TestAssertion> firstList = new ArrayList<>(first);
        Collections.sort(firstList, comparator);
        List<TestAssertion> secondList = new ArrayList<>(second);
        Collections.sort(secondList, comparator);
        int size = first.size();
        for (int i = 0; i < size; ++i) {
            if (comparator.compare(firstList.get(i), secondList.get(i)) != 0) {
                return false;
            }
        }
        return true;
    }

//    public boolean doesMetadataFixerResultsEquals() throws ModelParsingException, IOException, ValidationException {
//        boolean res = true;
//        boolean isFirstNotNull = true;
//        MetadataFixerResult metadataFixerResult = null;
//
//        for (Map.Entry<ModelParserType, ModelParserResults> entry : this.parsers.entrySet()) {
//            if (entry.getValue().getMetadataFixerResult() == null) {
//                fixMetadata(entry.getKey());
//            }
//
//            if (metadataFixerResult == null) {
//                metadataFixerResult = entry.getValue().getMetadataFixerResult();
//                if (metadataFixerResult == null) {
//                    isFirstNotNull = false;
//                }
//            } else {
//                res &= metadataFixerResult.equals(entry.getValue().getMetadataFixerResult());
//            }
//        }
//
//        return res && (metadataFixerResult == null || isFirstNotNull);
//    }

    public boolean doesFeaturesCollectionsEquals() {
        boolean res = true;
        boolean isFirstNotNull = true;
        FeatureExtractionResult featuresCollection = null;

        for (Map.Entry<ModelParserType, ModelParserResults> entry : this.parsers.entrySet()) {
            if (entry.getValue().getFeaturesCollection() == null) {
                collectFeatures(entry.getKey());
            }

            if (featuresCollection == null) {
                featuresCollection = entry.getValue().getFeaturesCollection();
                if (featuresCollection == null) {
                    isFirstNotNull = false;
                }
            } else {
                res &= featuresCollection.equals(entry.getValue().getFeaturesCollection());
            }
        }

        return res && (featuresCollection == null || isFirstNotNull);
    }

    public long getTimeOfValidation(ModelParserType type) throws ValidationException, ModelParsingException {
        ModelParserResults modelParserResults = parsers.get(type);
        if (modelParserResults.getValidationResult() == null) {
            validate(type);
        }

        return modelParserResults.getValidationTime();
    }

//    public long getTimeOfMetadataFixing(ModelParserType type) throws ModelParsingException, IOException, ValidationException {
//        ModelParserResults modelParserResults = parsers.get(type);
//        if (modelParserResults.getMetadataFixerResult() == null) {
//            fixMetadata(type);
//        }
//
//        return modelParserResults.getMetadataFixerTime();
//    }

    public long getTimeOfFeaturesCollecting(ModelParserType type) {
        ModelParserResults modelParserResults = parsers.get(type);
        if (modelParserResults.getFeaturesCollection() == null) {
            collectFeatures(type);
        }

        return modelParserResults.getFeaturesCollectionTime();
    }

    public ValidationResult getValidationResult(ModelParserType type) throws ValidationException, ModelParsingException {
        ModelParserResults modelParserResults = parsers.get(type);
        if (modelParserResults.getValidationResult() == null) {
            validate(type);
        }

        return modelParserResults.getValidationResult();
    }

//    public MetadataFixerResult getMetadataFixerResult(ModelParserType type) throws ModelParsingException, IOException, ValidationException {
//        ModelParserResults modelParserResults = parsers.get(type);
//        if (modelParserResults.getMetadataFixerResult() == null) {
//            fixMetadata(type);
//        }
//
//        return modelParserResults.getMetadataFixerResult();
//    }

    public FeatureExtractionResult getFeaturesCollection(ModelParserType type) {
        ModelParserResults modelParserResults = parsers.get(type);
        if (modelParserResults.getFeaturesCollection() == null) {
            collectFeatures(type);
        }

        return modelParserResults.getFeaturesCollection();
    }

    private void validate(ModelParserType type) throws ValidationException, ModelParsingException {
        ModelParserResults res = this.parsers.get(type);
        PDFAParser parser = res.getParser();
        PDFAValidator validator = this.profile != null ?
                ValidatorFactory.createValidator(this.profile, this.logPassed, this.maxFail)
                : ValidatorFactory.createValidator(parser.getFlavour(), this.logPassed, this.maxFail);
        long startTime = System.currentTimeMillis();
        ValidationResult result = validator.validate(parser);
        long endTime = System.currentTimeMillis();
        res.setValidationResult(result, endTime - startTime);
    }

//    private void fixMetadata(ModelParserType type) throws IOException, ValidationException, ModelParsingException {
//        ModelParserResults res = this.parsers.get(type);
//        if (res.getValidationResult() == null) {
//            validate(type);
//        }
//
//        InputStream is = new FileInputStream(res.getTemp());
//        File tempOut = File.createTempFile("tempOut", "");
//        tempOut.deleteOnExit();
//        FileOutputStream os = new FileOutputStream(tempOut);
//
//        MetadataFixer fixer = MetadataFixerFactory.createModelParser(type);
//        long startTime = System.currentTimeMillis();
//        MetadataFixerResult fixerResult = fixer.fixMetadata(is, os, res.getValidationResult());
//        long endTime = System.currentTimeMillis();
//        res.setMetadataFixerResult(fixerResult, endTime - startTime);
//    }

    private void collectFeatures(ModelParserType type) {
        ModelParserResults res = this.parsers.get(type);
        PDFAParser parser = res.getParser();
        long startTime = System.currentTimeMillis();
        FeatureExtractionResult result = parser.getFeatures(featuresConfig);
        long endTime = System.currentTimeMillis();
        res.setFeaturesCollection(result, endTime - startTime);
    }

    private static class ModelParserResults {
        private PDFAParser parser;
        private File temp;
        private ValidationResult validationResult = null;
        private long validationTime = 0;
        private MetadataFixerResult metadataFixerResult = null;
        private long metadataFixerTime = 0;
        private FeatureExtractionResult featuresCollection = null;
        private long featuresCollectionTime = 0;

        public ModelParserResults(PDFAParser parser, File temp) {
            if (parser == null) {
                throw new IllegalArgumentException("ModelParser can't be null");
            }
            if (temp == null) {
                throw new IllegalArgumentException("File can't be null");
            }
            this.parser = parser;
            this.temp = temp;
        }

        public PDFAParser getParser() {
            return parser;
        }

        public File getTemp() {
            return temp;
        }

        public ValidationResult getValidationResult() {
            return validationResult;
        }

        public long getValidationTime() {
            return validationTime;
        }

        public MetadataFixerResult getMetadataFixerResult() {
            return metadataFixerResult;
        }

        public long getMetadataFixerTime() {
            return metadataFixerTime;
        }

        public FeatureExtractionResult getFeaturesCollection() {
            return featuresCollection;
        }

        public long getFeaturesCollectionTime() {
            return featuresCollectionTime;
        }

        public void setValidationResult(ValidationResult validationResult, long validationTime) {
            this.validationResult = validationResult;
            this.validationTime = validationTime;
        }

        public void setMetadataFixerResult(MetadataFixerResult metadataFixerResult, long metadataFixerTime) {
            this.metadataFixerResult = metadataFixerResult;
            this.metadataFixerTime = metadataFixerTime;
        }

        public void setFeaturesCollection(FeatureExtractionResult featuresCollection, long featuresCollectionTime) {
            this.featuresCollection = featuresCollection;
            this.featuresCollectionTime = featuresCollectionTime;
        }
    }
}
