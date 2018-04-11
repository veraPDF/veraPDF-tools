package org.verapdf.tools;

import org.apache.commons.io.IOUtils;
import org.verapdf.cos.COSKey;
import org.verapdf.cos.COSObjType;
import org.verapdf.cos.COSObject;
import org.verapdf.cos.COSStream;
import org.verapdf.pd.PDDocument;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Maksim Bezrukov
 */
public class Cli {

	private static final String HELP = "Arguments: objectNumber fileName";
	private static final String NOT_STREAM = "The object is not a stream";

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println(HELP);
			return;
		}
		PDDocument document = null;
		try {
			document = new PDDocument(args[1]);
			COSObject object = document.getDocument().getObject(new COSKey(Integer.parseInt(args[0])));
			if (object.getType() == COSObjType.COS_STREAM) {
				try (InputStream in = object.getData(COSStream.FilterFlags.DECODE)) {
					IOUtils.copy(in, System.out);
					System.out.println();
				}
			} else {
				System.out.println(NOT_STREAM);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (document != null) {
				document.close();
			}
		}
	}
}
