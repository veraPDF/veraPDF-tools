package org.verapdf.tools;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.pdfbox.text.PDFTextStripper;
import org.verapdf.exceptions.InvalidPasswordException;
import org.verapdf.pd.PDDocument;
import org.verapdf.pd.PDOutlineDictionary;
import org.verapdf.pd.PDOutlineItem;

/**
 * @author Maxim Plushchou
 */
public class CorpusWikiGenerator {

	private static final String veraUrl = "https://github.com/veraPDF/veraPDF-corpus/archive/staging.zip";
	private static final String LINK_START = "https://raw.githubusercontent.com/veraPDF/veraPDF-corpus/staging/";
	private static final String STAGING = "staging";
	private static final String PDF_UA_1 = "PDF_UA-1";
	private static final String EXPECTED_MESSAGE = "expected message";
	private static final String PDF_EXTENSION = ".pdf";
	private static PrintWriter writer;
	private static String corpusPart;

	public static void main(String[] args) throws IOException {
		writer = new PrintWriter(new FileOutputStream("test.md"));
		File zipFile;
		try {
			zipFile = CorpusDownload.createTempFileFromCorpus(URI.create(veraUrl).toURL(), "corpusWiki");
		} catch (IOException excep) {
			throw new IllegalStateException(excep);
		}
		ZipFile zipSource = new ZipFile(zipFile);
		Enumeration<? extends ZipEntry> entries = zipSource.entries();
		SortedSet<ZipEntry> entriesSet = new TreeSet<>(new ZipEntryComparator());
		while (entries.hasMoreElements()) {
			entriesSet.add(entries.nextElement());
		}
		for (ZipEntry entry : entriesSet) {
			if (entry.isDirectory()) {
				printDirectory(entry);
 			} else if (entry.getName().endsWith(PDF_EXTENSION)) {
				try {
					printFileDescription(zipSource, entry);
				} catch (InvalidPasswordException e) {
					writer.println("Encrypted pdf");
					System.out.println(entry.getName() + ": Encrypted pdf");
				}
			}
		}
	}

	private static int getHeadingLevel(String directoryName) {
		return directoryName.length() - directoryName.replace("/","").length();
	}

	private static void printDirectory(ZipEntry entry) throws FileNotFoundException {
		String directoryName = entry.getName();
		directoryName = directoryName.substring(directoryName.indexOf(STAGING) + STAGING.length() + 1);
		int headingLevel = getHeadingLevel(directoryName);
		if (!directoryName.isEmpty()) {
			directoryName = directoryName.substring(0, directoryName.length() - 1);
		}
		if (!directoryName.isEmpty() && headingLevel > 0) {
			directoryName = directoryName.substring(directoryName.lastIndexOf("/") + 1);
		}
		if (directoryName.isEmpty()) {
			return;
		}
		if (headingLevel == 1) {
			corpusPart = directoryName;
			writer.flush();
			writer.close();
			writer = new PrintWriter(new FileOutputStream(directoryName + ".md"));
		} else {
			printHeading(directoryName, headingLevel);
		}
	}

	private static void printHeading(String directoryName, int headingLevel) {
		for (int i = 1; i < headingLevel; i++) {
			writer.print("#");
		}
		writer.println(" " + directoryName);
		writer.println();
	}

	private static void printFileDescription(ZipFile zipSource, ZipEntry entry) throws IOException {
		PDDocument document = new PDDocument(zipSource.getInputStream(entry));
		printFileName(entry);
		printLinkToFile(entry);
		PDOutlineDictionary outlines = document.getOutlines();
		if (outlines != null) {
			PDOutlineItem outlineItem = outlines.getFirst();
			if (outlineItem != null) {
				if (outlineItem.getTitle() == null) {
					writer.println(" null title");
				}
				if (PDF_UA_1.equals(corpusPart)) {
					printTextFromPDFUAOutlines(outlineItem);
				} else {
					printTextFromOutlines(outlineItem);
				}
			} else {
				printTextFromPagesContents(zipSource, entry);
			}
		} else {
			printTextFromPagesContents(zipSource, entry);
		}
		writer.println();
		document.close();
	}

	private static void printTextFromPagesContents(ZipFile zipSource, ZipEntry entry) throws IOException {
		org.apache.pdfbox.pdmodel.PDDocument pdDocument = org.apache.pdfbox.pdmodel.PDDocument.load(zipSource.getInputStream(entry));
		PDFTextStripper pdfStripper = new PDFTextStripper();
		String text = pdfStripper.getText(pdDocument);
		String[] messages = text.split("\n");
		int outlinesIndex = -1;
		for (int i = 0; i < messages.length; i++) {
			if (messages[i].contains("Outlines:")) {
				outlinesIndex = i;
			}
		}
		writer.print(": ");
		for (int i = outlinesIndex + 2; i < messages.length - 1; i++) {
			messages[i] = messages[i].replace("\r","");
			if (stringStartsWithLabel(messages[i])) {
				messages[i] = messages[i].substring(2);
			}
			if (i == messages.length - 2 || stringStartsWithLabel(messages[i + 1])) {
				messages[i] = messages[i] + ".";
				writer.println(messages[i]);
			} else {
				writer.print(messages[i] + " ");
			}
		}
		pdDocument.close();
	}

	private static boolean stringStartsWithLabel(String str) {
		return str.startsWith("- ") || str.startsWith("• ");
	}

	private static void printFileName(ZipEntry entry) {
		String fileName = entry.getName();
		fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
		writer.print("[" + fileName + "]");
	}

	private static void printLinkToFile(ZipEntry entry) {
		String fileLink = entry.getName();
		fileLink = fileLink.substring(fileLink.indexOf(STAGING) + STAGING.length() + 1).replace(" ", "%20");
		writer.print("(" + LINK_START + fileLink + ")");
	}

	private static void printTextFromPDFUAOutlines(PDOutlineItem outlineItem) {
		writer.print(": ");
		outlineItem = outlineItem.getNext();
		while (outlineItem != null && outlineItem.getNext() != null) {
			if (outlineItem.getTitle() != null && outlineItem.getTitle().length() < 15_000) {
				String title = outlineItem.getTitle();
				title = getCorrectMDString(title);
				title = title.replace("\n", "");
				if (!title.endsWith(".")) {
					title = title + ".";
				}
				writer.println(title);
				printChildrenOutlines(outlineItem.getFirst());
			}
			outlineItem = outlineItem.getNext();
		}
	}

	private static String getCorrectMDString(String str) {
		return str.replace("<", "\\<").replace(">","\\>");
	}

	private static void printChildrenOutlines(PDOutlineItem outlineItem) {
		while (outlineItem != null) {
			if (outlineItem.getTitle() != null) {
				writer.println(outlineItem.getTitle());
			}
			printChildrenOutlines(outlineItem.getFirst());
			outlineItem = outlineItem.getNext();
		}
	}

	private static void printTextFromOutlines(PDOutlineItem outlineItem) {
		boolean isPrinted = false;
		while (outlineItem != null) {
			isPrinted = printTitle(outlineItem.getTitle(), isPrinted);
			printTextFromOutlines(outlineItem.getFirst());
			outlineItem = outlineItem.getNext();
		}
	}

	private static boolean printTitle(String string, boolean isPrinted) {
		if (string.contains(EXPECTED_MESSAGE) || isPrinted) {
			String message = string.replace(EXPECTED_MESSAGE, "");
			if (message.length() < 15_000) {
				writer.println(message);
			}
			return true;
		}
		return false;
	}
}
