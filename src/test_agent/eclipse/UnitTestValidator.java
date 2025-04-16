
package test_agent.eclipse;

import test_agent.models.GeneratedTest;
import test_agent.results.AnalysisResult;
import test_agent.results.TestValidationResult;
import test_agent.utils.YamlParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import test_agent.utils.LanguageUtils;
import static test_agent.utils.FileUtils.getRelativePath;
import static test_agent.utils.FileUtils.readFile;


public class UnitTestValidator {
    private String filePath;
    private String srcFilePath;
    private String codeCoverageReportPath;
    private String testCommand;
    private String testCommandDir;
    private int desiredCoverage;
    private static final Logger logger = Logger.getLogger(UnitTestValidator.class.getName());
    private CoverageProcessor coverageProcessor;
    private double currentCoverage;
    private Map<String, Double> lastCoveragePercentages;
    private double lastSourceFileCoverage;
    private String codeCoverageReport;

    // New fields for test suite analysis

    private Integer relevantLineNumberToInsertTestsAfter;
    private Integer relevantLineNumberToInsertImportsAfter;
    private String testingFramework;
    private int totalInputTokenCount;
    private int totalOutputTokenCount;
    private String projectRoot;
    private String language;
    private String sourceCode;
    private String testFilePath;
    private AgentCompletion agentCompletion;
    private int numAttempts;
    private List<Map<String, Object>> failedTestRuns = new ArrayList<>();


    public UnitTestValidator(
            String filePath,
            String srcFilePath,
            String codeCoverageReportPath,
            String testCommand,
            String testCommandDir,
            int desiredCoverage,
            String projectRoot,
            AgentCompletion agentCompletion,
            int numAttempts) {

        // Initialize existing fields
        this.filePath = filePath;
        this.srcFilePath = srcFilePath;
        this.codeCoverageReportPath = codeCoverageReportPath;
        this.testCommand = testCommand;
        this.testCommandDir = testCommandDir;
        this.desiredCoverage = desiredCoverage;
        this.lastCoveragePercentages = new HashMap<>();
        this.codeCoverageReport = "";

        // Initialize new fields
        this.projectRoot = projectRoot;
        this.agentCompletion = agentCompletion;
        this.numAttempts = numAttempts;
        this.testFilePath = filePath;
        this.totalInputTokenCount = 0;
        this.totalOutputTokenCount = 0;
        this.testingFramework = "Unknown";
        this.relevantLineNumberToInsertTestsAfter = null;
        this.relevantLineNumberToInsertImportsAfter = null;
        this.language = LanguageUtils.getCodeLanguageFromPath(srcFilePath);

        // Initialize the coverage processor
        this.coverageProcessor = new CoverageProcessor(
                codeCoverageReportPath,
                srcFilePath

        );

        // Read source code
        try {
            this.sourceCode = new String(Files.readAllBytes(Paths.get(srcFilePath)));
        } catch (IOException e) {
            logger.severe("Error reading source file: " + e.getMessage());
            this.sourceCode = "";
        }
    }



    // Add getter methods for new fields


    public List<Map<String, Object>> getFailedTestRuns() {
        return failedTestRuns;
    }


    public double getCurrentCoverage() {
        return currentCoverage;
    }


    public int getDesiredCoverage() {
        return desiredCoverage;
    }



    public Integer getRelevantLineNumberToInsertTestsAfter() {
        return relevantLineNumberToInsertTestsAfter;
    }

    public Integer getRelevantLineNumberToInsertImportsAfter() {
        return relevantLineNumberToInsertImportsAfter;
    }

    public String getTestingFramework() {
        return testingFramework;
    }

    public int getTotalInputTokenCount() {
        return totalInputTokenCount;
    }

    public int getTotalOutputTokenCount() {
        return totalOutputTokenCount;
    }


    public String getLanguage() {
        return language;
    }




public void initialTestSuiteAnalysis() throws Exception {
        try {
            // Analyze insert positions
            Integer relevantLineNumberToInsertTestsAfter = null;
            Integer relevantLineNumberToInsertImportsAfter = null;
            String testingFrameworkValue = null;
            int allowedAttempts = 3;
            int counterAttempts = 0;

            while ((relevantLineNumberToInsertTestsAfter == null ||
                    relevantLineNumberToInsertImportsAfter == null) &&
                    counterAttempts < allowedAttempts) {

                // Create numbered test file content
                String[] lines = readFile(testFilePath).split("\n");
                StringBuilder numberedContent = new StringBuilder();
                for (int i = 0; i < lines.length; i++) {
                    numberedContent.append(String.format("%d %s\n", i + 1, lines[i]));
                }
                System.out.println("Performing Test Suite Analysis.....");

                AnalysisResult result = agentCompletion.analyzeTestInsertLine(
                        language,
                        numberedContent.toString(),
                        getRelativePath(testFilePath, projectRoot)
                );
                //logger.info("Raw AI YAML response:\n" + result.getResponse());

                // Update token counts
                totalInputTokenCount += result.getInputTokenCount();
                totalOutputTokenCount += result.getOutputTokenCount();

                // Parse YAML response
                Map<String, Object> testsDict = YamlParser.loadYaml(result.getResponse());

                Object testsAfterObj = testsDict.get("relevant_line_number_to_insert_tests_after");
                Object importsAfterObj = testsDict.get("relevant_line_number_to_insert_imports_after");
                Object frameworkObj = testsDict.get("testing_framework");

                if (testsAfterObj instanceof Number) {
                    relevantLineNumberToInsertTestsAfter = ((Number) testsAfterObj).intValue();
                }
                if (importsAfterObj instanceof Number) {
                    relevantLineNumberToInsertImportsAfter = ((Number) importsAfterObj).intValue();
                }
                if (frameworkObj != null) {
                    testingFrameworkValue = String.valueOf(frameworkObj);
                }

                counterAttempts++;
            }

            if (relevantLineNumberToInsertTestsAfter == null) {
                throw new Exception("Failed to analyze the relevant line number to insert new tests");
            }
            if (relevantLineNumberToInsertImportsAfter == null) {
                throw new Exception("Failed to analyze the relevant line number to insert new imports");
            }

            // Store the results
            this.relevantLineNumberToInsertTestsAfter = relevantLineNumberToInsertTestsAfter;
            this.relevantLineNumberToInsertImportsAfter = relevantLineNumberToInsertImportsAfter;
            this.testingFramework = testingFrameworkValue != null ? testingFrameworkValue : "Unknown";

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during initial test suite analysis: " + e.getMessage(), e);
            throw new Exception("Error during initial test suite analysis", e);
        }
    }






    /**
     * Perform an initial build/test command to generate coverage report and get a baseline.
     *
     * @throws RuntimeException if the test command fails or coverage processing encounters an error
     */
    public void runCoverage() {
        //logger.info("Running build/test command to generate coverage report: \"" + testCommand + "\"");
          System.out.println("Running build/test command to generate coverage report: \"" + testCommand + "\"");
        // Execute the test command
        Runner.CommandResult result = Runner.runCommand(testCommand, testCommandDir);

        // Verify the command execution was successful
        if (result.getExitCode() != 0) {
            String errorMessage = String.format(
                    "Fatal: Error running test command. Are you sure the command is correct? \"%s\"\n" +
                            "Exit code %d.\nStdout:\n%s\nStderr:\n%s",
                    testCommand,
                    result.getExitCode(),
                    result.getStdout(),
                    result.getStderr()
            );
            throw new RuntimeException(errorMessage);
        }

        try {
            // Process the coverage report
            CoverageResult coverageResult = postProcessCoverageReport(result.getCommandStartTime());

            // Update the current coverage and coverage percentages
            this.currentCoverage = coverageResult.getOverallCoverage();
            this.lastCoveragePercentages = new HashMap<>(coverageResult.getCoveragePercentages());

            // Format and store the coverage report
            this.codeCoverageReport = String.format(
                    "Lines covered: %d\nLines missed: %d\nPercentage covered: %.2f%%",
                    coverageResult.getCoveredLinesCount(),
                    coverageResult.getMissedLinesCount(),
                    coverageResult.getOverallCoverage() * 100
            );

           // logger.info(String.format("Initial coverage: %.2f%%", this.currentCoverage * 100));

        } catch (AssertionError error) {
            // Handle coverage report processing errors
            logger.severe("Error in coverage processing: " + error.getMessage());
            throw new RuntimeException("Coverage processing failed", error);

        } catch (Exception e) {
            // Handle other types of errors
            logger.warning("Error parsing coverage report: " + e.getMessage());
            logger.info("Will default to using the full coverage report. " +
                    "You will need to check coverage manually for each passing test.");

            try {
                // Read the full coverage report
                this.codeCoverageReport = new String(Files.readAllBytes(Paths.get(codeCoverageReportPath)));

            } catch (IOException ioException) {
                logger.severe("Error reading coverage report file: " + ioException.getMessage());
                throw new RuntimeException("Failed to read coverage report", ioException);
            }
        }
    }


    /**
     * Process the coverage report and calculate coverage percentages.
     *
     * @param timeOfTestCommand The time when the test command was executed
     * @return CoverageResult containing the overall coverage percentage and a map of file-specific coverages
     */
    public CoverageResult postProcessCoverageReport(long timeOfTestCommand) {
        Map<String, Double> coveragePercentages = new HashMap<>();
        double percentageCovered;
        int totalLinesCovered;
        int totalLinesMissed;

        try {
            CoverageProcessor.CoverageData coverageData = coverageProcessor.processCoverageReport(timeOfTestCommand);

            totalLinesCovered = coverageData.getCoveredLines().size();
            totalLinesMissed = coverageData.getMissedLines().size();
            int totalLines = totalLinesCovered + totalLinesMissed;

            // Calculate coverage percentage
            percentageCovered = (totalLines > 0) ? (double) totalLinesCovered / totalLines : 0.0;

            // Update coverage percentage for the source file
            String sourceFileName = new java.io.File(srcFilePath).getName();
            coveragePercentages.put(sourceFileName, coverageData.getCoveragePercentage());

            if (srcFilePath.equals(sourceFileName)) {
                lastSourceFileCoverage = coverageData.getCoveragePercentage();
            }

           /* logger.info(String.format("Total lines covered: %d, Total lines missed: %d, Total lines: %d",
                    totalLinesCovered, totalLinesMissed, totalLines));*/
            //logger.info(String.format("Coverage: Percentage %.2f%%", percentageCovered * 100));

        } catch (Exception e) {
            logger.severe("Error processing coverage report: " + e.getMessage());
            throw new RuntimeException("Failed to process coverage report", e);
        }

        return new CoverageResult(
                percentageCovered,
                coveragePercentages,
                totalLinesCovered,
                totalLinesMissed
        );
    }

    public static class CoverageResult {
        private final double overallCoverage;
        private final Map<String, Double> coveragePercentages;
        private final int coveredLinesCount;
        private final int missedLinesCount;

        public CoverageResult(double overallCoverage,
                            Map<String, Double> coveragePercentages,
                            int coveredLinesCount,
                            int missedLinesCount) {
            this.overallCoverage = overallCoverage;
            this.coveragePercentages = new HashMap<>(coveragePercentages);
            this.coveredLinesCount = coveredLinesCount;
            this.missedLinesCount = missedLinesCount;
        }

        public double getOverallCoverage() {
            return overallCoverage;
        }

        public Map<String, Double> getCoveragePercentages() {
            return new HashMap<>(coveragePercentages);
        }

        public int getCoveredLinesCount() {
            return coveredLinesCount;
        }

        public int getMissedLinesCount() {
            return missedLinesCount;
        }
    }

    public String getCodeCoverageReport() {
        return codeCoverageReport;
    }


    private String extractErrorMessage(Map<String, Object> failDetails) {
        try {
            // Check if processedTestFile is null and handle it
            String processedTestFile = (String) failDetails.get("processedTestFile");
            if (processedTestFile == null) {
                logger.warning("processedTestFile is null in extractErrorMessage");
                // Use an empty string instead of null to avoid template error
                processedTestFile = "";
            }

            AnalysisResult result = agentCompletion.analyzeTestFailure(
                    getRelativePath(srcFilePath, projectRoot),
                    readFile(srcFilePath),
                    processedTestFile,
                    (String) failDetails.get("stdout"),
                    (String) failDetails.get("stderr"),
                    getRelativePath(testFilePath, projectRoot)
            );

            totalInputTokenCount += result.getInputTokenCount();
            totalOutputTokenCount += result.getOutputTokenCount();

            return result.getResponse().trim();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error extracting error message: " + e.getMessage(), e);
            return "";
        }
    }


    /**
     * Validate a generated test by inserting it into the test file, running the test, and checking for pass/fail.
     *
     * @param generatedTest The generated test to validate, containing test code and additional imports.
     * @return A TestValidationResult containing the status of the test validation, including pass/fail status,
     *         exit code, stderr, stdout, and the test details.
     */
    public TestValidationResult validateTest(GeneratedTest generatedTest) {
        // Store original content of the test file
        String originalContent;
        try {
            originalContent = new String(Files.readAllBytes(Paths.get(testFilePath)));
        } catch (IOException e) {
            logger.severe("Error reading test file: " + e.getMessage());
            return new TestValidationResult.Builder()
                    .status(TestValidationResult.STATUS_FAIL)
                    .reason("Error reading test file: " + e.getMessage())
                    .test(generatedTest)
                    .language(language)
                    .sourceFile(sourceCode)
                    .build();
        }

        String processedTest = "";
        try {
            // Extract test code and additional imports
            String testCode = generatedTest.getTestCode().trim();
            String additionalImports = generatedTest.getNewImportsCode().trim();

            // Clean up the additional imports if necessary
            if (additionalImports != null && additionalImports.startsWith("\"") && additionalImports.endsWith("\"")) {
                additionalImports = additionalImports.substring(1, additionalImports.length() - 1);
            }

            // Check if additional_imports only contains '""'
            if (additionalImports != null && additionalImports.equals("\"\"")) {
                additionalImports = "";
            }

            // Get the relevant line numbers for inserting tests and imports
            Integer relevantLineNumberToInsertTestsAfter = this.relevantLineNumberToInsertTestsAfter;
            Integer relevantLineNumberToInsertImportsAfter = this.relevantLineNumberToInsertImportsAfter;



            int exitCode = 0;
            if (!testCode.isEmpty() && relevantLineNumberToInsertTestsAfter != null) {
                // Step 1: Insert imports first, then insert the generated test code
                List<String> additionalImportsLines = new ArrayList<>();
                String[] originalContentLines = originalContent.split("\n");

                // Build a deduplicated list of import lines
                if (additionalImports != null && !additionalImports.isEmpty()) {
                    String[] rawImportLines = additionalImports.split("\n");
                    for (String line : rawImportLines) {
                        // Only add if it's not already present (stripped match) in the file
                        boolean isDuplicate = false;
                        String trimmedLine = line.trim();
                        if (!trimmedLine.isEmpty()) {
                            for (String existing : originalContentLines) {
                                if (trimmedLine.equals(existing.trim())) {
                                    isDuplicate = true;
                                    break;
                                }
                            }
                            if (!isDuplicate) {
                                additionalImportsLines.add(line);
                            }
                        }
                    }
                }

                int insertedLinesCount = 0;
                List<String> processedContentLines = new ArrayList<>(Arrays.asList(originalContentLines));

                if (relevantLineNumberToInsertImportsAfter != null && !additionalImportsLines.isEmpty()) {
                    insertedLinesCount = additionalImportsLines.size();
                    processedContentLines.addAll(relevantLineNumberToInsertImportsAfter, additionalImportsLines);
                }

                // Offset the test insertion point by however many lines we just inserted
                int updatedTestInsertionPoint = relevantLineNumberToInsertTestsAfter;
                if (insertedLinesCount > 0) {
                    updatedTestInsertionPoint += insertedLinesCount;
                }

                // Now insert the test code at 'updatedTestInsertionPoint'
                String[] testCodeLines = testCode.split("\n");
                List<String> testCodeLinesList = new ArrayList<>(Arrays.asList(testCodeLines));
                processedContentLines.addAll(updatedTestInsertionPoint, testCodeLinesList);

                processedTest = String.join("\n", processedContentLines);
                //logger.info("Test file content just before running:\n" + processedTest);

                try {
                    Files.write(Paths.get(testFilePath), processedTest.getBytes(),
                            StandardOpenOption.CREATE,          // Create if doesn't exist (usually already does)
                            StandardOpenOption.TRUNCATE_EXISTING, // Overwrite existing content
                            StandardOpenOption.WRITE,           // Open for writing
                            StandardOpenOption.SYNC);



                } catch (IOException e) {
                    logger.severe("Error writing to test file: " + e.getMessage());
                    return new TestValidationResult.Builder()
                            .status(TestValidationResult.STATUS_FAIL)
                            .reason("Error writing to test file: " + e.getMessage())
                            .test(generatedTest)
                            .language(language)
                            .sourceFile(sourceCode)
                            .originalTestFile(originalContent)
                            .build();
                }

                // Step 2: Run the test using the Runner class
                Runner.CommandResult result = null;
                for (int i = 0; i < numAttempts; i++) {
                    logger.info("Running test with the following command: \"" + testCommand + "\"");
                    result = Runner.runCommand(testCommand, testCommandDir);
                    exitCode = result.getExitCode();
                    if (exitCode != 0) {
                        break;
                    }
                }

                if (result == null) {
                    // This should never happen, but just in case
                    return new TestValidationResult.Builder()
                            .status(TestValidationResult.STATUS_FAIL)
                            .reason("Test execution failed with null result")
                            .test(generatedTest)
                            .language(language)
                            .sourceFile(sourceCode)
                            .originalTestFile(originalContent)
                            .processedTestFile(processedTest)
                            .build();
                }

                // Step 3: Check for pass/fail from the Runner object
                if (exitCode != 0) {
                    // Test failed, roll back the test file to its original content
                    try {
                        Files.write(Paths.get(testFilePath), originalContent.getBytes(),
                                StandardOpenOption.CREATE,
                                StandardOpenOption.TRUNCATE_EXISTING,
                                StandardOpenOption.WRITE,
                                StandardOpenOption.SYNC);
                    } catch (IOException e) {
                        logger.severe("Error rolling back test file: " + e.getMessage());
                    }

                    logger.info("Skipping a generated test that failed");

                    TestValidationResult failResult = new TestValidationResult.Builder()
                            .status(TestValidationResult.STATUS_FAIL)
                            .reason("Test failed")
                            .exitCode(exitCode)
                            .stderr(result.getStderr())
                            .stdout(result.getStdout())
                            .test(generatedTest)
                            .language(language)
                            .sourceFile(sourceCode)
                            .originalTestFile(originalContent)
                            .processedTestFile(processedTest)
                            .build();

                    String errorMessage = extractErrorMessage(failResult.toMap());
                    if (errorMessage != null && !errorMessage.isEmpty()) {
                        logger.severe("Error message summary:\n" + errorMessage);
                        failResult = new TestValidationResult.Builder()
                                .status(TestValidationResult.STATUS_FAIL)
                                .reason("Test failed")
                                .exitCode(exitCode)
                                .stderr(result.getStderr())
                                .stdout(result.getStdout())
                                .test(generatedTest)
                                .language(language)
                                .sourceFile(sourceCode)
                                .originalTestFile(originalContent)
                                .processedTestFile(processedTest)
                                .errorMessage(errorMessage)
                                .build();
                    }

                    // Add to failed test runs
                    Map<String, Object> failedTest = new HashMap<>();
                    failedTest.put("code", generatedTest.toMap());
                    failedTest.put("error_message", errorMessage);
                    failedTestRuns.add(failedTest);

                    return failResult;
                }

                // If test passed, check for coverage increase
                try {
                    CoverageResult coverageResult = postProcessCoverageReport(result.getCommandStartTime());
                    double newPercentageCovered = coverageResult.getOverallCoverage();
                    Map<String, Double> newCoveragePercentages = coverageResult.getCoveragePercentages();

                    if (newPercentageCovered <= currentCoverage) {
                        // Coverage has not increased, rollback the test by removing it from the test file
                        try {
                            Files.write(Paths.get(testFilePath), originalContent.getBytes(),
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.TRUNCATE_EXISTING,
                                    StandardOpenOption.WRITE,
                                    StandardOpenOption.SYNC);
                        } catch (IOException e) {
                            logger.severe("Error rolling back test file: " + e.getMessage());
                        }

                        logger.info("Test did not increase coverage. Rolling back.");

                        TestValidationResult failResult = new TestValidationResult.Builder()
                                .status(TestValidationResult.STATUS_FAIL)
                                .reason("Coverage did not increase. Maybe the test did run but did not increase coverage, or maybe the test execution was skipped due to some problem")
                                .exitCode(exitCode)
                                .stderr(result.getStderr())
                                .stdout(result.getStdout())
                                .test(generatedTest)
                                .language(language)
                                .sourceFile(sourceCode)
                                .originalTestFile(originalContent)
                                .processedTestFile(processedTest)
                                .build();

                        // Add to failed test runs
                        Map<String, Object> failedTest = new HashMap<>();
                        failedTest.put("code", generatedTest);
                        failedTest.put("error_message", "Test did not increase code coverage");
                        failedTestRuns.add(failedTest);

                        return failResult;
                    }

                    // If we got here, everything passed and coverage increased
                    // Update the insertion point for the next test

                    //this.relevantLineNumberToInsertTestsAfter += additionalImportsLines.size();
                    int numberOfTestLinesInserted = testCodeLinesList.size();
                    this.relevantLineNumberToInsertTestsAfter += (insertedLinesCount + numberOfTestLinesInserted);
                    logger.fine("Updated relevantLineNumberToInsertTestsAfter to: " + this.relevantLineNumberToInsertTestsAfter);

                    // Log coverage increases
                    for (Map.Entry<String, Double> entry : newCoveragePercentages.entrySet()) {
                        String key = entry.getKey();
                        double newCoverage = entry.getValue();
                        double oldCoverage = lastCoveragePercentages.getOrDefault(key, 0.0);

                        if (newCoverage > oldCoverage) {
                            String sourceFileName = new File(srcFilePath).getName();
                            if (key.equals(sourceFileName)) {
                                logger.info(String.format("Coverage for provided source file: %s increased from %.2f%% to %.2f%%",
                                        key, oldCoverage * 100, newCoverage * 100));
                            } else {
                                logger.info(String.format("Coverage for non-source file: %s increased from %.2f%% to %.2f%%",
                                        key, oldCoverage * 100, newCoverage * 100));
                            }
                        }
                    }

                    // Update current coverage and percentages
                    this.currentCoverage = newPercentageCovered;
                    this.lastCoveragePercentages = new HashMap<>(newCoveragePercentages);

                    logger.info(String.format("Test passed and coverage increased. Current coverage: %.2f%%",
                            newPercentageCovered * 100));

                    return new TestValidationResult.Builder()
                            .status(TestValidationResult.STATUS_PASS)
                            .reason("")
                            .exitCode(exitCode)
                            .stderr(result.getStderr())
                            .stdout(result.getStdout())
                            .test(generatedTest)
                            .language(language)
                            .sourceFile(sourceCode)
                            .originalTestFile(originalContent)
                            .processedTestFile(processedTest)
                            .build();

                } catch (Exception e) {
                    // Handle errors gracefully
                    logger.severe("Error during coverage verification: " + e.getMessage());

                    // Roll back even in case of error
                    try {
                        Files.write(Paths.get(testFilePath), originalContent.getBytes(),
                                StandardOpenOption.CREATE,
                                StandardOpenOption.TRUNCATE_EXISTING,
                                StandardOpenOption.WRITE,
                                StandardOpenOption.SYNC);
                    } catch (IOException ioEx) {
                        logger.severe("Error rolling back test file: " + ioEx.getMessage());
                    }

                    TestValidationResult failResult = new TestValidationResult.Builder()
                            .status(TestValidationResult.STATUS_FAIL)
                            .reason("Runtime error")
                            .exitCode(exitCode)
                            .stderr(result.getStderr())
                            .stdout(result.getStdout())
                            .test(generatedTest)
                            .language(language)
                            .sourceFile(sourceCode)
                            .originalTestFile(originalContent)
                            .processedTestFile(processedTest)
                            .build();

                    // Add to failed test runs
                    Map<String, Object> failedTest = new HashMap<>();
                    failedTest.put("code", generatedTest);
                    failedTest.put("error_message", "Coverage verification error");
                    failedTestRuns.add(failedTest);

                    return failResult;
                }
            }

            // If we get here, there was an issue with the test code or insertion points
            return new TestValidationResult.Builder()
                    .status(TestValidationResult.STATUS_FAIL)
                    .reason("Invalid test code or insertion points")
                    .test(generatedTest)
                    .language(language)
                    .sourceFile(sourceCode)
                    .originalTestFile(originalContent)
                    .build();

        } catch (Exception e) {
            logger.severe("Error validating test: " + e.getMessage());
            e.printStackTrace();

            return new TestValidationResult.Builder()
                    .status(TestValidationResult.STATUS_FAIL)
                    .reason("Error validating test: " + e.getMessage())
                    .stderr(e.toString())
                    .test(generatedTest)
                    .language(language)
                    .sourceFile(sourceCode)
                    .originalTestFile(originalContent)
                    .processedTestFile(processedTest)
                    .build();
        }
    }

}

