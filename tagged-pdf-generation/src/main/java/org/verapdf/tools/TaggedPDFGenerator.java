package org.verapdf.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdfwriter.compress.CompressParameters;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureNode;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;

import java.util.Map;
import java.util.logging.Logger;
import java.util.HashMap;

public class TaggedPDFGenerator {
    private static final Logger LOGGER = Logger.getLogger(TaggedPDFGenerator.class.getCanonicalName());
    private static final Map<StructureType, String> files = new HashMap<>();
    static {
        files.put(StructureType.DIV, "transitionaltag_div_test");
        files.put(StructureType.NON_STRUCT, "transitionaltag_nonstruct_test");
        files.put(StructureType.PART, "transitionaltag_part_test");
        files.put(null, "all_inclusions_test");
    }
    
    public static void main(String[] args) {
        TaggedPDFGenerator taggedPDFGenerator = new TaggedPDFGenerator();

        try {
            taggedPDFGenerator.run(args);
        } catch (Exception ex) {
            LOGGER.severe("Error during pdf generation: " + ex.getMessage() + ", proccess stopped.");
            ex.printStackTrace();
        }
    }

    private Integer currentMCID = 1;
    private String folder;
    
    private String getWorkingDir() {
        if (folder != null && (new File(folder)).exists()) {
            return folder;
        }

        File file = new File(System.getProperty("user.dir") + "\\generated_files");
        file.mkdirs();

        return file.getAbsolutePath();
    }

    private void run(String[] args) throws IOException {
        if (args.length > 0){
            folder = args[0];
        }

        String parentFolder = getWorkingDir();

        for (Map.Entry<StructureType, String> entry : files.entrySet()) {
            StructureType type = entry.getKey();
            currentMCID = 1;

            PDDocument document;
            if (type == null) {
                document = allInclusionsPDF();
            } else {
                document = transitionalPDF(type);
            }

            File file = Paths.get(parentFolder).resolve(entry.getValue() + ".pdf").toFile();

            document.save(file, CompressParameters.NO_COMPRESSION);
            document.close();
        }
    }

    private PDDocument allInclusionsPDF() throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        PDStructureTreeRootAccess treeRoot = new PDStructureTreeRootAccess();
        COSDictionary namespace2_0 = getPDF2_0Namespace(treeRoot);


        document.addPage(page);
        catalog.setStructureTreeRoot(treeRoot);

        PDPageContentStream content = new PDPageContentStream(document, page, AppendMode.OVERWRITE, false);

        treeRoot.appendKid(textContent(content, " "));
        treeRoot.appendKid(textContent(content, " "));
        for (StructureType type : StructureType.values()) {
            if (type.isTransitional()) {
                continue;
            }
            PDStructureElement element = createStructureElement(type, treeRoot, namespace2_0, page);
            element.appendKid(textContent(content, " "));
            element.appendKid(textContent(content, " "));
            for (StructureType subType : StructureType.values()) {
                if (subType.isTransitional()) {
                    continue;
                }
                for (int index = 0; index < 2; index++) {
                    createStructureElement(subType, element, namespace2_0, page);
                }
            }
            createStructureElement(type, treeRoot, namespace2_0, page);
        }

        content.close();

        return document;
    }

    private PDDocument transitionalPDF(StructureType transitionalType) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        PDStructureTreeRoot treeRoot = new PDStructureTreeRoot();
        COSDictionary namespace2_0 = getPDF2_0Namespace(treeRoot);

        document.addPage(page);
        catalog.setStructureTreeRoot(treeRoot);

        PDPageContentStream content = new PDPageContentStream(document, page, AppendMode.OVERWRITE, false);

        for (StructureType type : StructureType.values()) {
            PDStructureElement element = createStructureElement(type, treeRoot, namespace2_0, page);
            PDStructureElement transitionalElement = createStructureElement(transitionalType, element, namespace2_0, page);
            transitionalElement.appendKid(textContent(content, " "));
            transitionalElement.appendKid(textContent(content, " "));
            for (StructureType subType : StructureType.values()) {
                for (int index = 0; index < 2; index++) {
                    createStructureElement(subType, transitionalElement, namespace2_0, page);
                }
            }
            createStructureElement(transitionalType, element, namespace2_0, page);
            treeRoot.appendKid(element);
        }

        content.close();

        return document;
    }

    private PDMarkedContent textContent(PDPageContentStream content, String text) throws IOException {
        content.beginText();
        content.setFont(new PDType1Font(FontName.HELVETICA_BOLD), 14);
        COSDictionary dictionary = new COSDictionary();
        dictionary.setInt(COSName.MCID, currentMCID);
        currentMCID++;
        content.beginMarkedContent(COSName.P, PDPropertyList.create(dictionary));
        content.showText(text);
        content.endMarkedContent();
        PDMarkedContent markedContent = new PDMarkedContent(COSName.P, dictionary);
        content.endText();

        return markedContent;
    }
    
    private static PDStructureElement createStructureElement(StructureType type, PDStructureNode parent,
                                                             COSDictionary namespace2_0, PDPage page) {
        PDStructureElement structureElement = new PDStructureElement(type.getType(), parent);
        if (type.is2_0()) {
            structureElement.getCOSObject().setItem(COSName.getPDFName("NS"), namespace2_0);
        }
        parent.appendKid(structureElement);
        structureElement.setPage(page);
        return structureElement;
    }

    private static COSDictionary getPDF2_0Namespace(PDStructureTreeRoot root) {
        COSDictionary NS = new COSDictionary();
        NS.setName("Type", "Namespace");
        NS.setString("NS", "http://iso.org/pdf2/ssn");
        COSArray namespaces = new COSArray();
        namespaces.add(NS);
        root.getCOSObject().setItem("Namespaces", namespaces);
        return NS;
    }
}
