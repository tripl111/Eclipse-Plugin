package test_agent.eclipse;

import test_agent.models.GeneratedTest;
import test_agent.results.CommandAdaptationResult;
import test_agent.settings.ConfigManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Main agent class that orchestrates the test generation and validation process.
 * This class coordinates the generation of unit tests, validation of those tests,
 * and tracking of code coverage improvements.
 */
public class CoverAgent {
    private final Logger logger = Logger.getLogger(CoverAgent.class.getName());
    private final CoverAgentArgs args;
    private final UnitTestGenerator testGen;
    private final UnitTestValidator testValidator;
    private final AgentCompletion agentCompletion;

    /**
     * Initialize the CoverAgent class with the provided arguments and run the test generation process.
     *
     * @param args The command-line arguments containing necessary information for test generation.
     * @param agentCompletion The agent completion object to be used for test generation. Can be null to use default.
     * @throws NullPointerException if args is null.
     * @throws IllegalArgumentException if the model is not specified in args when agentCompletion is null.
     */
    public CoverAgent(CoverAgentArgs args, AgentCompletion agentCompletion) {
        this.args = Objects.requireNonNull(args, "CoverAgentArgs cannot be null");
        validatePaths();
        duplicateTestFile();

        if (agentCompletion != null) {
            this.agentCompletion = agentCompletion;
           // logger.info("Using provided AgentCompletion implementation.");
        } else {
           // logger.info("No AgentCompletion provided, creating DefaultAgentCompletion.");
            String modelToUse = args.getModel();

            if (modelToUse == null || modelToUse.isBlank()) {
                throw new IllegalArgumentException(
                        "Model must be specified in CoverAgentArgs when using the default AgentCompletion setup."
                );
            }


            AICaller aiCaller = new AICaller(
                    args.getApiKey(),
                    args.getSiteUrl(),
                    args.getSiteName(),
                    modelToUse
            );
            this.agentCompletion = new DefaultAgentCompletion(
                    aiCaller,
                    ConfigManager.getInstance().getConfig()
            );
        }

        String testCommand = args.getTestCommand();
        String newCommandLine = null;

        if (args.isRunEachTestSeparately()) {
            String testFileRelativePath = Paths.get(args.getProjectRoot())
                    .relativize(Paths.get(args.getTestFileOutputPath()))
                    .toString();


            // logger.info("Attempting AI-based command adaptation : " + testCommand);
            CommandAdaptationResult adaptationResult = this.agentCompletion.adaptTestCommandForSingleTest(
                    testFileRelativePath,
                    testCommand,
                    args.getTestCommandDir()
            );

            if (adaptationResult != null && adaptationResult.getResponse() != null &&
                    !adaptationResult.getResponse().startsWith("Error:") && !adaptationResult.getResponse().isBlank()) {
                newCommandLine = adaptationResult.getResponse();
                //  logger.info("AI successfully adapted command.");
            } else {
                logger.warning("AI failed to adapt test command or returned an error/empty string. Response: " + (adaptationResult != null ? adaptationResult.getResponse() : "null"));
            }


            if (newCommandLine != null) {
                args.setTestCommandOriginal(testCommand);
                args.setTestCommand(newCommandLine);
            }
        }

        this.testGen = new UnitTestGenerator(
                args.getSourceFilePath(),
                args.getTestFilePath(),
                this.agentCompletion,
                args.getIncludedFiles(),
                args.getAdditionalInstructions(),
                args.getProjectRoot()
        );

        this.testValidator = new UnitTestValidator(
                args.getTestFileOutputPath(),
                args.getSourceFilePath(),
                args.getCodeCoverageReportPath(),
                args.getTestCommand(),
                args.getTestCommandDir(),
                args.getDesiredCoverage(),
                args.getProjectRoot(),
                this.agentCompletion,
                args.getRunTestsMultipleTimes()
        );

    }


    /**
     * Validate the paths provided in the arguments.
     *
     * @throws RuntimeException If the source file or test file is not found at the specified paths.
     */
    private void validatePaths() {

        if (!Files.isRegularFile(Paths.get(args.getSourceFilePath()))) {
            throw new RuntimeException("Source file not found at " + args.getSourceFilePath());
        }


        if (!Files.isRegularFile(Paths.get(args.getTestFilePath()))) {
            throw new RuntimeException("Test file not found at " + args.getTestFilePath());
        }


        if (args.getProjectRoot() != null && !args.getProjectRoot().isBlank() && !Files.isDirectory(Paths.get(args.getProjectRoot()))) {
            throw new RuntimeException("Project root specified but not found at " + args.getProjectRoot());
        } else if (args.getProjectRoot() == null || args.getProjectRoot().isBlank()){

            logger.warning("Project root not specified. Relative paths might be ambiguous.");

        }



    }

    /**
     * Initialize the CoverAgent class with the provided arguments and run the test generation process.
     * If the test file output path is set, copy the test file there.
     */
    private void duplicateTestFile() {
        String sourceTestPath = args.getTestFilePath();
        String targetTestPath = args.getTestFileOutputPath();

        if (targetTestPath != null && !targetTestPath.isBlank() && !Paths.get(sourceTestPath).equals(Paths.get(targetTestPath))) {
            try {

                Path targetDir = Paths.get(targetTestPath).getParent();
                if (targetDir != null && !Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                    System.out.println("Created directory for test output path: " + targetDir);
                }

                Files.copy(
                        Paths.get(sourceTestPath),
                        Paths.get(targetTestPath),
                        StandardCopyOption.REPLACE_EXISTING
                );
                System.out.println("Copied initial test file from " + sourceTestPath + " to " + targetTestPath);
            } catch (IOException e) {
                logger.severe("Error copying test file from " + sourceTestPath + " to " + targetTestPath + ": " + e.getMessage());
                throw new RuntimeException("Failed to copy initial test file", e);
            }
        } else if (targetTestPath == null || targetTestPath.isBlank()){
            args.setTestFileOutputPath(sourceTestPath);
            System.out.println("Test file output path not specified or same as input, will modify test file in place: " + sourceTestPath);
        } else {
        	System.out.println("Test file output path is the same as input path: " + targetTestPath + ". Will modify in place.");
        }
    }


    /**
     * Prepare for test generation process
     * 1. Run the initial test suite analysis.
     * 2. Return the initial test run results.
     *
     * @return A tuple containing failed test runs, language, test framework, and coverage report
     * @throws Exception If there's an error during initialization
     */
    public static class InitResult {
        private final List<Map<String, Object>> failedTestRuns;
        private final String language;
        private final String testFramework;
        private final String coverageReport;

        public InitResult(
                List<Map<String, Object>> failedTestRuns,
                String language,
                String testFramework,
                String coverageReport) {
            this.failedTestRuns = failedTestRuns;
            this.language = language;
            this.testFramework = testFramework;
            this.coverageReport = coverageReport;
        }

        public List<Map<String, Object>> getFailedTestRuns() {
            return failedTestRuns;
        }

        public String getLanguage() {
            return language;
        }

        public String getTestFramework() {
            return testFramework;
        }

        public String getCoverageReport() {
            return coverageReport;
        }
    }

    public InitResult init() throws Exception {
        System.out.println("Starting initial test suite analysis...");
        testValidator.initialTestSuiteAnalysis();
        System.out.println("Running initial coverage analysis...");
        testValidator.runCoverage();
        System.out.println("Initial analysis complete.");


        List<Map<String, Object>> failedTests = testValidator.getFailedTestRuns();
        if (failedTests != null && !failedTests.isEmpty()) {
            logger.warning("Initial test run failed for " + failedTests.size() + " tests.");
        } else {
            logger.info("Initial test run passed.");
        }


        String coverageReport = testValidator.getCodeCoverageReport();
        if (coverageReport != null && !coverageReport.isBlank()) {
            logger.info("Initial coverage report generated/found.");
        } else {
            logger.warning("Could not obtain initial coverage report.");
        }


        return new InitResult(
                failedTests,
                testValidator.getLanguage(),
                testValidator.getTestingFramework(),
                coverageReport
        );
    }


    /**
     * Run the test generation process.
     * This method performs the following steps:
     * 1. Loop until desired coverage is reached or maximum iterations are met.
     * 2. Generate new tests.
     * 3. Loop through each new test and validate it.
     * 4. Increment the iteration count.
     * 5. Check if the desired coverage has been reached.
     * 6. If the desired coverage has been reached, log the final coverage.
     * 7. If the maximum iteration limit is reached, log a failure message if strict coverage is specified.
     * 8. Provide metrics on total token usage.
     * 9. Generate a report.
     *
     * @param failedTestRuns List of failed test runs from init()
     * @param language The programming language from init()
     * @param testFramework The testing framework from init()
     * @param coverageReport The initial coverage report from init()
     */
    public void runTestGen(
            List<Map<String, Object>> failedTestRuns,
            String language,
            String testFramework,
            String coverageReport
    ) {
        int iterationCount = 0;
        boolean targetReached = false;

        System.out.println("Starting test generation loop. Max iterations: " + args.getMaxIterations() +
                ", Desired coverage: " + args.getDesiredCoverage() + "%");


        while (iterationCount < args.getMaxIterations()) {
        	System.out.println("--- Iteration " + (iterationCount + 1) + "/" + args.getMaxIterations() + " ---");
            logCoverage();

            System.out.println("Generating new tests...");
            Map<String, Object> generatedTestsDict = testGen.generateTests(
                    failedTestRuns,
                    language,
                    testFramework,
                    coverageReport
            );

            List<GeneratedTest> newTests = null;
            if (generatedTestsDict != null && generatedTestsDict.containsKey("new_tests")) {
                try {

                    newTests = (List<GeneratedTest>) generatedTestsDict.get("new_tests");
                } catch (ClassCastException e) {
                    logger.severe("Unexpected type for 'new_tests' in generation result: " + e.getMessage());
                    newTests = null;
                }
            }


            if (newTests == null || newTests.isEmpty()) {
                logger.warning("No new tests were generated in this iteration.");

            } else {
            	System.out.println("Generated " + newTests.size() + " new test(s). Validating...");
                for (GeneratedTest generatedTest : newTests) {
                    if (generatedTest == null || generatedTest.getTestCode() == null || generatedTest.getTestCode().isBlank()) {
                        logger.warning("Skipping validation for an empty or null generated test.");
                        continue;
                    }
                    logger.fine("AI-Generated Test:\n" + generatedTest.getTestCode());
                    try {
                        testValidator.validateTest(generatedTest);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Failed to validate generated test: " + e.getMessage(), e);
                    }
                }
                System.out.println("Finished validating generated tests for this iteration.");
            }


            iterationCount++;

            try {
            	System.out.println("Re-running coverage analysis after iteration " + iterationCount + "...");
                testValidator.runCoverage();
                System.out.println("Coverage analysis complete.");


                failedTestRuns = testValidator.getFailedTestRuns();
                language = testValidator.getLanguage();
                testFramework = testValidator.getTestingFramework();
                coverageReport = testValidator.getCodeCoverageReport();


                double currentCoveragePercent = testValidator.getCurrentCoverage() * 100.0;
                double desiredCoveragePercent = (double) testValidator.getDesiredCoverage();


                if (currentCoveragePercent >= desiredCoveragePercent) {
                	System.out.println("Target coverage reached or exceeded.");
                    targetReached = true;
                    break;
                } else {
                	System.out.println(String.format("Coverage %.2f%% is still below target %.2f%%.",
                            currentCoveragePercent, desiredCoveragePercent));
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error running or checking coverage after iteration: " + e.getMessage(), e);
            }
        }


        logFinalCoverage(targetReached, iterationCount);
        logTokenUsage();
    }

    /**
     * Log the final coverage status after the loop finishes.
     * @param targetReached Whether the desired coverage was reached.
     * @param iterationCount The number of iterations completed.
     */
    private void logFinalCoverage(boolean targetReached, int iterationCount) {
        double finalCoveragePercent = testValidator.getCurrentCoverage() * 100.0;
        int desiredCoveragePercent = testValidator.getDesiredCoverage();

        if (targetReached) {
        	System.out.println(String.format(
                    "SUCCESS: Reached target coverage of %d%% (Actual: %.2f%%) in %d iteration(s).",
                    desiredCoveragePercent,
                    finalCoveragePercent,
                    iterationCount
            ));
        } else {
            String failureMessage = String.format(
                    "Reached maximum iteration limit (%d) without achieving desired coverage of %d%%. " +
                            "Final Coverage: %.2f%%.",
                    args.getMaxIterations(),
                    desiredCoveragePercent,
                    finalCoveragePercent
            );
       


          
        }
    }


    /**
     * Log the total token usage.
     */
    private void logTokenUsage() {
        String modelName = args.getModel() != null ? args.getModel() : "[Unknown Model]";
        long totalInput = (testGen != null ? testGen.getTotalInputTokenCount() : 0) +
                (testValidator != null ? testValidator.getTotalInputTokenCount() : 0);
        long totalOutput = (testGen != null ? testGen.getTotalOutputTokenCount() : 0) +
                (testValidator != null ? testValidator.getTotalOutputTokenCount() : 0);


        System.out.println(String.format(
                "Token Usage for LLM model %s: Input=%d, Output=%d, Total=%d",
                modelName,
                totalInput,
                totalOutput,
                totalInput + totalOutput
        ));
    }


    /**
     * Log the current coverage information.
     */
    private void logCoverage() {
        double currentCoveragePercent = testValidator.getCurrentCoverage() * 100.0;

        System.out.println(String.format(
                "Current coverage: %.2f%% (Target: %d%%)",
                currentCoveragePercent,
                testValidator.getDesiredCoverage()
        ));
    }

   

    /**
     * Run the complete test generation process: initialize, then generate/validate.
     *
     * @throws Exception If there's an error during the process
     */
    public void run() throws Exception {
        InitResult initResult = init();
        runTestGen(
                initResult.getFailedTestRuns(),
                initResult.getLanguage(),
                initResult.getTestFramework(),
                initResult.getCoverageReport()
        );
        logger.info("CoverAgent run finished.");
    }

}