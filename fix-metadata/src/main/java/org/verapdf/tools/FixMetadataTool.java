package org.verapdf.tools;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.verapdf.metadata.fixer.gf.utils.DateConverter;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.xmp.XMPDateTimeFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

public class FixMetadataTool {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("arguments: inputFile outputFile flavourId/xmpFileName");
            System.out.println("possible flavourIds: " +
                    Arrays.toString(PDFAFlavour.values())
                            .replaceFirst("0, ", "")
                            .replace(", wcag2", ""));
            return;
        }
        PDDocument pdDocument = PDDocument.load(new File(args[0]));
        PDFAFlavour flavour = PDFAFlavour.byFlavourId(args[2]);
        if (flavour == PDFAFlavour.NO_FLAVOUR) {
            PDMetadata newMetadata = new PDMetadata(pdDocument, new FileInputStream(args[2]));
            pdDocument.getDocumentCatalog().setMetadata(newMetadata);
        } else {
            PDDocumentInformation pdInfo = pdDocument.getDocumentInformation();
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            setInfoEntries(pdInfo, time);
            setDocumentVersion(pdDocument, flavour);
            setMetadata(pdDocument, flavour, pdInfo.getCreationDate(), time);
        }
        pdDocument.save(args[1]);
        pdDocument.close();
    }

    private static void setInfoEntries(PDDocumentInformation pdInfo, Calendar time) {
        pdInfo.setProducer("veraPDF Test Builder 1.0");
        pdInfo.setCreator("veraPDF Test Builder");
        pdInfo.setAuthor("veraPDF Consortium");
        pdInfo.setKeywords(null);
        pdInfo.setTitle(null);
        pdInfo.setSubject(null);
        pdInfo.setModificationDate(time);
        Calendar creationDate = pdInfo.getCreationDate();
        if (creationDate == null) {
            pdInfo.setCreationDate(time);
        }

    }

    private static void setDocumentVersion(PDDocument pdDocument, PDFAFlavour flavour) {
        if (flavour.getPart() == PDFAFlavour.Specification.ISO_19005_4) {
            pdDocument.getDocument().getTrailer().removeItem(COSName.INFO);
            pdDocument.setVersion(2.0f);
            pdDocument.getDocument().setVersion(2.0f);
        } else if (flavour.getPart() == PDFAFlavour.Specification.ISO_19005_1) {
            pdDocument.getDocument().setVersion(1.4f);
        } else {
            pdDocument.getDocument().setVersion(1.7f);
        }
    }

    private static String getResourceName(PDFAFlavour flavour) {
        if (flavour.getPart() == PDFAFlavour.Specification.ISO_19005_4) {
            return "pdf-a4.xmp";
        }
        if (flavour.getPart() == PDFAFlavour.Specification.ISO_19005_1) {
            return "pdf-a.xmp";
        }
        if (flavour.getPart() == PDFAFlavour.Specification.ISO_14289_1) {
            return "pdf-ua1.xmp";
        }
        return "pdf-a.xmp";
    }

    private static void setMetadata(PDDocument pdDocument, PDFAFlavour flavour, Calendar creationDate, Calendar time) {
        String resourceName = getResourceName(flavour);
        try (InputStream newXMPData = FixMetadataTool.class.getClassLoader().getResourceAsStream(resourceName)) {
            Scanner s = new Scanner(newXMPData).useDelimiter("\\A");
            String meta = s.hasNext() ? s.next() : "";
            meta = meta.replace("CREATION_DATE", getXMPDate(creationDate));
            meta = meta.replace("MOD_DATE", getXMPDate(time));

            if (flavour != PDFAFlavour.PDFUA_1) {
                meta = meta.replace("FLAVOUR_PART", String.valueOf(flavour.getPart().getPartNumber()));
                meta = meta.replace("FLAVOUR_LEVEL", PDFAFlavour.PDFA_4 != flavour ?
                        "pdfaid:conformance=\"" + flavour.getLevel().getCode().toUpperCase() + "\" " : "");
            }
            PDMetadata newMetadata = new PDMetadata(pdDocument, new ByteArrayInputStream(meta.getBytes()));
            pdDocument.getDocumentCatalog().setMetadata(newMetadata);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getXMPDate(Calendar date) {
        return XMPDateTimeFactory.createFromCalendar(DateConverter.toCalendar(DateConverter.toPDFDateFormat(date))).getISO8601String();
    }
}
