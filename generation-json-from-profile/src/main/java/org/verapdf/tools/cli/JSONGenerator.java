package org.verapdf.tools.cli;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.verapdf.pdfa.validation.profiles.Profiles;
import org.verapdf.pdfa.validation.profiles.ValidationProfile;

import javax.xml.bind.JAXBException;
import java.io.*;

public class JSONGenerator {

	public static void main(String[] args) throws IOException {
		ValidationProfile profile;
		File file = new File(args[0]);
		try (InputStream is = new FileInputStream(file)) {
			profile = Profiles.profileFromXml(is);
		} catch (IOException | JAXBException e) {
			e.printStackTrace();
			return;
		}
		Rules rules = new Rules(profile.getRules());
		ObjectMapper objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule("Serializer", new Version(2, 1,
				3, null, null, null));
		RuleSerializer ruleSerializer = new RuleSerializer(Rules.class);
		module.addSerializer(Rules.class, ruleSerializer);
		objectMapper.registerModule(module);
		objectMapper.writeValue(System.out, rules);
	}
}
