package org.verapdf.tools.classification;

import com.beust.jcommander.Parameter;

import java.util.ArrayList;
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
	final static String RANTAG = OPTION_SEP + "ranTag";
	final static String XSLT = OPTION_SEP + "xslt";
	final static String CONSOLE = OPTION_SEP + "console";
	final static String PASSED = OPTION_SEP + "passed";
	final static String FILENAME = OPTION_SEP + "fileName";

	@Parameter(names = { HELP_FLAG, HELP }, description = "Shows this message and exits.", help = true)
	private boolean help = false;

	@Parameter(names = { RANTAG }, description = "Ran tag.")
	private String ranTag = "";

	@Parameter(names = { FILENAME }, description = "Custom file name.")
	private String fileName;

	@Parameter(names = { XSLT }, description = "XSLT for transformation.", required = true)
	private String xslt;

	@Parameter(names = { CONSOLE }, description = "Changes output to console.")
	private boolean console = false;

	@Parameter(names = { PASSED }, description = "Shows only passed results for all classes.")
	private boolean passed = false;

	@Parameter(description = "REPORT", required = true)
	private List<String> reportPaths = new ArrayList<>();

	public String ranTag() {
		return this.ranTag;
	}

	public String fileName() {
		return this.fileName;
	}

	/**
	 * @return true if help requested
	 */
	public boolean isHelp() {
		return this.help;
	}

	public String xslt() {
		return xslt;
	}

	public boolean console() {
		return console;
	}

	public boolean passed() {
		return passed;
	}

	/**
	 * @return the list of file paths
	 */
	public List<String> getReportPaths() {
		return this.reportPaths;
	}

}
