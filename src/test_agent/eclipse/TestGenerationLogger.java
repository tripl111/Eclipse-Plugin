package test_agent.eclipse;

import java.util.logging.Logger;

/**
 * Handles formatted console output for the test generation process.
 * Provides consistent formatting and visual indicators for different stages
 * of the test generation and validation process.
 */
public class TestGenerationLogger {
    private static final Logger logger = Logger.getLogger(TestGenerationLogger.class.getName());

    public static void printHeader() {
        System.out.println("\n========================================");
        System.out.println("🧪 Starting Test Generation Process");
        System.out.println("========================================\n");
    }

    public static void printInitialStatus(double initialCoverage, int targetCoverage) {
        System.out.println("📊 Coverage Status:");
        System.out.println("   Initial Coverage: " + String.format("%.2f%%", initialCoverage));
        System.out.println("   Target Coverage:  " + targetCoverage + "%");
        System.out.println("----------------------------------------\n");
    }

    public static void printIterationHeader(int current, int max) {
        System.out.println("\n🔄 Iteration " + current + "/" + max);
        System.out.println("----------------------------------------");
    }

    public static void printTestGenerationStart(int count) {
        System.out.println("\n🧪 Generating " + count + " New Tests");
        System.out.println("----------------------------------------");
    }

    public static void printTestValidationStatus(int testNumber, int totalTests) {
        System.out.println("   ✓ Validating Test " + testNumber + "/" + totalTests);
    }

    public static void printCoverageUpdate(double previousCoverage, double newCoverage) {
        if (newCoverage > previousCoverage) {
            System.out.println("   📈 Coverage increased: " +
                    String.format("%.2f%% → %.2f%% (+%.2f%%)",
                            previousCoverage,
                            newCoverage,
                            newCoverage - previousCoverage));
        }
    }

    public static void printFinalResults(boolean targetReached, int iterations, double finalCoverage, int targetCoverage) {
        System.out.println("\n========================================");
        System.out.println("🎯 Final Results");
        System.out.println("========================================");
        System.out.println("   • Final Coverage:   " + String.format("%.2f%%", finalCoverage));
        System.out.println("   • Target Coverage:  " + targetCoverage + "%");
        System.out.println("   • Total Iterations: " + iterations);
        System.out.println("   • Status: " + (targetReached ? "✅ Target Reached" : "⚠️ Target Not Reached"));
    }

    public static void printTokenUsage(String modelName, long inputTokens, long outputTokens) {
        System.out.println("\n📝 Token Usage Statistics");
        System.out.println("----------------------------------------");
        System.out.println("   • Model:          " + modelName);
        System.out.println("   • Input Tokens:   " + inputTokens);
        System.out.println("   • Output Tokens:  " + outputTokens);
        System.out.println("   • Total Tokens:   " + (inputTokens + outputTokens));
    }

    public static void printTestValidationError(String errorMessage) {
        System.out.println("   ❌ Test Validation Error: " + errorMessage);
    }

    public static void printGenerationError(String errorMessage) {
        System.out.println("   ⚠️ Generation Error: " + errorMessage);
    }
}