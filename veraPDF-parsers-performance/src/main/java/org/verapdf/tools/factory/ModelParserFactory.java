package org.verapdf.tools.factory;

import org.verapdf.core.EncryptedPdfException;
import org.verapdf.core.ModelParsingException;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.tools.performance.ModelParserType;

import java.io.InputStream;

/**
 * @author Maksim Bezrukov
 */
public class ModelParserFactory {

    private ModelParserFactory() {
    }

    public static PDFAParser createPDFBoxModelParser(InputStream toLoad, PDFAFlavour flavour) throws ModelParsingException, EncryptedPdfException {
        return org.verapdf.gf.model.GFModelParser.createModelWithFlavour(toLoad, flavour);
    }

    public static PDFAParser createGreenfieldModelParser(InputStream toLoad, PDFAFlavour flavour) throws ModelParsingException, EncryptedPdfException {
        return org.verapdf.gf.model.GFModelParser.createModelWithFlavour(toLoad, flavour);
    }

    public static PDFAParser createModelParser(ModelParserType type, InputStream toLoad, PDFAFlavour flavour) throws ModelParsingException, EncryptedPdfException {
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
