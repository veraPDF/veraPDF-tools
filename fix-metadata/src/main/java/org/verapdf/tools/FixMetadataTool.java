package org.verapdf.tools;

import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.verapdf.metadata.fixer.gf.utils.DateConverter;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.xmp.XMPDateTimeFactory;

import java.io.*;
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
        File ouputFile = new File(args[1]);
        PDDocument pdDocument = PDDocument.load(new File(args[0]));
        PDFAFlavour flavour = PDFAFlavour.byFlavourId(args[2]);
        if (flavour == PDFAFlavour.NO_FLAVOUR) {
            PDMetadata newMetadata = new PDMetadata(pdDocument, new FileInputStream(args[2]), false);
            pdDocument.getDocumentCatalog().setMetadata(newMetadata);
        } else {
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            setInfoEntries(flavour, pdDocument, time);
            setDocumentVersion(pdDocument, flavour);
            setMetadata(pdDocument, ouputFile, flavour, time);
        }
        if (flavour == PDFAFlavour.PDFUA_2) {
            setNamespace(pdDocument);
            pdDocument.getDocumentCatalog().setOpenAction(null);
        }
        pdDocument.save(ouputFile);
        pdDocument.close();
    }

    private static void setInfoEntries(PDFAFlavour flavour, PDDocument pdDocument, Calendar time) {
        if (flavour == PDFAFlavour.PDFUA_2 || flavour.getPart() == PDFAFlavour.Specification.ISO_19005_4) {
            pdDocument.getDocument().getTrailer().setItem(COSName.INFO, null);
        } else {
            PDDocumentInformation pdInfo = pdDocument.getDocumentInformation();
            pdInfo.setProducer("veraPDF Test Builder 1.0");
            pdInfo.setCreator("veraPDF Test Builder");
            pdInfo.setAuthor("veraPDF Consortium");
            pdInfo.setModificationDate(time);
            Calendar creationDate = pdInfo.getCreationDate();
            if (creationDate == null) {
                pdInfo.setCreationDate(time);
            }
            pdInfo.setKeywords(null);
            pdInfo.setTitle(null);
            pdInfo.setSubject(null);
        }
    }

    private static void setDocumentVersion(PDDocument pdDocument, PDFAFlavour flavour) {
        if (flavour.getPart() == PDFAFlavour.Specification.ISO_19005_4) {
            pdDocument.getDocument().getTrailer().removeItem(COSName.INFO);
            pdDocument.setVersion(2.0f);
            pdDocument.getDocument().setVersion(2.0f);
        } else if (flavour.getPart() == PDFAFlavour.Specification.ISO_14289_2) {
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
        if (flavour.getPart() == PDFAFlavour.Specification.ISO_14289_2) {
            return "pdf-ua2-wtpdf.xmp";
        }
        return "pdf-a.xmp";
    }

    private static void setMetadata(PDDocument pdDocument, File file, PDFAFlavour flavour, Calendar time) {
        String resourceName = getResourceName(flavour);
        try (InputStream newXMPData = FixMetadataTool.class.getClassLoader().getResourceAsStream(resourceName)) {
            Scanner s = new Scanner(newXMPData).useDelimiter("\\A");
            String meta = s.hasNext() ? s.next() : "";
            Calendar created = null;
            if (flavour != PDFAFlavour.PDFA_4 && flavour != PDFAFlavour.PDFUA_2) {
                PDDocumentInformation pdInfo = pdDocument.getDocumentInformation();
                created = pdInfo != null ? pdInfo.getCreationDate() : null;
            }
            if (created == null) {
                created = time;
            }
            meta = meta.replace("CREATION_DATE", getXMPDate(created));
            meta = meta.replace("MOD_DATE", getXMPDate(time));

            if (flavour != PDFAFlavour.PDFUA_1 && flavour != PDFAFlavour.PDFUA_2) {
                meta = meta.replace("FLAVOUR_PART", String.valueOf(flavour.getPart().getPartNumber()));
                meta = meta.replace("FLAVOUR_LEVEL", PDFAFlavour.PDFA_4 != flavour ?
                        "pdfaid:conformance=\"" + flavour.getLevel().getCode().toUpperCase() + "\" " : "");
            } else {
                meta = meta.replace("TITLE", file.getName().substring(0, file.getName().length() - 4));
            }
            PDMetadata newMetadata = new PDMetadata(pdDocument, new ByteArrayInputStream(meta.getBytes()), false);
            pdDocument.getDocumentCatalog().setMetadata(newMetadata);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getXMPDate(Calendar date) {
        return XMPDateTimeFactory.createFromCalendar(DateConverter.toCalendar(DateConverter.toPDFDateFormat(date))).getISO8601String();
    }
    
    private static void setNamespace(PDDocument document) {
        PDStructureTreeRoot root = document.getDocumentCatalog().getStructureTreeRoot();

        if (root != null) {
            COSDictionary NS = getPDF2_0Namespace(root);

            COSBase base = root.getK();
            if (base instanceof COSDictionary) {
                COSDictionary dictionary = (COSDictionary) base;
                dictionary.setItem("NS", NS);
                return;
            }

            root.getKids().forEach((child) -> {
                if (child instanceof PDStructureElement) {
                    COSDictionary dictionary = ((PDStructureElement) child).getCOSObject();
                    dictionary.setItem("NS", NS);
                }
            });
        }
    }
    
    private static COSDictionary getPDF2_0Namespace(PDStructureTreeRoot root) {
        COSBase originalNamespaces = root.getCOSObject().getItem("Namespaces");
        
        if (originalNamespaces instanceof COSArray) {
            COSArray originalNamespacesArray = (COSArray) originalNamespaces;
            for (COSBase currentNamespace : originalNamespacesArray) {
                if (currentNamespace instanceof COSObject) {
                    currentNamespace = ((COSObject) currentNamespace).getObject();
                }
                if (currentNamespace instanceof COSDictionary) {
                    if ("http://iso.org/pdf2/ssn".equals(((COSDictionary) currentNamespace).getString("NS"))) {
                        return (COSDictionary) currentNamespace;
                    }
                }
            }
        }
        COSDictionary NS = new COSDictionary();
        NS.setName("Type", "Namespace");
        NS.setString("NS", "http://iso.org/pdf2/ssn");
        if (originalNamespaces instanceof COSArray) {
            ((COSArray) originalNamespaces).add(NS);
        } else {
            COSArray namespaces = new COSArray();
            namespaces.add(NS);
            root.getCOSObject().setItem("Namespaces", namespaces);
        }
        return NS;
    }
}
