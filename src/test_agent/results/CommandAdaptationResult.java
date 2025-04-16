package test_agent.results;

public class CommandAdaptationResult extends AnalysisResult {
    public CommandAdaptationResult(String Response, int InputTokenCount, int OutputTokenCount, String userPrompt) {
        super(Response, InputTokenCount, OutputTokenCount, userPrompt);
    }
}
