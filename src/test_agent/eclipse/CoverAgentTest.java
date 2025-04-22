package test_agent.eclipse;



import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class to demonstrate the usage of CoverAgent with a Spring Boot project.
 */
public class CoverAgentTest {

    public static void main(String[] args) {
        try {
            // Define project path and file paths
            String projectPath = "C:\\Users\\sridi\\piggymetrics\\auth-service";

            // Verify the project directory exists
            Path projectDir = Paths.get(projectPath);
            if (!Files.exists(projectDir)) {
                throw new RuntimeException("Project directory not found: " + projectPath);
            }

            // Define source and test file paths
            String sourceFilePath = "C:\\Users\\sridi\\piggymetrics\\auth-service\\src\\main\\java\\com\\piggymetrics\\auth\\controller\\UserController.java";
            String includedFilePath = "C:\\Users\\sridi\\piggymetrics\\auth-service\\src\\main\\java\\com\\piggymetrics\\auth\\domain\\User.java";
            String includedFilePath2 = "C:\\Users\\sridi\\piggymetrics\\auth-service\\src\\main\\java\\com\\piggymetrics\\auth\\service\\UserServiceImpl.java";
            String testFilePath = "C:\\Users\\sridi\\piggymetrics\\auth-service\\src\\test\\java\\com\\piggymetrics\\auth\\controller\\UserControllerTest.java";
            String coverageReportPath = "C:\\Users\\sridi\\piggymetrics\\auth-service\\target\\site\\jacoco\\jacoco.xml";

            // Verify source, included, and test files exist
            if (!Files.exists(Paths.get(sourceFilePath))) {
                throw new RuntimeException("Source file not found: " + sourceFilePath);
            }
            if (!Files.exists(Paths.get(includedFilePath))) {
                throw new RuntimeException("Included file not found: " + includedFilePath);
            }
            if (!Files.exists(Paths.get(testFilePath))) {
                throw new RuntimeException("Test file not found: " + testFilePath);
            }

            // Debug: Check if coverage report exists before running tests
          /*  Path coverageReportFile = Paths.get(coverageReportPath);
            if (Files.exists(coverageReportFile)) {
                System.out.println("Coverage report exists before running tests. Size: " +
                        Files.size(coverageReportFile) + " bytes");
                // Print first few lines of the report to verify content
                List<String> reportLines = Files.readAllLines(coverageReportFile, StandardCharsets.UTF_8);
                System.out.println("Report preview:");
                for (int i = 0; i < Math.min(10, reportLines.size()); i++) {
                    System.out.println(reportLines.get(i));
                }
            } else {
                System.out.println("Coverage report does not exist before running tests: " + coverageReportPath);
            }*/

            // Define test command for Spring Boot project
            String testCommand = "mvn -f \"" + projectPath + "\"  clean test";
            //System.out.println("Using test command: " + testCommand);


            String apiKey = System.getenv("TEST_AGENT_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalStateException("TEST_AGENT_API_KEY environment variable is not set");
            }


            // Create a list for included files - add both source file and Customer entity
            List<String> includedFiles = new ArrayList<>();
            includedFiles.add(includedFilePath);
            includedFiles.add(includedFilePath2);




            // Create CoverAgentArgs using the Builder pattern
            CoverAgentArgs coverAgentArgs = new CoverAgentArgs.Builder()
                    .sourceFilePath(sourceFilePath)
                    .testFilePath(testFilePath)
                    .testFileOutputPath(testFilePath)
                    .codeCoverageReportPath(coverageReportPath)
                    .testCommand(testCommand)
                    //.model("google/gemini-2.0-flash-001")
                  // .model ("google/gemini-2.5-pro-preview-03-25")
                  // .model("google/gemini-2.5-pro-exp-03-25:free")
                     .model("deepseek/deepseek-chat-v3-0324:free")
                   //.model("deepseek/deepseek-chat:free")
                    .testCommandDir(projectPath)
                    .includedFiles(includedFiles)
                    .coverageType("jacoco")
                    .desiredCoverage(100)
                    .additionalInstructions(" ")
                    .projectRoot(projectPath)
                    .maxIterations(2)
                    .runEachTestSeparately(true)
                    .runTestsMultipleTimes(1)
                    .apiKey(apiKey)
                    .siteUrl("http://localhost")
                    .siteName("SpringBootCoverAgentTest")
                    .build();


            CoverAgent coverAgent = new CoverAgent(coverAgentArgs, null); // Use default AgentCompletion


            coverAgent.run();




        } catch (Exception e) {
            System.err.println("Error testing CoverAgent: " + e.getMessage());
            e.printStackTrace();
        }
    }
}