package org.verapdf.tools.cli;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.verapdf.pdfa.flavours.PDFAFlavour;

import java.io.File;
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
	final static String FLAVOUR_FLAG = FLAG_SEP + "f";
	final static String FLAVOUR = OPTION_SEP + "flavour";
	final static String SUCCESS = OPTION_SEP + "success";
	final static String PASSED = OPTION_SEP + "hidePassed";
	final static String LIST_FLAG = FLAG_SEP + "l";
	final static String LIST = OPTION_SEP + "list";
	final static String LOAD_PROFILE_FLAG = FLAG_SEP + "p";
	final static String LOAD_PROFILE = OPTION_SEP + "profile";
	final static String EXTRACT_FLAG = FLAG_SEP + "x";
	final static String EXTRACT = OPTION_SEP + "extract";
	final static String RECURSE_FLAG = FLAG_SEP + "r";
	final static String RECURSE = OPTION_SEP + "recurse";
	final static String MAX_FAILURES = OPTION_SEP + "maxfailures";
	final static String FIX_METADATA = OPTION_SEP + "fixmetadata";

	@Parameter(names = { HELP_FLAG, HELP }, description = "Shows this message and exits.", help = true)
	private boolean help = false;

	@Parameter(names = { VERSION }, description = "Version information.")
	private boolean showVersion = false;

	@Parameter(names = { FLAVOUR_FLAG, FLAVOUR }, description = "Choose built in Validation Profile flavour, e.g. 1b. Alternatively supply 0 to turn off PDF/A validation or supply auto to automatic flavour detection from file's metadata.", converter = FlavourConverter.class)
	private PDFAFlavour flavour = PDFAFlavour.AUTO;

	@Parameter(names = { SUCCESS, PASSED }, description = "Hide successful validation checks.")
	private boolean hidePassed = false;

	@Parameter(names = { LIST_FLAG, LIST }, description = "List built in Validation Profiles.")
	private boolean listProfiles = false;

	@Parameter(names = { LOAD_PROFILE_FLAG, LOAD_PROFILE }, description = "Load a Validation Profile from given path and exit if loading fails. This overrides any choice or default implied by the -f / --flavour option.", validateWith = ProfileFileValidator.class)
	private File profileFile;

	@Parameter(names = { EXTRACT_FLAG, EXTRACT }, description = "Extract and report PDF features.")
	private boolean features = false;

	@Parameter(names = { RECURSE_FLAG, RECURSE }, description = "Recurse directories, only files with a .pdf extension are processed.")
	private boolean isRecurse = false;

	@Parameter(names = { MAX_FAILURES }, description = "Sets maximum amount of failed checks.")
	private int maxFailures = -1;

	@Parameter(names = { FIX_METADATA }, description = "Performs metadata fix.")
	private boolean fixMetadata = false;

	@Parameter(description = "FILES")
	private List<String> pdfPaths = new ArrayList<>();

	/**
	 * @return true if version information requested
	 */
	public boolean showVersion() {
		return this.showVersion;
	}

	/**
	 * @return true if a list of supported profiles requested
	 */
	public boolean listProfiles() {
		return this.listProfiles;
	}

	/**
	 * @return maximum amount of failed checks
	 */
	public int maxFailures() {
		return this.maxFailures;
	}

	/**
	 * @return true if metadata fix is enabled
	 */
	public boolean fixMetadata() {
		return this.fixMetadata;
	}

	/**
	 * @return true if to recursively process sub-dirs
	 */
	public boolean isRecurse() {
		return this.isRecurse;
	}

	/**
	 * @return true if help requested
	 */
	public boolean isHelp() {
		return this.help;
	}

	/**
	 * @return true if hide passed checks requested
	 */
	public boolean hidePassed() {
		return this.hidePassed;
	}

	/**
	 * @return true if PDF Feature extraction requested
	 */
	public boolean extractFeatures() {
		return this.features;
	}

	/**
	 * @return the validation flavour string id
	 */
	public PDFAFlavour getFlavour() {
		return this.flavour;
	}

	/**
	 * @return the {@link File} object for the validation profile
	 */
	public File getProfileFile() {
		return this.profileFile;
	}

	/**
	 * @return the list of file paths
	 */
	public List<String> getPdfPaths() {
		return this.pdfPaths;
	}

	/**
	 * JCommander parameter converter for {@link PDFAFlavour}, see
	 * {@link IStringConverter} and {@link PDFAFlavour#byFlavourId(String)}.
	 *
	 * @author <a href="mailto:carl@openpreservation.org">Carl Wilson</a>
	 *
	 */
	public static final class FlavourConverter implements
			IStringConverter<PDFAFlavour> {
		/**
		 * { @inheritDoc }
		 */
		@Override
		public PDFAFlavour convert(final String value) {
			for (PDFAFlavour flavourLocal : PDFAFlavour.values()) {
				if (flavourLocal.getId().equalsIgnoreCase(value))
					return flavourLocal;
			}
			throw new ParameterException("Illegal --flavour argument:" + value);
		}

	}

	/**
	 * JCommander parameter validator for {@link File}, see
	 * {@link IParameterValidator}. Enforces an existing, readable file.
	 *
	 * @author <a href="mailto:carl@openpreservation.org">Carl Wilson</a>
	 *
	 */
	public static final class ProfileFileValidator implements
			IParameterValidator {
		/**
		 * { @inheritDoc }
		 */
		@Override
		public void validate(final String name, final String value)
				throws ParameterException {
			File profileFileLocal = new File(value);
			if (!profileFileLocal.isFile() || !profileFileLocal.canRead()) {
				throw new ParameterException("Parameter " + name
						+ " must be the path to an existing, readable file, value=" + value);
			}
		}

	}
}
