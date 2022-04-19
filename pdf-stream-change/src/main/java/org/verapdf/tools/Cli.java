package org.verapdf.tools;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSObjectKey;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDStream;

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
		if (READ.equals(args[0])) {
			COSBase base = object.getObject();
			if (!(base instanceof COSStream)) {
				System.out.println("Object with number " + objectNumber + " not a stream.");
				return;
			}
			COSStream stream = (COSStream)base;
			try (InputStream in = stream.createInputStream(); OutputStream out = new FileOutputStream(textFileName)) {
				IOUtils.copy(in, out);
			}
		} else if (WRITE.equals(args[0])) {
			try (InputStream in = new FileInputStream(args[2])) {
				object.setObject(new PDStream(document, in).getCOSObject());
			}
		} else {
			System.out.println(INFO);
		}
		document.save(pdfFileName);
		document.close();
	}

}
