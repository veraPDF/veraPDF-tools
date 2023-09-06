import org.verapdf.core.EncryptedPdfException;
import org.verapdf.core.ModelParsingException;
import org.verapdf.core.ValidationException;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.ValidationResult;

import java.io.FileInputStream;
import java.io.IOException;

//	example from https://docs.verapdf.org/develop/ (veraPDF.github.io\develop\index.md)
public class IOTest3 {

	public static void main(String[] args) {
		VeraGreenfieldFoundryProvider.initialise();
		PDFAFlavour flavour = PDFAFlavour.fromString("1b");
		try (PDFAParser parser = Foundries.defaultInstance().createParser(new FileInputStream("mydoc.pdf"), flavour)) {
			PDFAValidator validator = Foundries.defaultInstance().createValidator(flavour, false);
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
