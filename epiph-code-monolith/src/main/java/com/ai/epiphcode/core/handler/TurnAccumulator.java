package com.ai.epiphcode.core.handler;

import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 单轮对话聚合上下文（流式阶段仅聚合，不落库）。
 */
@Getter
public class TurnAccumulator {

    private final long appId;

    private final long userId;

    private final String memoryId;

    private final String turnId;

    private final String codeGenType;

    private final String agent;

    private final String userMessage;

    private final StringBuilder assistantBuilder = new StringBuilder();

    private final StringBuilder thinkingBuilder = new StringBuilder();

    private final List<ToolTrace> toolTraces = new ArrayList<>();

    private Integer inputTokens;

    private Integer outputTokens;

    private Integer totalTokens;

    private boolean flushing;

    public TurnAccumulator(long appId, long userId, String memoryId, String turnId, String codeGenType,
                           String userMessage, String agent) {
        this.appId = appId;
        this.userId = userId;
        this.memoryId = memoryId;
        this.turnId = turnId;
        this.codeGenType = codeGenType;
        this.userMessage = userMessage;
        this.agent = agent;
    }

    public synchronized boolean beginFlush() {
        if (flushing) {
            return false;
        }
        flushing = true;
        return true;
    }

    public synchronized void resetFlushFlagOnFailure() {
        flushing = false;
    }

    public synchronized void appendAssistant(String chunk) {
        if (chunk != null && !chunk.isEmpty()) {
            assistantBuilder.append(chunk);
        }
    }

    public synchronized void appendThinking(String chunk) {
        if (chunk != null && !chunk.isEmpty()) {
            thinkingBuilder.append(chunk);
        }
    }

    public synchronized String assistantText() {
        return assistantBuilder.toString();
    }

    public synchronized String thinkingText() {
        return thinkingBuilder.toString();
    }

    public synchronized void addToolRequest(String toolCallId, String toolName, String toolArguments, String content, String rawEventJson) {
        toolTraces.add(ToolTrace.request(toolCallId, toolName, toolArguments, content, rawEventJson));
    }

    public synchronized void addToolResult(String toolCallId, String toolName, String toolArguments, String toolResult, String content, String rawEventJson) {
        toolTraces.add(ToolTrace.result(toolCallId, toolName, toolArguments, toolResult, content, rawEventJson));
    }

    public synchronized List<ToolTrace> copyToolTraces() {
        return new ArrayList<>(toolTraces);
    }

    public synchronized void recordTokenUsage(Integer input, Integer output, Integer total) {
        if (input == null && output == null && total == null) {
            return;
        }
        inputTokens = add(inputTokens, input);
        outputTokens = add(outputTokens, output);
        totalTokens = add(totalTokens, total);
    }

    public synchronized boolean hasTokenUsage() {
        return inputTokens != null || outputTokens != null || totalTokens != null;
    }

    public synchronized Map<String, Object> tokenUsageSnapshot() {
        Map<String, Object> usage = new LinkedHashMap<>();
        if (inputTokens != null) usage.put("inputTokens", inputTokens);
        if (outputTokens != null) usage.put("outputTokens", outputTokens);
        if (totalTokens != null) usage.put("totalTokens", totalTokens);
        if (Objects.nonNull(agent)) usage.put("agent", agent);
        return usage;
    }

    private Integer add(Integer current, Integer addition) {
        if (addition == null) return current;
        return current == null ? addition : current + addition;
    }

    @Getter
    public static class ToolTrace {

        private final String eventType;

        private final String role;

        private final String toolCallId;

        private final String toolName;

        private final String toolArguments;

        private final String toolResult;

        private final String content;

        private final String rawEventJson;

        private ToolTrace(String eventType, String role, String toolCallId, String toolName, String toolArguments,
                          String toolResult, String content, String rawEventJson) {
            this.eventType = eventType;
            this.role = role;
            this.toolCallId = toolCallId;
            this.toolName = toolName;
            this.toolArguments = toolArguments;
            this.toolResult = toolResult;
            this.content = content;
            this.rawEventJson = rawEventJson;
        }

        public static ToolTrace request(String toolCallId, String toolName, String toolArguments, String content, String rawEventJson) {
            return new ToolTrace("TOOL_REQUEST", "assistant", toolCallId, toolName, toolArguments, null, content, rawEventJson);
        }

        public static ToolTrace result(String toolCallId, String toolName, String toolArguments, String toolResult, String content, String rawEventJson) {
            return new ToolTrace("TOOL_RESULT", "tool", toolCallId, toolName, toolArguments, toolResult, content, rawEventJson);
        }
    }
}
