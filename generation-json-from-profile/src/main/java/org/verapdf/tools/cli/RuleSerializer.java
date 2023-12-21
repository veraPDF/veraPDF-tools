package org.verapdf.tools.cli;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.verapdf.pdfa.validation.profiles.Rule;
import org.verapdf.pdfa.validation.profiles.RuleId;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RuleSerializer extends StdSerializer<Rules> {

	private static final Logger LOGGER = Logger.getLogger(RuleSerializer.class.getCanonicalName());

	public static String ERROR_ARGUMENT_WARNING = "Error message for rule %s contains error argument";

	protected RuleSerializer(Class<Rules> t) {
		super(t);
	}

	public void serialize(Rules rules, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
			throws IOException {
		RuleId lastRuleId = null;
		jsonGenerator.writeStartObject();
		for (Rule rule : rules.getRules()) {
			RuleId id = rule.getRuleId();
			if (lastRuleId == null || !Objects.equals(lastRuleId.getSpecification(), id.getSpecification())) {
				if (lastRuleId != null) {
					jsonGenerator.writeEndObject();
					jsonGenerator.writeEndObject();
				}
				jsonGenerator.writeFieldName(id.getSpecification().getId());
				jsonGenerator.writeStartObject();
			}
			if (lastRuleId == null || !lastRuleId.getClause().equals(id.getClause())) {
				if (lastRuleId != null) {
					jsonGenerator.writeEndObject();
				}
				jsonGenerator.writeFieldName(id.getClause());
				jsonGenerator.writeStartObject();
			}
			jsonGenerator.writeFieldName(String.valueOf(id.getTestNumber()));
			jsonGenerator.writeStartObject();
			jsonGenerator.writeStringField("SUMMARY", rule.getError().getMessage());
			if (!rule.getError().getArguments().isEmpty()) {
				LOGGER.log(Level.WARNING, String.format(ERROR_ARGUMENT_WARNING, rule.getRuleId().getClause() + '-' +
						rule.getRuleId().getTestNumber()));
			}
			jsonGenerator.writeStringField("DESCRIPTION", rule.getDescription());
			jsonGenerator.writeEndObject();
			lastRuleId = rule.getRuleId();
		}
		jsonGenerator.writeEndObject();
		jsonGenerator.writeEndObject();
	}
}
