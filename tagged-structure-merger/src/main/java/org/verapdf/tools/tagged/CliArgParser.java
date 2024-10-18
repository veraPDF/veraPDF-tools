package org.verapdf.tools.tagged;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.verapdf.tools.tagged.enums.PDFVersion;

import java.util.List;

/**
 * @author Maksim Bezrukov
 */
public class CliArgParser {
	final static CliArgParser DEFAULT_ARGS = new CliArgParser();
	final static String FLAG_SEP = "-";
	final static String OPTION_SEP = "--";
	final static String HELP_FLAG = FLAG_SEP + "h";
	final static String HELP = OPTION_SEP + "help";
	final static String OUTPUT_FLAG = FLAG_SEP + "o";
	final static String OUTPUT = OPTION_SEP + "out";
	final static String PROFILE_NAME = OPTION_SEP + "name";
	final static String PROFILE_DESCRIPTION = OPTION_SEP + "description";
	final static String PROFILE_CREATOR = OPTION_SEP + "creator";
	final static String PDF_VERSION_FLAG = FLAG_SEP + "v";
	final static String PDF_VERSION = OPTION_SEP + "pdf-version";

	@Parameter(names = { HELP_FLAG, HELP }, description = "Shows this message and exits.", help = true)
	private boolean help = false;

	@Parameter(names = { PDF_VERSION_FLAG, PDF_VERSION }, description = "PDF version.", converter = PDFVersionConverter.class)
	private PDFVersion pdfVersion = PDFVersion.PDF_2_0;

	@Parameter(names = { OUTPUT_FLAG, OUTPUT }, description = "Output path. If the value is not specified, then System Out is using")
	private String outputPath = null;

	@Parameter(names = { PROFILE_NAME }, description = "The name of generated profile")
	private String name = "Tagged pdf profile";

	@Parameter(names = { PROFILE_DESCRIPTION }, description = "The description of generated profile")
	private String description = "Profile for validation of tagged pdf structure tree relations";

	@Parameter(names = { PROFILE_CREATOR }, description = "The creator of generated profile")
	private String creator = "veraPDF Consortium";

	@Parameter(description = "INPUT_CSV", required = false)
	private List<String> csvPath = null;
	/**
	 * @return true if help requested
	 */
	public boolean isHelp() {
		return this.help;
	}

	public PDFVersion getPdfVersion() {
		return pdfVersion;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getCreator() {
		return creator;
	}

	public String getOutputPath() {
		return outputPath;
	}

	/**
	 * @return the list of file paths
	 */
	public String getCsvPath() {
		return this.csvPath != null ? this.csvPath.get(0) : null;
	}


	public static final class PDFVersionConverter implements IStringConverter<PDFVersion> {
		/**
		 * { @inheritDoc }
		 */
		@Override
		public PDFVersion convert(final String value) {
			for (PDFVersion version : PDFVersion.values()) {
				if (version.toString().equalsIgnoreCase(value))
					return version;
			}
			throw new ParameterException("Illegal --pdf-version argument:" + value);
		}

	}
}
