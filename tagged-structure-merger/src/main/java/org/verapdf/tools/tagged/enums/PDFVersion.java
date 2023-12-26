package org.verapdf.tools.tagged.enums;

/**
 * @author Maksim Bezrukov
 */
public enum PDFVersion {
	PDF_1_7("PDF-1.7", "ISO-32000-1"),
	PDF_2_0("PDF-2.0", "ISO-32000-2");

	private String name;
	private String iso;

	PDFVersion(String name, String iso) {
		this.name = name;
		this.iso = iso;
	}

	public String getIso() {
		return iso;
	}

	@Override
	public String toString() {
		return this.name;
	}
}
