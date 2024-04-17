package org.verapdf.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdfwriter.compress.CompressParameters;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;

import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Set;

public class TaggedPDFGenerator {
    private static Logger logger = Logger.getLogger("");
    private static Set<String> types = new TreeSet<String>();
    private static Set<String> typesExcludeTransit = new TreeSet<String>();
    private static Set<String> transitionalTypes = new TreeSet<String>();
    private static HashMap<String, String> files = new HashMap<String, String>();
    static {
        transitionalTypes.add(StructureType.DIV.string());
        transitionalTypes.add(StructureType.NON_STRUCT.string());
        transitionalTypes.add(StructureType.PART.string());

        for (StructureType type : StructureType.values()) {
            types.add(type.string());
            typesExcludeTransit.add(type.string());
        }

        for (String type : transitionalTypes) {
            typesExcludeTransit.remove(type);
        }

        files.put(StructureType.DIV.string(), "transitionaltag_div_test");
        files.put(StructureType.NON_STRUCT.string(), "transitionaltag_nonstruct_test");
        files.put(StructureType.PART.string(), "transitionaltag_part_test");
        files.put("all_inclusions", "all_inclusions_test");
    }
    
    public static void main(String[] args) {
        TaggedPDFGenerator taggedPDFGenerator = new TaggedPDFGenerator();

        try {
            taggedPDFGenerator.run(args);
        } catch (Exception ex) {
            logger.severe("Error during pdf generation: " + ex.getMessage() + ", proccess stopped.");
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

        for (String type : files.keySet()) {
            currentMCID = 1;

            PDDocument document;
            if (type.equals("all_inclusions")) {
                document = allInclusionsPDF(type);
            } else {
                document = transitionalPDF(type);
            }

            File file = Paths.get(parentFolder).resolve(files.get(type) + ".pdf").toFile();

            document.save(file, CompressParameters.NO_COMPRESSION);
            document.close();
        }
    }

    private PDDocument allInclusionsPDF(String pdftype) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        PDStructureTreeRootAccess treeRoot = new PDStructureTreeRootAccess();

        document.addPage(page);
        catalog.setStructureTreeRoot(treeRoot);

        PDPageContentStream content = new PDPageContentStream(document, page, AppendMode.OVERWRITE, false);

        treeRoot.appendKid(textContent(content, " "));
        treeRoot.appendKid(textContent(content, " "));
        for (String type : typesExcludeTransit) {
            PDStructureElement element = new PDStructureElement(type, treeRoot);

            element.appendKid(textContent(content, " "));
            element.appendKid(textContent(content, " "));
            for (String subType : typesExcludeTransit) {
                for (Integer index = 0; index < 2; index++) {
                    PDStructureElement subElement = new PDStructureElement(subType, element);
                    subElement.setPage(page);

                    element.appendKid(subElement);
                }
            }

            element.setPage(page);
            treeRoot.appendKid(element);

            element = new PDStructureElement(type, treeRoot);
            element.setPage(page);
            treeRoot.appendKid(element);
        }

        content.close();

        return document;
    }

    private PDDocument transitionalPDF(String transitionalType) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        PDStructureTreeRoot treeRoot = new PDStructureTreeRoot();

        document.addPage(page);
        catalog.setStructureTreeRoot(treeRoot);

        PDPageContentStream content = new PDPageContentStream(document, page, AppendMode.OVERWRITE, false);

        for (String type : types) {
            PDStructureElement element = new PDStructureElement(type, treeRoot);
            PDStructureElement transitionalElement = new PDStructureElement(transitionalType, element);

            transitionalElement.appendKid(textContent(content, " "));
            transitionalElement.appendKid(textContent(content, " "));
            for (String subType : types) {
                for (Integer index = 0; index < 2; index++) {
                    PDStructureElement sub_element = new PDStructureElement(subType, transitionalElement);
                    sub_element.setPage(page);

                    transitionalElement.appendKid(sub_element);
                }
            }

            element.setPage(page);
            element.appendKid(transitionalElement);

            transitionalElement = new PDStructureElement(transitionalType, element);
            element.appendKid(transitionalElement);
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
}
