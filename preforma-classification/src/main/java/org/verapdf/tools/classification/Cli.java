package org.verapdf.tools.classification;

import java.io.IOException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

/**
 * @author Maksim Bezrukov
 *
 */
public final class Cli {
	private static final String APP_NAME = "";

	private Cli() {
		// disable default constructor
	}

	/**
	 * Main CLI entry point, process the command line arguments
	 *
	 * @param args
	 *            Java.lang.String array of command line args, to be processed
	 *            using Apache commons CLI.
	 */
	public static void main(final String[] args) {
		CliArgParser cliArgParser = new CliArgParser();
		JCommander jCommander = new JCommander(cliArgParser);
		jCommander.setProgramName(APP_NAME);

		try {
			jCommander.parse(args);
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			jCommander.usage();
			System.exit(1);
		}
		if (cliArgParser.isHelp()) {
			jCommander.usage();
			System.exit(0);
		}

		if (!(cliArgParser.getReportPaths().isEmpty() && cliArgParser.isHelp())) {
			try {
				CliProcessor processor = CliProcessor.createProcessorFromArgs(cliArgParser);
				processor.processPaths(cliArgParser.getReportPaths());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
