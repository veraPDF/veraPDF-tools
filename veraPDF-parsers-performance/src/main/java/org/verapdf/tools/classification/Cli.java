package org.verapdf.tools.classification;

import java.io.IOException;

import org.verapdf.ReleaseDetails;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.verapdf.pdfa.validation.profiles.ProfileDirectory;
import org.verapdf.pdfa.validation.profiles.Profiles;
import org.verapdf.pdfa.validation.profiles.ValidationProfile;

/**
 * @author Maksim Bezrukov
 *
 */
public final class Cli {
	private static final String APP_NAME = "veraPDFConformance";
	private static final ReleaseDetails RELEASE_DETAILS = ReleaseDetails.getInstance();
	private static final String FLAVOURS_HEADING = APP_NAME + " supported PDF/A profiles:";
	private static final ProfileDirectory PROFILES = Profiles.getVeraProfileDirectory();

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
		ReleaseDetails.addDetailsFromResource(
				ReleaseDetails.APPLICATION_PROPERTIES_ROOT + "app." + ReleaseDetails.PROPERTIES_EXT);
		CliArgParser cliArgParser = new CliArgParser();
		JCommander jCommander = new JCommander(cliArgParser);
		jCommander.setProgramName(APP_NAME);

		try {
			jCommander.parse(args);
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			showVersionInfo();
			jCommander.usage();
			System.exit(1);
		}
		if (cliArgParser.isHelp()) {
			showVersionInfo();
			jCommander.usage();
			System.exit(0);
		}
		messagesFromParser(cliArgParser);
		if (isProcess(cliArgParser)) {
			try {
				CliProcessor processor = CliProcessor.createProcessorFromArgs(cliArgParser);
				processor.processPaths(cliArgParser.getPdfPaths());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static void messagesFromParser(final CliArgParser parser) {

		if (parser.listProfiles()) {
			listProfiles();
		}

		if (parser.showVersion()) {
			showVersionInfo();
		}
	}

	private static void listProfiles() {
		System.out.println(FLAVOURS_HEADING);
		for (ValidationProfile profile : PROFILES.getValidationProfiles()) {
			System.out.println("  " + profile.getPDFAFlavour().getId() + " - " + profile.getDetails().getName());
		}
		System.out.println();
	}

	private static void showVersionInfo() {
		ReleaseDetails details = RELEASE_DETAILS.byId("gui");
		System.out.println("Version: " + details.getVersion());
		System.out.println("Built: " + details.getBuildDate());
		System.out.println();
	}

	private static boolean isProcess(final CliArgParser parser) {
		if (parser.getPdfPaths().isEmpty() && (parser.isHelp() || parser.listProfiles() || parser.showVersion())) {
			return false;
		}
		return true;
	}
}
