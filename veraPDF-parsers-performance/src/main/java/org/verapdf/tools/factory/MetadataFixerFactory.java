package org.verapdf.tools.factory;

import org.verapdf.pdfa.MetadataFixer;
import org.verapdf.tools.performance.ModelParserType;

/**
 * @author Maksim Bezrukov
 */
public class MetadataFixerFactory {

    public static MetadataFixer createGreenfieldMetadataFixer() {
        // TODO: implement me
        return null;
    }

    public static MetadataFixer createModelParser(ModelParserType type){
        switch (type) {
            case GREENFIELD:
                return createGreenfieldMetadataFixer();
            default:
                throw new IllegalArgumentException("Parser that corresponds to argument type is not supported");
        }
    }
}
