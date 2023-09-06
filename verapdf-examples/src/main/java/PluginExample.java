import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
// and from VeraPDF:
import org.verapdf.core.FeatureParsingException;
import org.verapdf.features.AbstractEmbeddedFileFeaturesExtractor;
import org.verapdf.features.EmbeddedFileFeaturesData;
import org.verapdf.features.tools.FeatureTreeNode;

//	example from https://docs.verapdf.org/plugins/ (veraPDF.github.io\plugins\index.md)
public class PluginExample extends AbstractEmbeddedFileFeaturesExtractor {

	private static final Logger LOGGER = Logger.getLogger(PluginExample.class.getCanonicalName());

	@Override
	public List<FeatureTreeNode> getEmbeddedFileFeatures(EmbeddedFileFeaturesData embeddedFileFeaturesData) {
		List<FeatureTreeNode> res = new ArrayList<>();
		try {
			FeatureTreeNode node = FeatureTreeNode.createRootNode("Hello");
			node.setValue("World");
			res.add(node);
		} catch (FeatureParsingException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return res;
	}

}
