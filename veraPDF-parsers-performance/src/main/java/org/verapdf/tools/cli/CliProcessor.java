package org.verapdf.tools.cli;

import org.verapdf.core.EncryptedPdfException;
import org.verapdf.core.ModelParsingException;
import org.verapdf.core.ValidationException;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.validation.profiles.Profiles;
import org.verapdf.pdfa.validation.profiles.ValidationProfile;
import org.verapdf.tools.performance.ModelParserType;
import org.verapdf.tools.performance.ParsersPerformanceChecker;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.util.Formatter;
import java.util.List;

/**
 * @author Maksim Bezrukov
 */
final class CliProcessor {

	private static final long MS_IN_SEC = 1000L;
	private static final int SEC_IN_MIN = 60;
	private static final long MS_IN_MIN = SEC_IN_MIN * MS_IN_SEC;
	private static final int MIN_IN_HOUR = 60;
	private static final long MS_IN_HOUR = MS_IN_MIN * MIN_IN_HOUR;

	private final boolean recurse;
	private String baseDirectory = "";
	private final CliArgParser args;

	private CliProcessor() throws IOException {
		this(new CliArgParser());
	}

	private CliProcessor(final CliArgParser args) throws IOException {
		this.recurse = args.isRecurse();
		this.args = args;
	}

	void processPaths(final List<String> pdfPaths) {
		for (String pdfPath : pdfPaths) {
			File file = new File(pdfPath);
			if (file.isDirectory()) {
				baseDirectory = file.getAbsolutePath();
				processDir(file);
			} else {
				processFile(file);
			}
		}
	}

	static CliProcessor createProcessorFromArgs(final CliArgParser args)
			throws IOException {
		return new CliProcessor(args);
	}

	private void processDir(final File dir) {
		for (File file : dir.listFiles()) {
			if (file.isFile()) {
				int extIndex = file.getName().lastIndexOf(".");
				String ext = file.getName().substring(extIndex + 1);
				if ("pdf".equalsIgnoreCase(ext)) {
					processFile(file);
				}
			} else if (file.isDirectory()) {
				if (this.recurse) {
					processDir(file);
				}
			}
		}
	}

	private void processFile(final File pdfFile) {
		if (checkFileCanBeProcessed(pdfFile)) {
			try (InputStream toProcess = new FileInputStream(pdfFile)) {
				processStream(toProcess, pdfFile.getAbsolutePath());
			} catch (IOException | ModelParsingException | EncryptedPdfException e) {
				System.err.println("Exception raised while processing " + pdfFile.getAbsolutePath());
				e.printStackTrace();
			}
		}
	}

	private static boolean checkFileCanBeProcessed(final File file) {
		if (!file.isFile()) {
			System.err.println("Path " + file.getAbsolutePath() + " is not an existing file.");
			return false;
		} else if (!file.canRead()) {
			System.err.println("Path " + file.getAbsolutePath() + " is not a readable file.");
			return false;
		}
		return true;
	}

	private void processStream(final InputStream toProcess, String filePath) throws IOException, ModelParsingException, EncryptedPdfException {
		File profileFile = args.getProfileFile();
		ValidationProfile profile = null;
		try {
			profile = profileFile == null ? null : Profiles.profileFromXml(new FileInputStream(profileFile));
		} catch (JAXBException | FileNotFoundException e) {
			System.err.println("Can not load validation profile. Process starts with using "
					+ (args.getFlavour() == PDFAFlavour.NO_FLAVOUR ? "default" : args.getFlavour().getId())
					+ " flavour");
			e.printStackTrace();
		}
		ParsersPerformanceChecker checker = profile == null ?
				ParsersPerformanceChecker.createCheckerWithFlavour(toProcess, args.getFlavour(), !args.hidePassed(), args.maxFailures())
				: ParsersPerformanceChecker.createCheckerWithProfile(toProcess, profile, !args.hidePassed(), args.maxFailures());

		System.out.println();
		System.out.println("File: " + filePath);

		if (!args.isValidationOff()) {
			try {
				showResults("Validation",
						checker.doesValidationResultsEquals(),
						checker.getTimeOfValidation(ModelParserType.PDFBOX),
						checker.getTimeOfValidation(ModelParserType.GREENFIELD));
			} catch (ValidationException e) {
				System.err.println("Exception during one of validations");
				e.printStackTrace();
			}
		}

//		if (args.fixMetadata()) {
//			showResults("Metadata Fixer",
//					checker.doesMetadataFixerResultsEquals(),
//					checker.getTimeOfMetadataFixing(ModelParserType.PDFBOX),
//					checker.getTimeOfMetadataFixing(ModelParserType.GREENFIELD));
//		}

		if (args.extractFeatures()) {
			showResults("Features Extraction",
					checker.doesFeaturesCollectionsEquals(),
					checker.getTimeOfFeaturesCollecting(ModelParserType.PDFBOX),
					checker.getTimeOfFeaturesCollecting(ModelParserType.GREENFIELD));
		}
	}

	private void showResults(String processType, boolean isEquals, long pdfboxTime, long greenfieldTime) {
		System.out.println(processType + " results:");
		System.out.println("	Equals: " + isEquals);
		System.out.println("	" + convertMillisToHumanReadableTime(pdfboxTime) + " PDFBox based time");
		System.out.println("	" + convertMillisToHumanReadableTime(greenfieldTime) + " Greenfield based time");

		double pr = (greenfieldTime*1./pdfboxTime)*100 - 100;
		if (pr > 0) {
			System.out.println("	SLOWER " + (int)pr + "%");
		} else if (pr < 0) {
			System.out.println("	FASTER " + Math.abs((int)pr) + "%");
		} else {
			System.out.println("	EQUAL");
		}
	}

	private String convertMillisToHumanReadableTime(long millis) {
		long processingTime = millis;

		Long hours = Long.valueOf(processingTime / MS_IN_HOUR);
		processingTime %= MS_IN_HOUR;

		Long mins = Long.valueOf(processingTime / MS_IN_MIN);
		processingTime %= MS_IN_MIN;

		Long sec = Long.valueOf(processingTime / MS_IN_SEC);
		processingTime %= MS_IN_SEC;

		Long ms = Long.valueOf(processingTime);

		String res;

		try (Formatter formatter = new Formatter()) {
			formatter.format("%02d:", hours);
			formatter.format("%02d:", mins);
			formatter.format("%02d.", sec);
			formatter.format("%03d", ms);
			res = formatter.toString();
		}

		return res;
	}
}
