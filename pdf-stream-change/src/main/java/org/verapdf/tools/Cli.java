package org.verapdf.tools;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.*;

/**
 * @author Maxim Plushchov
 */
public class Cli {

	private static final String HELP = "Arguments: write/read pdfFile textFile objectNumber";
	private static final String INFO = "First argument: write or read";
	private static final String WRITE = "write";
	private static final String READ = "read";

	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.out.println(HELP);
			return;
		}
		String pdfFileName = args[1];
		String textFileName = args[2];
		Integer objectNumber = Integer.decode(args[3]);
		PDDocument document = PDDocument.load(new File(pdfFileName));
		COSObject object = document.getDocument().getObjectFromPool(new COSObjectKey(objectNumber, 0));
		if (object == null) {
			System.out.println("Object with number " + objectNumber + " not found.");
			return;
		}
		COSBase base = object.getObject();
		if (!(base instanceof COSStream)) {
			System.out.println("Object with number " + objectNumber + " not a stream.");
			return;
		}
		COSStream stream = (COSStream)base;
		if (READ.equals(args[0])) {
			try (InputStream in = stream.createInputStream(); OutputStream out = new FileOutputStream(textFileName)) {
				IOUtils.copy(in, out);
			}
		} else if (WRITE.equals(args[0])) {
			stream.setItem(COSName.FILTER, null);
			try (InputStream in = new FileInputStream(textFileName); OutputStream out = stream.createOutputStream()) {
				IOUtils.copy(in, out);
			}
		} else {
			System.out.println(INFO);
		}
		document.save(pdfFileName);
		document.close();
	}

}
