package org.verapdf.tools.performance;

import org.verapdf.core.ModelParsingException;
import org.verapdf.core.ValidationException;
import org.verapdf.features.pb.PBFeatureParser;
import org.verapdf.features.tools.FeaturesCollection;
import org.verapdf.metadata.fixer.impl.MetadataFixerImpl;
import org.verapdf.metadata.fixer.impl.fixer.PBoxMetadataFixerImpl;
import org.verapdf.pdfa.MetadataFixer;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.ValidationModelParser;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.MetadataFixerResult;
import org.verapdf.pdfa.results.ValidationResult;
import org.verapdf.pdfa.validation.ValidationProfile;
import org.verapdf.pdfa.validators.Validators;
import org.verapdf.tools.factory.MetadataFixerFactory;
import org.verapdf.tools.factory.ModelParserFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.Map;

/**
 * @author Maksim Bezrukov
 */
public class ParsersPerformanceChecker {

    private Map<ModelParserType, ModelParserResults> parsers = new EnumMap<ModelParserType, ModelParserResults>(ModelParserType.class);
    private ValidationProfile profile = null;

    private ParsersPerformanceChecker(){
    }

    public static ParsersPerformanceChecker createCheckerWithProfile(InputStream toLoad, ValidationProfile profile) throws IOException, ModelParsingException {
        if (toLoad == null) {
            throw new IllegalArgumentException("Can not create Parsers Performance Checker without input stream of a file");
        }
        if (profile == null) {
            throw new IllegalArgumentException("Cannot create Parsers Performance Checker without validation profile");
        }
        ParsersPerformanceChecker checker = new ParsersPerformanceChecker();
        checker.profile = profile;
        File temp = generateTempFile(toLoad);
        temp.deleteOnExit();
        PDFAFlavour pdfaFlavour = profile.getPDFAFlavour();

        for (ModelParserType type : ModelParserType.values()) {
            InputStream is = new FileInputStream(temp);
            ValidationModelParser parser = ModelParserFactory.createModelParser(type, is, pdfaFlavour);
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
        ValidationResult validationResult = null;

        for (Map.Entry<ModelParserType, ModelParserResults> entry : this.parsers.entrySet()) {
            if (entry.getValue().getValidationResult() == null) {
                validate(entry.getKey());
            }

            if (validationResult == null) {
                validationResult = entry.getValue().getValidationResult();
            } else {
                res &= validationResult.equals(entry.getValue().getValidationResult());
            }
        }

        return res;
    }

    public boolean doesMetadataFixerResultsEquals() throws ModelParsingException, IOException, ValidationException {
        boolean res = true;
        MetadataFixerResult metadataFixerResult = null;

        for (Map.Entry<ModelParserType, ModelParserResults> entry : this.parsers.entrySet()) {
            if (entry.getValue().getMetadataFixerResult() == null) {
                fixMetadata(entry.getKey());
            }

            if (metadataFixerResult == null) {
                metadataFixerResult = entry.getValue().getMetadataFixerResult();
            } else {
                res &= metadataFixerResult.equals(entry.getValue().getMetadataFixerResult());
            }
        }

        return res;
    }

    public boolean doesFeaturesCollectionsEquals() {
        boolean res = true;
        FeaturesCollection featuresCollection = null;

        for (Map.Entry<ModelParserType, ModelParserResults> entry : this.parsers.entrySet()) {
            if (entry.getValue().getFeaturesCollection() == null) {
                collectFeatures(entry.getKey());
            }

            if (featuresCollection == null) {
                featuresCollection = entry.getValue().getFeaturesCollection();
            } else {
                res &= featuresCollection.equals(entry.getValue().getFeaturesCollection());
            }
        }

        return res;
    }

    public long getTimeOfValidation(ModelParserType type) throws ValidationException, ModelParsingException {
        ModelParserResults modelParserResults = parsers.get(type);
        if (modelParserResults.getValidationResult() == null) {
            validate(type);
        }

        return modelParserResults.getValidationTime();
    }

    public long getTimeOfMetadataFixing(ModelParserType type) throws ModelParsingException, IOException, ValidationException {
        ModelParserResults modelParserResults = parsers.get(type);
        if (modelParserResults.getMetadataFixerResult() == null) {
            fixMetadata(type);
        }

        return modelParserResults.getMetadataFixerTime();
    }

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

    public MetadataFixerResult getMetadataFixerResult(ModelParserType type) throws ModelParsingException, IOException, ValidationException {
        ModelParserResults modelParserResults = parsers.get(type);
        if (modelParserResults.getMetadataFixerResult() == null) {
            fixMetadata(type);
        }

        return modelParserResults.getMetadataFixerResult();
    }

    public FeaturesCollection getFeaturesCollection(ModelParserType type) {
        ModelParserResults modelParserResults = parsers.get(type);
        if (modelParserResults.getFeaturesCollection() == null) {
            collectFeatures(type);
        }

        return modelParserResults.getFeaturesCollection();
    }

    private void validate(ModelParserType type) throws ValidationException, ModelParsingException {
        ModelParserResults res = this.parsers.get(type);
        ValidationModelParser parser = res.getParser();
        PDFAValidator validator = Validators.createValidator(this.profile, true);
        long startTime = System.currentTimeMillis();
        ValidationResult result = validator.validate(parser);
        long endTime = System.currentTimeMillis();
        res.setValidationResult(result, endTime - startTime);
    }

    private void fixMetadata(ModelParserType type) throws IOException, ValidationException, ModelParsingException {
        ModelParserResults res = this.parsers.get(type);
        if (res.getValidationResult() == null) {
            validate(type);
        }

        InputStream is = new FileInputStream(res.getTemp());
        File tempOut = File.createTempFile("tempOut", "");
        tempOut.deleteOnExit();
        FileOutputStream os = new FileOutputStream(tempOut);

        MetadataFixer fixer = MetadataFixerFactory.createModelParser(type);
        long startTime = System.currentTimeMillis();
        MetadataFixerResult fixerResult = fixer.fixMetadata(is, os, res.getValidationResult());
        long endTime = System.currentTimeMillis();
        res.setMetadataFixerResult(fixerResult, endTime - startTime);
    }

    private void collectFeatures(ModelParserType type) {
        // TODO: implement me
    }

    private static class ModelParserResults {
        private ValidationModelParser parser;
        private File temp;
        private ValidationResult validationResult = null;
        private long validationTime = 0;
        private MetadataFixerResult metadataFixerResult = null;
        private long metadataFixerTime = 0;
        private FeaturesCollection featuresCollection = null;
        private long featuresCollectionTime = 0;

        public ModelParserResults(ValidationModelParser parser, File temp) {
            if (parser == null) {
                throw new IllegalArgumentException("ModelParser can't be null");
            }
            if (temp == null) {
                throw new IllegalArgumentException("File can't be null");
            }
            this.parser = parser;
            this.temp = temp;
        }

        public ValidationModelParser getParser() {
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

        public FeaturesCollection getFeaturesCollection() {
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

        public void setFeaturesCollection(FeaturesCollection featuresCollection, long featuresCollectionTime) {
            this.featuresCollection = featuresCollection;
            this.featuresCollectionTime = featuresCollectionTime;
        }
    }
}
