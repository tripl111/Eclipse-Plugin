package test_agent.results;

/**
 * Represents a response from an AI language model.
 * Contains the response text and token usage information.
 */
public class ModelResponse {
    private final String response;
    private final int promptTokens;
    private final int completionTokens;

    /**
     * Constructs a ModelResponse with the provided response text and token counts.
     *
     * @param response The text response from the model
     * @param promptTokens The number of tokens used in the prompt
     * @param completionTokens The number of tokens used in the completion
     */
    public ModelResponse(String response, int promptTokens, int completionTokens) {
        this.response = response;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
    }

    public String getResponse() {
        return response;
    }

    public int getPromptTokens() {
        return promptTokens;
    }


    public int getCompletionTokens() {
        return completionTokens;
    }


    public int getTotalTokens() {
        return promptTokens + completionTokens;
    }
}