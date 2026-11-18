package com.ai.epiphcode.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ai.epiphcode.core.handler.TurnAccumulator;
import com.ai.epiphcode.core.handler.TurnAccumulatorManager;
import com.ai.epiphcode.model.entity.ChatEventLog;
import com.ai.epiphcode.model.entity.AppVersion;
import com.ai.epiphcode.model.enums.ChatEventTypeEnum;
import com.ai.epiphcode.model.enums.ChatHistoryMessageTypeEnum;
import com.ai.epiphcode.service.ChatEventLogService;
import com.ai.epiphcode.service.ChatHistoryService;
import com.ai.epiphcode.service.impl.AppVersionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单轮对话聚合结果落库服务。
 */
@Service
@Slf4j
public class TurnFlushService {

    @Resource
    private TurnAccumulatorManager turnAccumulatorManager;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ChatEventLogService chatEventLogService;

    @Resource
    private AppVersionService appVersionService;

    @Transactional(rollbackFor = Exception.class)
    public void flushSuccess(String turnId) {
        TurnAccumulator accumulator = turnAccumulatorManager.get(turnId);
        if (accumulator == null) {
            return;
        }
        if (!accumulator.beginFlush()) {
            return;
        }
        try {
            AppVersion version = appVersionService.commitTurn(accumulator);
            flushTransactional(accumulator, null, version);
            turnAccumulatorManager.remove(turnId);
        } catch (Exception e) {
            try {
                appVersionService.rollbackTurn(accumulator);
            } catch (RuntimeException rollbackError) {
                e.addSuppressed(rollbackError);
                log.error("轮次落库失败后恢复网页工作区失败 turnId={}", turnId, rollbackError);
            }
            accumulator.resetFlushFlagOnFailure();
            throw e;
        } finally {
            appVersionService.finishTurn(accumulator);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void flushError(String turnId, String errorMessage) {
        TurnAccumulator accumulator = turnAccumulatorManager.get(turnId);
        if (accumulator == null) {
            return;
        }
        if (!accumulator.beginFlush()) {
            return;
        }
        try {
            appVersionService.rollbackTurn(accumulator);
            flushTransactional(accumulator, StrUtil.blankToDefault(errorMessage, "unknown error"), null);
            turnAccumulatorManager.remove(turnId);
        } catch (Exception e) {
            accumulator.resetFlushFlagOnFailure();
            throw e;
        } finally {
            appVersionService.finishTurn(accumulator);
        }
    }

    private void flushTransactional(TurnAccumulator accumulator, String errorMessage, AppVersion version) {
        String turnId = accumulator.getTurnId();
        String thinking = StrUtil.emptyToNull(accumulator.thinkingText());
        String assistantText = accumulator.assistantText();
        boolean failed = StrUtil.isNotBlank(errorMessage);
        String finalAssistantText;
        if (failed) {
            finalAssistantText = StrUtil.isBlank(assistantText)
                    ? "AI回复失败: " + errorMessage
                    : assistantText + "\n\nAI回复失败: " + errorMessage;
        } else {
            finalAssistantText = assistantText;
        }

        // chat_history：每轮仅写 user + ai 两条
        chatHistoryService.addChatMessage(
                accumulator.getAppId(),
                accumulator.getUserMessage(),
                ChatHistoryMessageTypeEnum.USER.getValue(),
                accumulator.getUserId(),
                turnId
        );
        chatHistoryService.addChatMessage(
                accumulator.getAppId(),
                StrUtil.blankToDefault(finalAssistantText, ""),
                ChatHistoryMessageTypeEnum.AI.getValue(),
                accumulator.getUserId(),
                thinking,
                turnId
        );

        // chat_event_log：每轮批量写关键事件（不写 token 级分片）
        List<ChatEventLog> eventLogs = new ArrayList<>();
        AtomicInteger seq = new AtomicInteger(0);

        eventLogs.add(ChatEventLog.builder()
                .appId(accumulator.getAppId())
                .memoryId(accumulator.getMemoryId())
                .turnId(turnId)
                .seq(seq.getAndIncrement())
                .codeGenType(accumulator.getCodeGenType())
                .role("user")
                .eventType(ChatEventTypeEnum.USER_MESSAGE.getValue())
                .content(accumulator.getUserMessage())
                .userId(accumulator.getUserId())
                .build());

        if (StrUtil.isNotBlank(thinking)) {
            eventLogs.add(ChatEventLog.builder()
                    .appId(accumulator.getAppId())
                    .memoryId(accumulator.getMemoryId())
                    .turnId(turnId)
                    .seq(seq.getAndIncrement())
                    .codeGenType(accumulator.getCodeGenType())
                    .role("assistant")
                    .eventType(ChatEventTypeEnum.THINKING_FINAL.getValue())
                    .reasoningContent(thinking)
                    .userId(accumulator.getUserId())
                    .build());
        }

    
        for (TurnAccumulator.ToolTrace toolTrace : accumulator.copyToolTraces()) {
            eventLogs.add(ChatEventLog.builder()
                    .appId(accumulator.getAppId())
                    .memoryId(accumulator.getMemoryId())
                    .turnId(turnId)
                    .seq(seq.getAndIncrement())
                    .codeGenType(accumulator.getCodeGenType())
                    .role(toolTrace.getRole())
                    .eventType(toolTrace.getEventType())
                    .toolCallId(toolTrace.getToolCallId())
                    .toolName(toolTrace.getToolName())
                    .toolArguments(toolTrace.getToolArguments())
                    .toolResult(toolTrace.getToolResult())
                    .content(toolTrace.getContent())
                    .rawEventJson(toolTrace.getRawEventJson())
                    .userId(accumulator.getUserId())
                    .build());
        }

        eventLogs.add(ChatEventLog.builder()
                .appId(accumulator.getAppId())
                .memoryId(accumulator.getMemoryId())
                .turnId(turnId)
                .seq(seq.getAndIncrement())
                .codeGenType(accumulator.getCodeGenType())
                .role("assistant")
                .eventType(failed ? ChatEventTypeEnum.ERROR.getValue() : ChatEventTypeEnum.ASSISTANT_FINAL.getValue())
                .content(StrUtil.blankToDefault(finalAssistantText, ""))
                .reasoningContent(thinking)
                .rawEventJson(finalEventMetadata(accumulator, errorMessage, version))
                .userId(accumulator.getUserId())
                .build());

        boolean saved = chatEventLogService.saveBatch(eventLogs);
        if (!saved) {
            throw new RuntimeException("批量写入 chat_event_log 失败，turnId=" + turnId);
        }
        log.info("轮次聚合落库完成: turnId={}, eventCount={}, failed={}", turnId, eventLogs.size(), failed);
    }

    private String finalEventMetadata(TurnAccumulator accumulator, String errorMessage, AppVersion version) {
        if (StrUtil.isNotBlank(errorMessage)) return errorMessage;
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (accumulator.hasTokenUsage()) metadata.putAll(accumulator.tokenUsageSnapshot());
        if (version != null) {
            metadata.put("versionId", version.getId());
            metadata.put("versionNo", version.getVersionNo());
            metadata.put("parentVersionId", version.getParentVersionId());
        }
        return metadata.isEmpty() ? null : JSONUtil.toJsonStr(metadata);
    }
}
