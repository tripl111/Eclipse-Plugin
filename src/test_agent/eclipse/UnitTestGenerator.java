package test_agent.eclipse;


import test_agent.models.GeneratedTest;
import test_agent.results.TestGenerationResult;
import test_agent.utils.YamlParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import test_agent.utils.FileUtils;

/**
 * Generates unit tests for source code using AI-powered test generation.
 */
public class UnitTestGenerator {
    private static final int MAX_TESTS_PER_RUN = 4;
    private static final Logger logger = Logger.getLogger(UnitTestGenerator.class.getName());

    private final String projectRoot;
    private final String sourceFilePath;
    private final String testFilePath;
    private final List<String> includedFiles;
    private final String additionalInstructions;
    private final AgentCompletion agentCompletion;
    private int totalInputTokenCount;
    private int totalOutputTokenCount;
    private String sourceCode;
    private String testCode;


    /**
     * Initializes the UnitTestGenerator with the provided parameters.
     *
     * @param sourceFilePath The path to the source file being tested
     * @param testFilePath The path to the test file where generated tests will be written
     * @param agentCompletion The agent completion object to be used for test generation
     * @param includedFiles Additional files to include
     * @param additionalInstructions Additional instructions for test generation
     * @param projectRoot The root directory of the project
     */
    public UnitTestGenerator(
            String sourceFilePath,
            String testFilePath,
            AgentCompletion agentCompletion,
            List<String> includedFiles,
            String additionalInstructions,
            String projectRoot
    ) {
        this.projectRoot = projectRoot;
        this.sourceFilePath = sourceFilePath;
        this.testFilePath = testFilePath;
        this.includedFiles = includedFiles;
        this.additionalInstructions = additionalInstructions;
        this.agentCompletion = agentCompletion;

        // Initialize state variables
        this.totalInputTokenCount = 0;
        this.totalOutputTokenCount = 0;


        try {
            // Read source and test files
            this.sourceCode = Files.readString(Paths.get(this.sourceFilePath));
            this.testCode = Files.readString(Paths.get(this.testFilePath));
        } catch (IOException e) {
            logger.severe("Error reading source or test files: " + e.getMessage());
            throw new RuntimeException("Failed to read source or test files", e);
        }
    }



    /**
     * Processes the failed test runs and returns a formatted string with details.
     *
     * @param failedTestRuns A list of maps containing information about failed test runs
     * @return A formatted string with details of the failed tests
     */
    public String checkForFailedTestRuns(List<Map<String, Object>> failedTestRuns) {
        if (failedTestRuns == null || failedTestRuns.isEmpty()) {
            return "";
        }

        StringBuilder failedTestRunsValue = new StringBuilder();

        try {
            for (Map<String, Object> failedTest : failedTestRuns) {
                @SuppressWarnings("unchecked")
                Map<String, Object> failedTestDict = (Map<String, Object>) failedTest.get("code");

                if (failedTestDict == null || failedTestDict.isEmpty()) {
                    continue;
                }

                // Convert map to JSON string
                String code = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(failedTestDict);
                String errorMessage = (String) failedTest.get("error_message");

                failedTestRunsValue.append("Failed Test:\n```\n").append(code).append("\n```\n");

                if (errorMessage != null && !errorMessage.isEmpty()) {
                    failedTestRunsValue.append("Test execution error analysis:\n")
                            .append(errorMessage).append("\n\n\n");
                } else {
                    failedTestRunsValue.append("\n\n");
                }
            }
        } catch (Exception e) {
            logger.severe("Error processing failed test runs: " + e.getMessage());
            return "";
        }

        return failedTestRunsValue.toString();
    }

    /**
     * Generates tests using the AI model based on the constructed prompt.
     *
     * @param failedTestRuns A list of maps containing information about failed test runs
     * @param language The programming language of the source code
     * @param testingFramework The testing framework to use
     * @param codeCoverageReport The code coverage report
     * @return A map containing the generated tests
     */
    public Map<String, Object> generateTests(
            List<Map<String, Object>> failedTestRuns,
            String language,
            String testingFramework,
            String codeCoverageReport
    ) {
        String failedTestRunsValue = checkForFailedTestRuns(failedTestRuns);

        // Get relative path of source file from project root
        Path sourceFileRelPath = Paths.get(projectRoot).relativize(Paths.get(sourceFilePath));
        Path testFileRelPath = Paths.get(projectRoot).relativize(Paths.get(testFilePath));

        // Number the source code lines
        String sourceFileNumbered = IntStream.range(0, sourceCode.split("\n").length)
                .mapToObj(i -> (i + 1) + " " + sourceCode.split("\n")[i])
                .collect(Collectors.joining("\n"));
        // Gathers the contents of includedFiles into a single string
        String includedContent = FileUtils.getIncludedFilesContent(includedFiles);



        TestGenerationResult result = agentCompletion.generateTests(
                sourceFileRelPath.toString(),
                MAX_TESTS_PER_RUN,
                sourceFileNumbered,
                codeCoverageReport,
                language,
                testCode,
                testFileRelPath.toString(),
                testingFramework,
                additionalInstructions,
                includedContent,
                failedTestRunsValue
        );
        //("Raw AI YAML response:\n" + result.getResponse());
        // Update token counts
        this.totalInputTokenCount += result.getInputTokenCount();
        this.totalOutputTokenCount += result.getOutputTokenCount();


        try {
            // Parse the YAML response
            Map<String, Object> testsDict = YamlParser.loadYaml(result.getResponse());

            if (testsDict != null) {
                // Convert the raw map entries in new_tests to GeneratedTest objects
                if (testsDict.containsKey("new_tests") && testsDict.get("new_tests") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> rawTests = (List<Object>) testsDict.get("new_tests");
                    List<GeneratedTest> convertedTests = new ArrayList<>();

                    for (Object rawTest : rawTests) {
                        if (rawTest instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> testMap = (Map<String, Object>) rawTest;
                            // Use the fromMap method to convert the map to a GeneratedTest
                            GeneratedTest generatedTest = GeneratedTest.fromMap(testMap);
                            convertedTests.add(generatedTest);
                            // Log each converted test
                            //logger.info("Converted test: " + generatedTest);
                        }
                    }

                    // Replace the original list with the converted list
                    testsDict.put("new_tests", convertedTests);
                   //logger.info("Converted new_tests after transformation: " + convertedTests);

                }

                return testsDict;

            } else {
                logger.warning("Parsed YAML response is null");
                return new HashMap<>();
            }
        } catch (Exception e) {
            logger.severe("Error during test generation: " + e.getMessage());

            // Record the error as a failed test attempt
            Map<String, Object> failDetails = new HashMap<>();
            failDetails.put("status", "FAIL");
            failDetails.put("reason", "Parsing error: " + e.getMessage());
            failDetails.put("exit_code", null);
            failDetails.put("stderr", e.toString());
            failDetails.put("stdout", "");
            failDetails.put("test", result.getResponse());


            return new HashMap<>();
        }
    }

    // Getters for the class properties

    public int getTotalInputTokenCount() {
        return totalInputTokenCount;
    }
    public int getTotalOutputTokenCount() {
        return totalOutputTokenCount;
    }
    }