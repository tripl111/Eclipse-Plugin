package test_agent.results;

public class AnalysisResult {
    private final String Response;
    private final int InputTokenCount;
    private final int OutputTokenCount;
    private final String userPrompt;

    public AnalysisResult(String Response, int InputTokenCount, int OutputTokenCount, String userPrompt) {
        this.Response = Response;
        this.InputTokenCount = InputTokenCount;
        this.OutputTokenCount = OutputTokenCount;
        this.userPrompt = userPrompt;
    }

    public String getResponse() { return Response; }
    public int getInputTokenCount() { return InputTokenCount; }
    public int getOutputTokenCount() { return OutputTokenCount; }
    public String getuserPrompt() { return userPrompt; }
}