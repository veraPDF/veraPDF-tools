package org.verapdf.tools;

import org.apache.commons.io.IOUtils;
import org.verapdf.as.io.ASInputStream;
import org.verapdf.as.io.ASMemoryInStream;
import org.verapdf.cos.COSKey;
import org.verapdf.cos.COSObjType;
import org.verapdf.cos.COSObject;
import org.verapdf.cos.COSStream;
import org.verapdf.io.SeekableInputStream;
import org.verapdf.pd.PDDocument;
import org.verapdf.pd.font.type1.EexecFilterDecode;

import java.io.*;

/**
 * @author Maxim Plushchov
 */
public class CliDecodeFonts {

	private static final String HELP = "Arguments: font file object number, file name";
	private static final String NOT_STREAM = "The object is not a stream";

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println(HELP);
			return;
		}
		PDDocument document = null;
		try {
			document = new PDDocument(args[0]);
			COSObject object = document.getDocument().getObject(new COSKey(Integer.parseInt(args[1])));
			if (object.getType() == COSObjType.COS_STREAM) {
				try (ASInputStream in = object.getData(COSStream.FilterFlags.DECODE);
					 PrintStream fontPrint = new PrintStream("font.txt")) {
					byte[] bytes = IOUtils.toByteArray(in);
					int k = new String(bytes).indexOf("eexec") + 6;
					if (k == 5) {
						System.out.println("not eexec");
						return;
					}
					fontPrint.write(bytes);
					byte[] bytes1 = new byte[bytes.length - k];
					System.arraycopy(bytes, k, bytes1, 0, bytes.length - k);
					try (ASInputStream eexecDecoded = new EexecFilterDecode(new ASMemoryInStream(bytes1), false);
						 PrintWriter privatePartWriter = new PrintWriter("private_part.txt");
						 SeekableInputStream inputStream = SeekableInputStream.getSeekableStream(eexecDecoded)) {
						IOUtils.copy(inputStream, privatePartWriter);
					}
					try (ASInputStream eexecDecoded = new EexecFilterDecode(new ASMemoryInStream(bytes1), false);
						 PrintWriter privatePartBytesWriter = new PrintWriter("private_part_bytes.txt")) {
						for (byte b : IOUtils.toByteArray(eexecDecoded)) {
							privatePartBytesWriter.print((b & 255) + " ");
						}
					}
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
