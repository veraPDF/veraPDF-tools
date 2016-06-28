package org.verapdf.tools.factory;

import org.verapdf.core.ModelParsingException;
import org.verapdf.pdfa.PDFParser;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.tools.performance.ModelParserType;

import java.io.InputStream;

/**
 * @author Maksim Bezrukov
 */
public class ModelParserFactory {

    private ModelParserFactory() {
    }

    public static PDFParser createPDFBoxModelParser(InputStream toLoad, PDFAFlavour flavour) throws ModelParsingException {
        return org.verapdf.model.ModelParser.createModelWithFlavour(toLoad, flavour);
    }

    public static PDFParser createGreenfieldModelParser(InputStream toLoad, PDFAFlavour flavour) {
        // TODO: implement me
        return null;
    }

    public static PDFParser createModelParser(ModelParserType type, InputStream toLoad, PDFAFlavour flavour) throws ModelParsingException {
        switch (type) {
            case PDFBOX:
                return createPDFBoxModelParser(toLoad, flavour);
            case GREENFIELD:
                return createGreenfieldModelParser(toLoad, flavour);
            default:
                throw new IllegalArgumentException("Parser that corresponds to argument type is not supported");
        }
    }
}
