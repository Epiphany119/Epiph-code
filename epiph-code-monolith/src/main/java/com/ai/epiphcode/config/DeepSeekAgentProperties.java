package com.ai.epiphcode.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "langchain4j.deepseek")
public class DeepSeekAgentProperties {

    private String baseUrl = "https://api.deepseek.com";
    private String apiKey = "";
    private String chatModelName = "deepseek-flash";
    private String reasoningModelName = "deepseek-flash";
    private Integer maxTokens = 8192;
    private Integer reasoningMaxTokens = 32768;
    private Duration timeout = Duration.ofSeconds(600);
    private String chatThinkingType = "disabled";
    private String reasoningThinkingType = "enabled";
    private String reasoningEffort = "";
    private Boolean reasoningReturnThinking = true;
    private Boolean reasoningSendThinking = true;
    private String reasoningThinkingField = "reasoning_content";
    private Boolean logRequests = true;
    private Boolean logResponses = true;
}
