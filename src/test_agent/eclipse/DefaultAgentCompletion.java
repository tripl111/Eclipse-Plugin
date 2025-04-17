package test_agent.eclipse;

import com.typesafe.config.Config;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import test_agent.results.AnalysisResult;
import test_agent.results.CommandAdaptationResult;
import test_agent.results.ModelResponse;
import test_agent.results.TestGenerationResult;
import test_agent.utils.YamlParser;

public class DefaultAgentCompletion implements AgentCompletion {
    private final Logger logger = Logger.getLogger(DefaultAgentCompletion.class.getName());
    private final AICaller caller;
    private final PromptBuilder promptBuilder;


    public DefaultAgentCompletion(AICaller caller, Config config) {
        this.caller = caller;
        this.promptBuilder = new PromptBuilder(config);
    }


    @Override
    public TestGenerationResult generateTests(
            String sourceFileName, int maxTests, String sourceFileNumbered,
            String codeCoverageReport, String language, String testFile,
            String testFileName, String testingFramework, String additionalInstructionsText,
            String additionalIncludesSection, String failedTestsSection
    ) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("source_file_name", sourceFileName);
        variables.put("max_tests", maxTests);
        variables.put("source_file_numbered", sourceFileNumbered);
        variables.put("code_coverage_report", codeCoverageReport);
        variables.put("language", language);
        variables.put("test_file", testFile);
        variables.put("test_file_name", testFileName);
        variables.put("testing_framework", testingFramework);
        variables.put("additional_instructions_text", additionalInstructionsText);
        variables.put("additional_includes_section", additionalIncludesSection);
        variables.put("failed_tests_section", failedTestsSection);

        Map<String, String> prompt = promptBuilder.buildPrompt("test_generation_prompt", variables);
        //logger.info("Sending prompt for test generation using configured AICaller model."+prompt);

        try {

            ModelResponse response = caller.callModel(prompt, true);

            if (response != null && response.getResponse() != null) {
                return new TestGenerationResult(
                        response.getResponse(),
                        response.getPromptTokens(),
                        response.getCompletionTokens(),
                        prompt.get("user")
                );
            } else {
                logger.severe("Received null response from configured AICaller model.");
                return new TestGenerationResult(
                        "Error: No response from model", 0, 0, prompt.get("user")
                );
            }
        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "Error calling configured AICaller model: " + e.getMessage(), e);
            return new TestGenerationResult(
                    "Error calling model: " + e.getMessage(), 0, 0, prompt.get("user")
            );
        }
    }

    @Override
    public AnalysisResult analyzeTestFailure(
            String sourceFileName, String sourceFile, String processedTestFile,
            String stdout, String stderr, String testFileName
    ) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("source_file_name", sourceFileName);
        variables.put("source_file", sourceFile);
        variables.put("processed_test_file", processedTestFile);
        variables.put("stdout", stdout);
        variables.put("stderr", stderr);
        variables.put("test_file_name", testFileName);

        Map<String, String> prompt = promptBuilder.buildPrompt("analyze_test_run_failure", variables);
        //logger.info("Sending prompt for test failure analysis using configured AICaller model.");

        try {
            ModelResponse response = caller.callModel(prompt, false);

            if (response != null && response.getResponse() != null) {
                return new AnalysisResult(
                        response.getResponse(),
                        response.getPromptTokens(),
                        response.getCompletionTokens(),
                        prompt.get("user")
                );
            } else {
                logger.severe("Received null response from configured AICaller model.");
                return new AnalysisResult(
                        "Error: No response from model", 0, 0, prompt.get("user")
                );
            }
        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "Error calling configured AICaller model: " + e.getMessage(), e);
            return new AnalysisResult(
                    "Error calling model: " + e.getMessage(), 0, 0, prompt.get("user")
            );
        }
    }

    @Override
    public AnalysisResult analyzeTestInsertLine(
            String language, String testFileNumbered, String testFileName

    ) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("language", language);
        variables.put("test_file_numbered", testFileNumbered);
        variables.put("test_file_name", testFileName);

        Map<String, String> prompt = promptBuilder.buildPrompt("analyze_suite_test_insert_line", variables);
        //logger.info("Sending prompt for test insert line analysis using configured AICaller model."+prompt);

        try {
            ModelResponse response = caller.callModel(prompt, false);

            if (response != null && response.getResponse() != null) {
                return new AnalysisResult(
                        response.getResponse(),
                        response.getPromptTokens(),
                        response.getCompletionTokens(),
                        prompt.get("user")
                );
            } else {
                logger.severe("Received null response from configured AICaller model.");
                return new AnalysisResult(
                        "Error: No response from model", 0, 0, prompt.get("user")
                );
            }
        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "Error calling configured AICaller model: " + e.getMessage(), e);
            return new AnalysisResult(
                    "Error calling model: " + e.getMessage(), 0, 0, prompt.get("user")
            );
        }
    }

    @Override
    public AnalysisResult analyzeTestAgainstContext(
            String language, String testFileContent, String testFileNameRel,
            String contextFilesNamesRel
    ) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("language", language);
        variables.put("test_file_content", testFileContent);
        variables.put("test_file_name_rel", testFileNameRel);
        variables.put("context_files_names_rel", contextFilesNamesRel);

        Map<String, String> prompt = promptBuilder.buildPrompt("analyze_test_against_context", variables);
        logger.info("Sending prompt for test analysis against context using configured AICaller model.");

        try {
            ModelResponse response = caller.callModel(prompt, false);

            if (response != null && response.getResponse() != null) {
                return new AnalysisResult(
                        response.getResponse(),
                        response.getPromptTokens(),
                        response.getCompletionTokens(),
                        prompt.get("user")
                );
            } else {
                logger.severe("Received null response from configured AICaller model.");
                return new AnalysisResult(
                        "Error: No response from model", 0, 0, prompt.get("user")
                );
            }
        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "Error calling configured AICaller model: " + e.getMessage(), e);
            return new AnalysisResult(
                    "Error calling model: " + e.getMessage(), 0, 0, prompt.get("user")
            );
        }
    }


@Override
    public CommandAdaptationResult adaptTestCommandForSingleTest(
            String testFileRelativePath, String testCommand, String projectRootDir
    ) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("test_file_relative_path", testFileRelativePath);
        variables.put("test_command", testCommand);
        variables.put("project_root_dir", projectRootDir);

        Map<String, String> prompt = promptBuilder.buildPrompt("adapt_test_command_for_a_single_test_via_ai", variables);
       // logger.info("Sending prompt for command adaptation using configured AICaller model.");

        try {
            ModelResponse response = caller.callModel(prompt, false);


            if (response == null || response.getResponse() == null) {
                logger.severe("Received null response from configured AICaller model.");
                return new CommandAdaptationResult(
                        "Error: No response from model", 0, 0, prompt.get("user")
                );
            }

            String newCommandLine = null;
            try {
                Map<String, Object> responseYaml = YamlParser.loadYaml(response.getResponse());
                if (responseYaml != null && responseYaml.containsKey("new_command_line")) {
                    newCommandLine = ((String) responseYaml.get("new_command_line")).trim();
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed parsing YAML for adapt_test_command. response: " + response.getResponse() + ". Error: " + e.getMessage(), e);
            }

            return new CommandAdaptationResult(
                    newCommandLine != null ? newCommandLine : "Error: Could not parse command",
                    response.getPromptTokens(),
                    response.getCompletionTokens(),
                    prompt.get("user")
            );
        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "Error calling configured AICaller model: " + e.getMessage(), e);
            return new CommandAdaptationResult(
                    "Error calling model: " + e.getMessage(), 0, 0, prompt.get("user")
            );
        }
    }
}