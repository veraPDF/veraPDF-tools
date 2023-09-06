import org.verapdf.core.EncryptedPdfException;
import org.verapdf.core.ModelParsingException;
import org.verapdf.core.ValidationException;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.results.ValidationResult;

import java.io.FileInputStream;
import java.io.IOException;

//	example from https://docs.verapdf.org/develop/ (veraPDF.github.io\develop\index.md)
public class IOTest2 {


	public static void main(String[] args) {
		VeraGreenfieldFoundryProvider.initialise();
		try (PDFAParser parser = Foundries.defaultInstance().createParser(new FileInputStream("mydoc.pdf"))) {
			PDFAValidator validator = Foundries.defaultInstance().createValidator(parser.getFlavour(), false);
			ValidationResult result = validator.validate(parser);
			if (result.isCompliant()) {
				// File is a valid PDF/A 1b
			} else {
				// it isn't
			}
		} catch (IOException | ValidationException | ModelParsingException | EncryptedPdfException exception) {
			// Exception during validation
		}
	}

}
