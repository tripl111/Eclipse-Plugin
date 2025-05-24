package test_agent.eclipse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import test_agent.results.ModelResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles API calls to language models through the OpenRouter API.
 * Each instance is configured to use a specific model.
 * Supports both streaming and non-streaming responses.
 */
public class AICaller {
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final int DEFAULT_TIMEOUT_SECONDS = 200;
    private int maxRetries = 3;
    private long initialRetryDelayMs = 1000;
    private long maxRetryDelayMs = 10000;
    private final Logger logger = Logger.getLogger(AICaller.class.getName());

    private final String apiKey;
    private final String siteUrl;
    private final String siteName;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Constructs an AICaller configured for a specific model.
     *
     * @param apiKey   The OpenRouter API key.
     * @param siteUrl  The URL of the site making the request (for OpenRouter rankings).
     * @param siteName The name of the site making the request (for OpenRouter rankings).
     * @param model    The specific model identifier to use for all calls made by this instance. Cannot be null or empty.
     * @throws NullPointerException if model is null
     * @throws IllegalArgumentException if model is empty or blank
     */
    public AICaller(String apiKey, String siteUrl, String siteName, String model) {
        this.apiKey = apiKey;
        this.siteUrl = siteUrl;
        this.siteName = siteName;
        // Ensure model is provided
        this.model = Objects.requireNonNull(model, "Model cannot be null");
        if (model.isBlank()) {
            throw new IllegalArgumentException("Model cannot be empty or blank");
        }

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .build();
        this.objectMapper = new ObjectMapper();
       // logger.info("AICaller initialized for model: " + this.model);
    }

    /**
     * Calls the configured language model with the provided prompt and retrieves the response.
     * If streaming is enabled, this method will use the streaming callback approach
     * to provide real-time updates to the caller.
     *
     * @param prompt A map containing "system" and "user" messages.
     * @param stream Whether to stream the response or not.
     * @return A ModelResponse object containing the response text, prompt tokens, and completion tokens.
     * @throws IOException          If there's an error with the HTTP request or response.
     * @throws InterruptedException If the HTTP request is interrupted.
     */
    public ModelResponse callModel(Map<String, String> prompt, boolean stream) throws IOException, InterruptedException {
        int attempt = 0;
        long currentDelay = initialRetryDelayMs;
        IOException lastException = null;

        while (attempt <= maxRetries) {
            try {
                // Original call implementation
                if (stream) {
                    StringBuffer responseBuffer = new StringBuffer();
                    Consumer<String> chunkConsumer = chunk -> {
                        responseBuffer.append(chunk);
                        System.out.print(chunk);
                        System.out.flush();
                    };

                    CompletableFuture<ModelResponse> future = callModelWithStreamingCallback(prompt, chunkConsumer);
                    return future.join();
                } else {
                    return handleNonStreamingResponse(createRequest(prompt, false), this.model);
                }
            } catch (IOException e) {
                lastException = e;
                if (attempt++ < maxRetries && isRetryable(e)) {
                    logger.warning(String.format("Attempt %d/%d failed. Retrying in %dms... (%s)",
                            attempt, maxRetries, currentDelay, e.getMessage()));
                    Thread.sleep(currentDelay);
                    currentDelay = Math.min(currentDelay * 2, maxRetryDelayMs);
                } else {
                    break;
                }
            }
        }
        throw new IOException("Failed after " + maxRetries + " retries", lastException);
    }
    /**
     * Determines if an exception is retryable based on error type and status code.
     */
    private boolean isRetryable(IOException e) {
        // Check for network-related exceptions
        if (e.getCause() instanceof java.net.ConnectException ||
                e.getCause() instanceof java.net.SocketTimeoutException) {
            return true;
        }

        // Check for retryable HTTP status codes from error message
        Matcher matcher = Pattern.compile("status code (\\d+)").matcher(e.getMessage());
        if (matcher.find()) {
            int statusCode = Integer.parseInt(matcher.group(1));
            return statusCode == 429 || (statusCode >= 500 && statusCode < 600);
        }

        return true; // Default retry for other IOExceptions
    }

    /**
     * Creates an HTTP request for the OpenRouter API.
     *
     * @param prompt A map containing "system" and "user" messages.
     * @param stream Whether to stream the response or not.
     * @return An HttpRequest object.
     */
    private HttpRequest createRequest(Map<String, String> prompt, boolean stream) {
        ObjectNode requestBody = createRequestBody(prompt);
        requestBody.put("stream", stream);

        return HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", siteUrl)
                .header("X-Title", siteName)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
    }

    /**
     * Creates the request body for the OpenRouter API using the instance's configured model.
     *
     * @param prompt A map containing "system" and "user" messages.
     * @return A JsonNode representing the request body.
     */
    private ObjectNode createRequestBody(Map<String, String> prompt) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", this.model);
        requestBody.put("temperature", 0.35);

        ArrayNode messages = objectMapper.createArrayNode();
        // Add system message if present
        if (prompt.containsKey("system") && !prompt.get("system").isEmpty()) {
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", prompt.get("system"));
            messages.add(systemMessage);
        }
        // Add user message
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt.get("user"));
        messages.add(userMessage);

        requestBody.set("messages", messages);
        return requestBody;
    }

    /**
     * Handles a non-streaming response from the OpenRouter API.
     * @param request The HTTP request to send.
     * @param expectedModel The model name expected (used for logging comparison).
     * @return A ModelResponse containing the response text and token counts.
     * @throws IOException If there's an error with the HTTP request or response.
     * @throws InterruptedException If the HTTP request is interrupted.
     */
    private ModelResponse handleNonStreamingResponse(HttpRequest request, String expectedModel) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            String errorMsg = String.format("API request failed for model %s with status code %d: %s",
                    expectedModel, response.statusCode(), response.body());
            logger.severe(errorMsg);
            throw new IOException(errorMsg);
        }

        JsonNode responseJson = objectMapper.readTree(response.body());
        String content = responseJson
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText("");
        int promptTokens = responseJson
                .path("usage")
                .path("prompt_tokens")
                .asInt(0);
        int completionTokens = responseJson
                .path("usage")
                .path("completion_tokens")
                .asInt(0);



        return new ModelResponse(content, promptTokens, completionTokens);
    }

    /**
     * Calls the configured language model with streaming and provides chunks to a consumer function.
     * Uses the model this AICaller instance was configured with.
     *
     * @param prompt        A map containing "system" and "user" messages.
     * @param chunkConsumer A consumer function that processes each chunk as it arrives.
     * @return A CompletableFuture that will complete with the final ModelResponse.
     */
    public CompletableFuture<ModelResponse> callModelWithStreamingCallback(
            Map<String, String> prompt, Consumer<String> chunkConsumer) {

        //logger.info("Calling configured model with streaming callback: " + this.model);
        String instanceModel = this.model;

        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = createRequest(prompt, true);
                StringBuilder contentBuilder = new StringBuilder();
                int[] tokenCounts = new int[2]; // [promptTokens, completionTokens]
                String[] modelUsed = new String[1];
                modelUsed[0] = instanceModel;

                HttpResponse<java.io.InputStream> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofInputStream()
                );

                if (response.statusCode() != 200) {
                    throw new IOException(String.format("API streaming callback request failed for model %s with status code %d",
                            instanceModel, response.statusCode()));
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if ("[DONE]".equals(data)) {
                                continue;
                            }
                            try {
                                JsonNode chunk = objectMapper.readTree(data);

                                // Extract model information if present
                                if (chunk.has("model")) {
                                    modelUsed[0] = chunk.path("model").asText(modelUsed[0]);
                                }

                                // Extract content delta if present
                                String contentDelta = chunk
                                        .path("choices")
                                        .path(0)
                                        .path("delta")
                                        .path("content")
                                        .asText(null);
                                if (contentDelta != null) {
                                    contentBuilder.append(contentDelta);
                                    chunkConsumer.accept(contentDelta); // Pass chunk to consumer
                                }

                                // Check for usage information
                                if (chunk.has("usage")) {
                                    tokenCounts[0] = chunk
                                            .path("usage")
                                            .path("prompt_tokens")
                                            .asInt(0);
                                    tokenCounts[1] = chunk
                                            .path("usage")
                                            .path("completion_tokens")
                                            .asInt(0);
                                }
                            } catch (Exception e) {
                                logger.warning("Error parsing streaming callback chunk: " + e.getMessage() + " | Chunk: " + data);
                            }
                        }
                    }
                }

              

                return new ModelResponse(contentBuilder.toString(), tokenCounts[0], tokenCounts[1]);

            } catch (Exception e) {
                logger.severe("Error calling model " + instanceModel + " with streaming callback: " + e.getMessage());
                throw new RuntimeException("Error calling model with streaming callback", e);
            }
        });
    }
}