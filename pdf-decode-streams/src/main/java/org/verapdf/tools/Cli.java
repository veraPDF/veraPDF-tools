package org.verapdf.tools;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author Maxim Plushchov
 */
public class Cli {

	private static final String HELP = "Arguments: inputFile outputFile";

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println(HELP);
			return;
		}
		try (PDDocument document = PDDocument.load(new File(args[0]))) {
			for (PDPage page : document.getPages()) {
				page.setContents(new PDStream(document, page.getContents()));
				PDResources resources = page.getResources();
				for (COSName name : resources.getXObjectNames()) {
					PDXObject pdxObject = resources.getXObject(name);
					if (pdxObject instanceof PDFormXObject) {
						COSStream stream = pdxObject.getCOSObject();
						COSStream newStream = new PDStream(document, stream.createInputStream()).getCOSObject();
						for (Map.Entry<COSName, COSBase> entry : stream.entrySet()) {
							if (entry.getKey() != COSName.LENGTH && entry.getKey() != COSName.FILTER) {
								newStream.setItem(entry.getKey(), entry.getValue());
							}
						}
						resources.put(name, PDFormXObject.createXObject(newStream,
								((PDFormXObject)pdxObject).getResources()));
					}
				}
			}
			document.save(args[1]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
