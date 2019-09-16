package org.verapdf.tools.tagged;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import javax.xml.bind.JAXBException;
import java.io.*;

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
	public static void main(final String[] args) throws IOException, JAXBException {
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

		if (cliArgParser.getCsvPath() != null) {
			CliProcessor processor = CliProcessor.createProcessorFromArgs(cliArgParser);
			String outputPath = cliArgParser.getOutputPath();
			if (outputPath != null) {
				try (OutputStream out = new FileOutputStream(new File(outputPath))) {
					processor.process(cliArgParser.getCsvPath(), out);
				}
			} else {
				processor.process(cliArgParser.getCsvPath(), System.out);
			}
		}
	}
}
