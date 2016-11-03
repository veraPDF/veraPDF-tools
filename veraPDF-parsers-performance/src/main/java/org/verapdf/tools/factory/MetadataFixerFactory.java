package org.verapdf.tools.factory;

import org.verapdf.metadata.fixer.MetadataFixerEnum;
import org.verapdf.pdfa.MetadataFixer;
import org.verapdf.tools.performance.ModelParserType;

/**
 * @author Maksim Bezrukov
 */
public class MetadataFixerFactory {

    public static MetadataFixer createPDFBoxMetadataFixer() {
        return MetadataFixerEnum.BOX_INSTANCE.getInstance();
    }

    public static MetadataFixer createGreenfieldMetadataFixer() {
        // TODO: implement me
        return null;
    }

    public static MetadataFixer createModelParser(ModelParserType type){
        switch (type) {
            case PDFBOX:
                return createPDFBoxMetadataFixer();
            case GREENFIELD:
                return createGreenfieldMetadataFixer();
            default:
                throw new IllegalArgumentException("Parser that corresponds to argument type is not supported");
        }
    }
}
