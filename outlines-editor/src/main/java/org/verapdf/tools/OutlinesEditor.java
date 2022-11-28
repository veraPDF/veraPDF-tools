package org.verapdf.tools;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

import java.io.*;

public class OutlinesEditor {

	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			System.out.println("Parameters: input pdf file, input text file, output pdf file");
			return;
		}
		PDDocument document = PDDocument.load(new File(args[0]));
		PDDocumentOutline outlines = new PDDocumentOutline();
		document.getDocumentCatalog().setDocumentOutline(outlines);
		PDOutlineItem outline = new PDOutlineItem();
		outlines.addFirst(outline);
		try (BufferedReader reader = new BufferedReader(new FileReader(args[1]))) {
			String line = reader.readLine();
			outline.setTitle(line);
			line = reader.readLine();
			while(line != null && !line.isEmpty()) {
				PDOutlineItem newOutline = new PDOutlineItem();
				newOutline.setTitle(line);
				outline.insertSiblingAfter(newOutline);
				outline = newOutline;
				line = reader.readLine();
			}
		}
		document.save(new File(args[2]));
		document.close();
	}
}
