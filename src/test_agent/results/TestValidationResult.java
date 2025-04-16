package test_agent.results;

import test_agent.models.GeneratedTest;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the result of validating a generated test.
 * This class stores information about test execution status, error messages,
 * and other relevant details for test validation.
 */
public class TestValidationResult {
    // Constants for status
    public static final String STATUS_PASS = "PASS";
    public static final String STATUS_FAIL = "FAIL";

    private String status;
    private String reason;
    private Integer exitCode;
    private String stderr;
    private String stdout;
    private GeneratedTest test;
    private String language;
    private String sourceFile;
    private String originalTestFile;
    private String processedTestFile;
    private String errorMessage;

    /**
     * Private constructor used by the Builder.
     */
    private TestValidationResult() {
    }

    /**
     * Builder class for TestValidationResult.
     */
    public static class Builder {
        private final TestValidationResult result = new TestValidationResult();

        public Builder status(String status) {
            result.status = status;
            return this;
        }

        public Builder reason(String reason) {
            result.reason = reason;
            return this;
        }

        public Builder exitCode(Integer exitCode) {
            result.exitCode = exitCode;
            return this;
        }

        public Builder stderr(String stderr) {
            result.stderr = stderr;
            return this;
        }

        public Builder stdout(String stdout) {
            result.stdout = stdout;
            return this;
        }

        public Builder test(GeneratedTest test) {
            result.test = test;
            return this;
        }

        public Builder language(String language) {
            result.language = language;
            return this;
        }

        public Builder sourceFile(String sourceFile) {
            result.sourceFile = sourceFile;
            return this;
        }

        public Builder originalTestFile(String originalTestFile) {
            result.originalTestFile = originalTestFile;
            return this;
        }

        public Builder processedTestFile(String processedTestFile) {
            result.processedTestFile = processedTestFile;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            result.errorMessage = errorMessage;
            return this;
        }

        public TestValidationResult build() {
            return result;
        }
    }

    /**
     * Creates a TestValidationResult from a map representation.
     *
     * @param details A map containing test validation details
     * @return A new TestValidationResult instance
     */
    public static TestValidationResult fromMap(Map<String, Object> details) {
        Builder builder = new Builder();

        if (details.containsKey("status")) {
            builder.status((String) details.get("status"));
        }

        if (details.containsKey("reason")) {
            builder.reason((String) details.get("reason"));
        }

        if (details.containsKey("exit_code")) {
            Object exitCodeObj = details.get("exit_code");
            if (exitCodeObj instanceof Integer) {
                builder.exitCode((Integer) exitCodeObj);
            } else if (exitCodeObj != null) {
                builder.exitCode(Integer.parseInt(exitCodeObj.toString()));
            }
        }

        if (details.containsKey("stderr")) {
            builder.stderr((String) details.get("stderr"));
        }

        if (details.containsKey("stdout")) {
            builder.stdout((String) details.get("stdout"));
        }

        if (details.containsKey("test")) {
            Object testObj = details.get("test");
            if (testObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> testMap = (Map<String, Object>) testObj;
                builder.test(GeneratedTest.fromMap(testMap));
            } else if (testObj instanceof GeneratedTest) {
                builder.test((GeneratedTest) testObj);
            }
        }

        if (details.containsKey("language")) {
            builder.language((String) details.get("language"));
        }

        if (details.containsKey("source_file")) {
            builder.sourceFile((String) details.get("source_file"));
        }

        if (details.containsKey("original_test_file")) {
            builder.originalTestFile((String) details.get("original_test_file"));
        }

        if (details.containsKey("processed_test_file")) {
            builder.processedTestFile((String) details.get("processed_test_file"));
        }

        if (details.containsKey("error_message")) {
            builder.errorMessage((String) details.get("error_message"));
        }

        return builder.build();
    }

    /**
     * Converts this TestValidationResult to a map representation.
     *
     * @return A map containing all the test validation details
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        map.put("reason", reason);
        map.put("exitCode", exitCode);
        map.put("stderr", stderr);
        map.put("stdout", stdout);
        map.put("test", test);
        map.put("language", language);
        map.put("sourceFile", sourceFile);
        map.put("originalTestFile", originalTestFile);
        map.put("processedTestFile", processedTestFile);
        map.put("errorMessage", errorMessage);
        return map;
    }

    /**
     * Checks if the test validation passed.
     *
     * @return true if the status is PASS, false otherwise
     */
    public boolean isPassed() {
        return STATUS_PASS.equals(status);
    }

    /**
     * Checks if the test validation failed.
     *
     * @return true if the status is FAIL, false otherwise
     */
    public boolean isFailed() {
        return STATUS_FAIL.equals(status);
    }

    // Getters
    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public String getStderr() {
        return stderr;
    }

    public String getStdout() {
        return stdout;
    }

    public GeneratedTest getTest() {
        return test;
    }

    public String getLanguage() {
        return language;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public String getOriginalTestFile() {
        return originalTestFile;
    }

    public String getProcessedTestFile() {
        return processedTestFile;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "TestValidationResult{" +
                "status='" + status + '\'' +
                ", reason='" + reason + '\'' +
                ", exitCode=" + exitCode +
                ", test=" + test +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}