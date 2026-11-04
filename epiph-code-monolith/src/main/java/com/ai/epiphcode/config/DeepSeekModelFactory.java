package com.ai.epiphcode.config;

import com.ai.epiphcode.exception.BusinessException;
import com.ai.epiphcode.exception.ErrorCode;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;

@Component
public class DeepSeekModelFactory {

    private final DeepSeekAgentProperties properties;

    public DeepSeekModelFactory(DeepSeekAgentProperties properties) {
        this.properties = properties;
    }

    public ChatModel createChatModel() {
        requireApiKey();
        return OpenAiChatModel.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .modelName(properties.getChatModelName())
                .maxTokens(properties.getMaxTokens())
                .timeout(Duration.ofSeconds(120))
                .maxRetries(2)
                .customParameters(thinkingParameters(properties.getChatThinkingType()))
                .customHeaders(Map.of("Accept-Encoding", "identity"))
                .logRequests(Boolean.TRUE.equals(properties.getLogRequests()))
                .logResponses(Boolean.TRUE.equals(properties.getLogResponses()))
                .build();
    }

    public StreamingChatModel createChatStreamingModel() {
        return createStreamingModel(false);
    }

    public StreamingChatModel createReasoningStreamingChatModel() {
        return createStreamingModel(true);
    }

    private StreamingChatModel createStreamingModel(boolean reasoning) {
        requireApiKey();
        String modelName = reasoning
                ? properties.getReasoningModelName()
                : properties.getChatModelName();
        Integer maxTokens = reasoning
                ? properties.getReasoningMaxTokens()
                : properties.getMaxTokens();
        String thinkingType = reasoning
                ? properties.getReasoningThinkingType()
                : properties.getChatThinkingType();

        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder =
                OpenAiStreamingChatModel.builder()
                        .apiKey(properties.getApiKey())
                        .baseUrl(properties.getBaseUrl())
                        .modelName(modelName)
                        .maxTokens(maxTokens)
                        .timeout(properties.getTimeout())
                        .strictJsonSchema(true)
                        .strictTools(true)
                        .parallelToolCalls(false)
                        .returnThinking(reasoning && Boolean.TRUE.equals(properties.getReasoningReturnThinking()))
                        .sendThinking(reasoning && Boolean.TRUE.equals(properties.getReasoningSendThinking()),
                                properties.getReasoningThinkingField())
                        .customParameters(thinkingParameters(thinkingType))
                        .customHeaders(Map.of("Accept-Encoding", "identity"))
                        .logRequests(Boolean.TRUE.equals(properties.getLogRequests()))
                        .logResponses(Boolean.TRUE.equals(properties.getLogResponses()));

        if (reasoning && StringUtils.hasText(properties.getReasoningEffort())) {
            builder.reasoningEffort(properties.getReasoningEffort());
        }
        return builder.build();
    }

    private Map<String, Object> thinkingParameters(String thinkingType) {
        if (!StringUtils.hasText(thinkingType)) {
            return Map.of();
        }
        return Map.of("thinking", Map.of("type", thinkingType));
    }

    private void requireApiKey() {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new BusinessException(
                    ErrorCode.SYSTEM_ERROR,
                    "DeepSeek Agent 未配置 API Key，请填写 EPIPH_CODE_DEEPSEEK_API_KEY"
            );
        }
    }
}
