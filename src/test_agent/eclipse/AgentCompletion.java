package test_agent.eclipse;

import test_agent.results.AnalysisResult;
import test_agent.results.CommandAdaptationResult;
import test_agent.results.TestGenerationResult;

public interface AgentCompletion {
    TestGenerationResult generateTests(
            String sourceFileName,
            int maxTests,
            String sourceFileNumbered,
            String codeCoverageReport,
            String language,
            String testFile,
            String testFileName,
            String testingFramework,
            String additionalInstructionsText,
            String additionalIncludesSection,
            String failedTestsSection
    );

    AnalysisResult analyzeTestFailure(
            String sourceFileName,
            String sourceFile,
            String processedTestFile,
            String stdout,
            String stderr,
            String testFileName
    );

    AnalysisResult analyzeTestInsertLine(
            String language,
            String testFileNumbered,
            String testFileName

    );

    AnalysisResult analyzeTestAgainstContext(
            String language,
            String testFileContent,
            String testFileNameRel,
            String contextFilesNamesRel
    );



    CommandAdaptationResult adaptTestCommandForSingleTest(
            String testFileRelativePath,
            String testCommand,
            String projectRootDir
    );
}