package test_agent.eclipse;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import freemarker.template.TemplateExceptionHandler;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Builds prompts for AI models by loading templates from HOCON configuration files.
 * Uses FreeMarker as the template engine.
 */
public class PromptBuilder {
    private final Logger logger = Logger.getLogger(PromptBuilder.class.getName());
    private final Config config;
    private final Configuration templateEngine;

    /**
     * Constructs a PromptBuilder with the provided configuration.
     *
     * @param config The application configuration
     */
    public PromptBuilder(Config config) {
        this.config = config;
        this.templateEngine = createTemplateEngine();
    }

    /**
     * Creates and configures a FreeMarker template engine.
     *
     * @return A configured FreeMarker Configuration
     */
    private Configuration createTemplateEngine() {
        // Create a FreeMarker configuration with a specific version
        Configuration cfg = new Configuration(new Version(2, 3, 31));

        // Set template exception handling
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        // Set default encoding for templates
        cfg.setDefaultEncoding("UTF-8");

        // Set other configuration options as needed
        cfg.setLogTemplateExceptions(true);

        return cfg;
    }

    /**
     * Processes a template string with the provided variables.
     *
     * @param templateString The template string to process
     * @param variables The variables to use in template processing
     * @return The processed template as a string
     * @throws IOException If there's an error reading the template
     * @throws TemplateException If there's an error processing the template
     */
    private String processTemplate(String templateString, Map<String, Object> variables)
            throws IOException, TemplateException {
        // Create a template from the string
        Template template = new Template("inline-template",
                new StringReader(templateString),
                templateEngine);

        // Process the template with the variables
        StringWriter writer = new StringWriter();
        template.process(variables, writer);

        return writer.toString();
    }

    /**
     * Builds a prompt map containing system and user messages by loading templates from configuration.
     *
     * @param file The configuration key/file name to load templates from
     * @param variables A map of variables to use when rendering the templates
     * @return A map containing "system" and "user" keys with rendered template values
     * @throws IllegalArgumentException If the configuration does not contain valid system/user templates
     * @throws RuntimeException If an error occurs while rendering the templates
     */
    public Map<String, String> buildPrompt(String file, Map<String, Object> variables) {
        try {
            // 1. Fetch the prompt config from HOCON configuration
            Config promptConfig = config.getConfig(file);

            // 2. Check if the config has the required system and user keys
            if (!promptConfig.hasPath("system") || !promptConfig.hasPath("user")) {
                String msg = String.format("Could not find valid system/user prompt settings for: %s", file);
                logger.severe(msg);
                throw new IllegalArgumentException(msg);
            }

            // 3. Get the template strings from config
            String systemTemplate = promptConfig.getString("system");
            String userTemplate = promptConfig.getString("user");

            // 4. Render the templates using FreeMarker
            String systemPrompt = processTemplate(systemTemplate, variables);
            String userPrompt = processTemplate(userTemplate, variables);

            // 5. Return the result as a map
            Map<String, String> result = new HashMap<>();
            result.put("system", systemPrompt);
            result.put("user", userPrompt);
            return result;

        } catch (IllegalArgumentException e) {
            // Re-throw the IllegalArgumentException (equivalent to ValueError in Python)
            throw e;
        } catch (ConfigException e) {
            // Handle configuration-related errors
            String errorMsg = String.format("Configuration error for '%s': %s", file, e.getMessage());
            logger.severe(errorMsg);
            throw new RuntimeException(errorMsg, e);
        } catch (IOException e) {
            // Handle template reading errors
            String errorMsg = String.format("Template reading error for '%s': %s", file, e.getMessage());
            logger.severe(errorMsg);
            throw new RuntimeException(errorMsg, e);
        } catch (TemplateException e) {
            // Handle template processing errors
            String errorMsg = String.format("Template processing error for '%s': %s", file, e.getMessage());
            logger.severe(errorMsg);
            throw new RuntimeException(errorMsg, e);
        } catch (Exception e) {
            // Handle any other unexpected errors
            String errorMsg = String.format("Error rendering prompt for '%s': %s", file, e.getMessage());
            logger.severe(errorMsg);
            throw new RuntimeException(errorMsg, e);
        }
    }
}