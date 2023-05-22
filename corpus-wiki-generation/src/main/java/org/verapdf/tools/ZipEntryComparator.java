package org.verapdf.tools;

import java.util.Comparator;
import java.util.zip.ZipEntry;

public class ZipEntryComparator implements Comparator<ZipEntry> {

	@Override
	public int compare(ZipEntry o1, ZipEntry o2) {
		String name1 = o1.getName();
		String name2 = o2.getName();
		return compare(name1, name2);
	}

	public int compare(String name1, String name2) {
		int commonLength = getCommonStartLength(name1, name2);
		int start = getNotNumberStartLength(name1, commonLength);
		String substring1 = name1.substring(start);
		String substring2 = name2.substring(start);
		substring1 = substring1.substring(0, getNumberStartLength(substring1));
		substring2 = substring2.substring(0, getNumberStartLength(substring2));
		Integer int1 = getIntegerFromString(substring1);
		Integer int2 = getIntegerFromString(substring2);
		if (int1 != null && int2 != null && !int1.equals(int2)) {
			return int1 - int2;
		}
		return name1.compareTo(name2);
	}

	public static int getCommonStartLength(String s1, String s2) {
		return getCommonStartLength(s1, s2, Math.min(s1.length(), s2.length()));
	}

	private static int getCommonStartLength(String s1, String s2, int length) {
		for (int i = 0; i < length; i++) {
			if (s1.charAt(i) != s2.charAt(i)) {
				return i;
			}
		}
		return length;
	}

	protected static int getNotNumberStartLength(String string, int commonStartLength) {
		return getNotRegexStartLength(string, commonStartLength, "\\d+");
	}

	private static int getNotRegexStartLength(String string, int commonStartLength, String regex) {
		if (commonStartLength == 0) {
			return 0;
		}
		for (int i = commonStartLength; i > 0; i--) {
			if (!string.substring(i - 1, i).matches(regex)) {
				return i;
			}
		}
		return 0;
	}

	protected static int getNumberStartLength(String string) {
		return getRegexStartLength(string, "\\d+");
	}

	public static int getRegexStartLength(String string, String regex) {
		for (int i = 0; i < string.length(); i++) {
			if (!string.substring(i, i + 1).matches(regex)) {
				return i;
			}
		}
		return string.length();
	}

	public static Integer getIntegerFromString(String string) {
		try {
			return Integer.parseUnsignedInt(string);
		} catch (NumberFormatException ignored) {
		}
		return null;
	}
}
