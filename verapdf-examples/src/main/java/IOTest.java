import org.verapdf.core.VeraPDFException;
import org.verapdf.features.FeatureExtractorConfig;
import org.verapdf.features.FeatureFactory;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.metadata.fixer.FixerFactory;
import org.verapdf.metadata.fixer.MetadataFixerConfig;
import org.verapdf.pdfa.validation.validators.ValidatorConfig;
import org.verapdf.pdfa.validation.validators.ValidatorFactory;
import org.verapdf.processor.*;
import org.verapdf.processor.plugins.PluginsCollectionConfig;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

//	example from https://docs.verapdf.org/develop/processor/ (veraPDF.github.io\develop\processor\index.md)
public class IOTest {

	public static void main(String[] args) {
		// Foundry initialising. Can be changed into PDFBox based one
		VeraGreenfieldFoundryProvider.initialise();
		// Default validator config
		ValidatorConfig validatorConfig = ValidatorFactory.defaultConfig();
		// or it is possible to select the needed parameters using ValidatorConfigBuilder, for example flavour
		// ValidatorConfig validatorConfig = new ValidatorConfigBuilder().flavour(PDFAFlavour.PDFA_4).build();
		FormatOption format = FormatOption.MRR;
		//create builder
		//add VeraAppConfig
		// Default features config
		FeatureExtractorConfig featureConfig = FeatureFactory.defaultConfig();
		// Default plugins config
		PluginsCollectionConfig pluginsConfig = PluginsCollectionConfig.defaultConfig();
		// Default fixer config
		MetadataFixerConfig fixerConfig = FixerFactory.defaultConfig();
		// Tasks configuring
		EnumSet<TaskType> tasks = EnumSet.noneOf(TaskType.class);
		tasks.add(TaskType.VALIDATE);
		tasks.add(TaskType.EXTRACT_FEATURES);
		tasks.add(TaskType.FIX_METADATA);
		// Creating processor config
		ProcessorConfig processorConfig = ProcessorFactory.fromValues(validatorConfig, featureConfig, pluginsConfig, fixerConfig, tasks);
		// Creating processor and output stream. In this example output stream is System.out
		try (BatchProcessor processor = ProcessorFactory.fileBatchProcessor(processorConfig);
			 OutputStream reportStream = System.out) {
			// Generating list of files for processing
			List<File> files = new ArrayList<>();
			files.add(new File("fail.pdf"));
			// starting the processor
			processor.process(files, ProcessorFactory.getHandler(format, true, reportStream,
					processorConfig.getValidatorConfig().isRecordPasses()));
		} catch (VeraPDFException e) {
			System.err.println("Exception raised while processing batch");
			e.printStackTrace();
		} catch (IOException excep) {
			System.err.println("Exception raised closing MRR temp file.");
			excep.printStackTrace();
		}
	}
}
