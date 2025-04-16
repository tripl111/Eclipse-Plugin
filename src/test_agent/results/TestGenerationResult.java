package test_agent.results;

public class TestGenerationResult extends AnalysisResult {
    public TestGenerationResult(String content, int promptTokens, int completionTokens, String userPrompt) {
        super(content, promptTokens, completionTokens, userPrompt);
    }
}


