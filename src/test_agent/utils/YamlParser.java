package test_agent.utils;

import org.yaml.snakeyaml.Yaml;
import java.util.Map;
import java.util.logging.Logger;

public class YamlParser {
    private static final Logger logger = Logger.getLogger(YamlParser.class.getName());

    public static Map<String, Object> loadYaml(String responseText) {
        try {
            String cleanedText = responseText.trim()
                    .replaceFirst("```yaml", "")
                    .replaceFirst("```", "");

            Yaml yaml = new Yaml();
            return yaml.load(cleanedText);
        } catch (Exception e) {
            logger.warning("Failed to parse YAML response: " + e.getMessage());
            throw new RuntimeException("Failed to parse YAML response", e);
        }
    }
}