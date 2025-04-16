package test_agent.eclipse;

import java.util.List;

/**
 * Arguments class for the CoverAgent.
 * This class holds all the configuration parameters needed for test generation and validation.
 */
public class CoverAgentArgs {
    private String sourceFilePath;
    private String testFilePath;
    private String testFileOutputPath;
    private String codeCoverageReportPath;
    private String testCommand;
    private String testCommandOriginal;
    private String model;
    private String testCommandDir;
    private List<String> includedFiles;
    private String coverageType;
    private int desiredCoverage;
    private String additionalInstructions;
    private String projectRoot;
    private int maxIterations;
    private boolean runEachTestSeparately;
    private int runTestsMultipleTimes;
    private String apiKey;
    private String siteUrl;
    private String siteName;

    /**
     * Default constructor
     */
    public CoverAgentArgs() {
        // Default values
        this.maxIterations = 5;
        this.desiredCoverage = 80;
        this.runEachTestSeparately = false;
        this.runTestsMultipleTimes = 1;
    }

    /**
     * Constructor with all parameters
     */
    public CoverAgentArgs(String sourceFilePath, String testFilePath, String testFileOutputPath,
                          String codeCoverageReportPath, String testCommand, String model,
                          String testCommandDir, List<String> includedFiles, String coverageType,
                          int desiredCoverage, String additionalInstructions,
                          boolean useReportCoverageFeatureFlag, String projectRoot,
                          int maxIterations, boolean diffCoverage, boolean strictCoverage,
                          String reportFilepath, boolean runEachTestSeparately,
                          int runTestsMultipleTimes, String apiKey, String siteUrl, String siteName) {
        this.sourceFilePath = sourceFilePath;
        this.testFilePath = testFilePath;
        this.testFileOutputPath = testFileOutputPath;
        this.codeCoverageReportPath = codeCoverageReportPath;
        this.testCommand = testCommand;
        this.model = model;
        this.testCommandDir = testCommandDir;
        this.includedFiles = includedFiles;
        this.coverageType = coverageType;
        this.desiredCoverage = desiredCoverage;
        this.additionalInstructions = additionalInstructions;
        this.projectRoot = projectRoot;
        this.maxIterations = maxIterations;
        this.runEachTestSeparately = runEachTestSeparately;
        this.runTestsMultipleTimes = runTestsMultipleTimes;
        this.apiKey = apiKey;
        this.siteUrl = siteUrl;
        this.siteName = siteName;
    }

    // Getters and setters
    public String getSourceFilePath() {
        return sourceFilePath;
    }

    public void setSourceFilePath(String sourceFilePath) {
        this.sourceFilePath = sourceFilePath;
    }

    public String getTestFilePath() {
        return testFilePath;
    }

    public void setTestFilePath(String testFilePath) {
        this.testFilePath = testFilePath;
    }

    public String getTestFileOutputPath() {
        return testFileOutputPath;
    }

    public void setTestFileOutputPath(String testFileOutputPath) {
        this.testFileOutputPath = testFileOutputPath;
    }

    public String getCodeCoverageReportPath() {
        return codeCoverageReportPath;
    }

    public void setCodeCoverageReportPath(String codeCoverageReportPath) {
        this.codeCoverageReportPath = codeCoverageReportPath;
    }

    public String getTestCommand() {
        return testCommand;
    }

    public void setTestCommand(String testCommand) {
        this.testCommand = testCommand;
    }

    public String getTestCommandOriginal() {
        return testCommandOriginal;
    }

    public void setTestCommandOriginal(String testCommandOriginal) {
        this.testCommandOriginal = testCommandOriginal;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getTestCommandDir() {
        return testCommandDir;
    }

    public void setTestCommandDir(String testCommandDir) {
        this.testCommandDir = testCommandDir;
    }

    public List<String> getIncludedFiles() {
        return includedFiles;
    }

    public void setIncludedFiles(List<String> includedFiles) {
        this.includedFiles = includedFiles;
    }

    public String getCoverageType() {
        return coverageType;
    }

    public void setCoverageType(String coverageType) {
        this.coverageType = coverageType;
    }

    public int getDesiredCoverage() {
        return desiredCoverage;
    }

    public void setDesiredCoverage(int desiredCoverage) {
        this.desiredCoverage = desiredCoverage;
    }

    public String getAdditionalInstructions() {
        return additionalInstructions;
    }

    public void setAdditionalInstructions(String additionalInstructions) {
        this.additionalInstructions = additionalInstructions;
    }



    public String getProjectRoot() {
        return projectRoot;
    }

    public void setProjectRoot(String projectRoot) {
        this.projectRoot = projectRoot;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }




    public boolean isRunEachTestSeparately() {
        return runEachTestSeparately;
    }

    public void setRunEachTestSeparately(boolean runEachTestSeparately) {
        this.runEachTestSeparately = runEachTestSeparately;
    }

    public int getRunTestsMultipleTimes() {
        return runTestsMultipleTimes;
    }

    public void setRunTestsMultipleTimes(int runTestsMultipleTimes) {
        this.runTestsMultipleTimes = runTestsMultipleTimes;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    /**
     * Builder class for CoverAgentArgs
     */
    public static class Builder {
        private final CoverAgentArgs args = new CoverAgentArgs();

        public Builder sourceFilePath(String sourceFilePath) {
            args.setSourceFilePath(sourceFilePath);
            return this;
        }

        public Builder testFilePath(String testFilePath) {
            args.setTestFilePath(testFilePath);
            return this;
        }

        public Builder testFileOutputPath(String testFileOutputPath) {
            args.setTestFileOutputPath(testFileOutputPath);
            return this;
        }

        public Builder codeCoverageReportPath(String codeCoverageReportPath) {
            args.setCodeCoverageReportPath(codeCoverageReportPath);
            return this;
        }

        public Builder testCommand(String testCommand) {
            args.setTestCommand(testCommand);
            return this;
        }

        public Builder model(String model) {
            args.setModel(model);
            return this;
        }

        public Builder testCommandDir(String testCommandDir) {
            args.setTestCommandDir(testCommandDir);
            return this;
        }

        public Builder includedFiles(List<String> includedFiles) {
            args.setIncludedFiles(includedFiles);
            return this;
        }

        public Builder coverageType(String coverageType) {
            args.setCoverageType(coverageType);
            return this;
        }

        public Builder desiredCoverage(int desiredCoverage) {
            args.setDesiredCoverage(desiredCoverage);
            return this;
        }

        public Builder additionalInstructions(String additionalInstructions) {
            args.setAdditionalInstructions(additionalInstructions);
            return this;
        }



        public Builder projectRoot(String projectRoot) {
            args.setProjectRoot(projectRoot);
            return this;
        }

        public Builder maxIterations(int maxIterations) {
            args.setMaxIterations(maxIterations);
            return this;
        }





        public Builder runEachTestSeparately(boolean runEachTestSeparately) {
            args.setRunEachTestSeparately(runEachTestSeparately);
            return this;
        }

        public Builder runTestsMultipleTimes(int runTestsMultipleTimes) {
            args.setRunTestsMultipleTimes(runTestsMultipleTimes);
            return this;
        }

        public Builder apiKey(String apiKey) {
            args.setApiKey(apiKey);
            return this;
        }

        public Builder siteUrl(String siteUrl) {
            args.setSiteUrl(siteUrl);
            return this;
        }

        public Builder siteName(String siteName) {
            args.setSiteName(siteName);
            return this;
        }

        public CoverAgentArgs build() {
            return args;
        }
    }
}