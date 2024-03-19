package org.verapdf.tools;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.util.List;

public class Utils {

	public static void removeAllSecurity(PDDocument pdDocument) {
		try {
			pdDocument.setAllSecurityToBeRemoved(true);
		} catch (Exception e) {
			throw new IllegalStateException("The document is encrypted, and we can't decrypt it.", e);
		}
	}

	public static void nameCheck(List<Long> list, COSDictionary dictionary) {
		if (dictionary.containsKey(COSName.NAME)) {
			if (COSName.TYPE1.equals(dictionary.getDictionaryObject(COSName.SUBTYPE)) ||
			    (COSName.TYPE3).equals(dictionary.getDictionaryObject(COSName.SUBTYPE)) ||
			    (COSName.IMAGE).equals(dictionary.getDictionaryObject(COSName.SUBTYPE)) ||
			    (COSName.FORM).equals(dictionary.getDictionaryObject(COSName.SUBTYPE)) ||
			    (COSName.TRUE_TYPE).equals(dictionary.getDictionaryObject(COSName.SUBTYPE))) {

				if (!list.contains(dictionary.getKey().getNumber())) {
					list.add(dictionary.getKey().getNumber());
				}
			}
		}
	}

}
