package org.verapdf.tools;

import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;

/**
 * Allows to add PDMarkedContent in PDStructureTreeRoot
 */
public class PDStructureTreeRootAccess extends PDStructureTreeRoot {
    public PDStructureTreeRootAccess() {
        super();
    }

    public void appendKid(PDMarkedContent markedContent) {
        if (markedContent == null) {
            return;
        }
        this.appendKid(COSInteger.get(markedContent.getMCID()));
    }
}
