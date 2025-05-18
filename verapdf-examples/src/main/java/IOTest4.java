import org.verapdf.core.EncryptedPdfException;
import org.verapdf.core.ModelParsingException;
import org.verapdf.core.ValidationException;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.flavours.PDFFlavours;
import org.verapdf.pdfa.results.ValidationResult;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

//	example from https://docs.verapdf.org/develop/processor/ (veraPDF.github.io\develop\processor\index.md)
public class IOTest4 {

	public static void main(String[] args) {
		VeraGreenfieldFoundryProvider.initialise();
		try (PDFAParser parser = Foundries.defaultInstance().createParser(new FileInputStream("test.pdf"))) {
			List<PDFAFlavour> detectedFlavours = parser.getFlavours();
			List<PDFAFlavour> flavours = new LinkedList<>();
			for (PDFAFlavour flavour : detectedFlavours) {
				// iterate through all detected flavours and pick up PDF/A and PDF/UA ones for validation
				if (PDFFlavours.isFlavourFamily(flavour, PDFAFlavour.SpecificationFamily.PDF_A) ||
						PDFFlavours.isFlavourFamily(flavour, PDFAFlavour.SpecificationFamily.PDF_UA)) {
					flavours.add(flavour);
				}
			}
			PDFAValidator validator = Foundries.defaultInstance().createValidator(Collections.emptyList());
			List<ValidationResult> results = validator.validateAll(parser);
			for (ValidationResult result : results) {
				if (result.isCompliant()) {
					// File complies to flavour
				} else {
					// File doesn't comply to flavour
				}
			}
		} catch (IOException | ValidationException | ModelParsingException | EncryptedPdfException exception) {
			// Exception during validation
		}
	}

}
