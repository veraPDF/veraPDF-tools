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
	final static String VERSION = OPTION_SEP + "version";
	final static String XSLT = OPTION_SEP + "xslt";

	@Parameter(names = { HELP_FLAG, HELP }, description = "Shows this message and exits.", help = true)
	private boolean help = false;

	@Parameter(names = { VERSION }, description = "Version information for output.", required = true)
	private String version;

	@Parameter(names = { XSLT }, description = "XSLT for transformation.", required = true)
	private String xslt;

	@Parameter(description = "REPORT", required = true)
	private List<String> reportPaths = new ArrayList<>();

	public String version() {
		return this.version;
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

	/**
	 * @return the list of file paths
	 */
	public List<String> getReportPaths() {
		return this.reportPaths;
	}

}
