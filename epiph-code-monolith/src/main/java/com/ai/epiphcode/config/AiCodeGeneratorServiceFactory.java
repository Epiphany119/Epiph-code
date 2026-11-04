package com.ai.epiphcode.config;

import com.ai.epiphcode.ai.AiCodeGeneratorService;
import com.ai.epiphcode.ai.guardrail.PromptSafetyInputGuardrail;
import com.ai.epiphcode.ai.guardrail.RetryOutputGuardrail;
import com.ai.epiphcode.ai.tools.*;
import com.ai.epiphcode.exception.BusinessException;
import com.ai.epiphcode.exception.ErrorCode;
import com.ai.epiphcode.model.enums.ChatAgentEnum;
import com.ai.epiphcode.model.enums.CodeGenTypeEnum;
import com.ai.epiphcode.service.ChatMemoryReplayService;
import com.ai.epiphcode.service.ChatHistoryService;
import com.ai.epiphcode.service.ContextCompactionService;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {
    /**
     * Vue 工作流保留的最近消息窗口，避免超长历史污染当前任务。
     */
    private static final int VUE_PROJECT_MAX_MESSAGES = 160;

    /**
     * Redis 记忆缺失时，从事件日志回放的上限，控制上下文噪声。
     */
    private static final int VUE_PROJECT_REPLAY_MAX_EVENTS = 300;

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel openAiStreamingChatModel;

    @Resource
    private StreamingChatModel reasoningStreamingChatModel;

    @Resource
    private DeepSeekModelFactory deepSeekModelFactory;

    @Resource
    private ChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private ChatMemoryReplayService chatMemoryReplayService;
    @Resource
    private ContextCompactionService contextCompactionService;
    @Autowired
    private ToolManager toolManager;


//    /**
//     * 根据 appId 获取服务
//     */
//    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
//        // 根据 appId 构建独立的对话记忆
//        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
//                .builder()
//                .id(appId)
//                .chatMemoryStore(redisChatMemoryStore)
//                .maxMessages(20)
//                .build();
//        return AiServices.builder(AiCodeGeneratorService.class)
//                .chatModel(chatModel)
//                .streamingChatModel(streamingChatModel)
//                .chatMemory(chatMemory)
//                .build();
//    }

    /**
     * 默认提供一个 Bean
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0L, CodeGenTypeEnum.HTML);
    }

    /**
     * 方案A（无状态服务）：
     * 不缓存 AiCodeGeneratorService 实例，避免本地实例状态与 Redis 会话状态失配。
     * 每次请求按 appId + codeGenType 动态创建服务，并动态装配会话记忆。
     */

//    /**
//     * 根据 appId 获取服务（带缓存）
//     */
//    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
//        return serviceCache.get(appId, this::createAiCodeGeneratorService);
//    }

//    /**
//     * 创建新的 AI 服务实例
//     */
//    private AiCodeGeneratorService createAiCodeGeneratorService(long appId) {
//        log.info("为 appId: {} 创建新的 AI 服务实例", appId);
//        // 根据 appId 构建独立的对话记忆
//        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
//                .builder()
//                .id(appId)
//                .chatMemoryStore(redisChatMemoryStore)
//                .maxMessages(20)
//                .build();
//        return AiServices.builder(AiCodeGeneratorService.class)
//                .chatModel(chatModel)
//                .streamingChatModel(streamingChatModel)
//                .chatMemory(chatMemory)
//                .build();
//    }
//    private AiCodeGeneratorService createAiCodeGeneratorService(long appId) {
//        log.info("为 appId: {} 创建新的 AI 服务实例", appId);
//        // 根据 appId 构建独立的对话记忆
//        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
//                .builder()
//                .id(appId)
//                .chatMemoryStore(redisChatMemoryStore)
//                .maxMessages(20)
//                .build();
//        // 从数据库加载历史对话到记忆中
//        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
//        return AiServices.builder(AiCodeGeneratorService.class)
//                .chatModel(chatModel)
//                .streamingChatModel(openAiStreamingChatModel)
//                .chatMemory(chatMemory)
//                .build();
//    }

    /**
     * 创建新的 AI 服务实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(
            long appId, CodeGenTypeEnum codeGenType, ChatAgentEnum agent) {
        if (agent == null) {
            agent = ChatAgentEnum.DEFAULT;
        }
        String memoryId = appId + "_" + codeGenType.getValue();
        int maxMessages = codeGenType == CodeGenTypeEnum.VUE_PROJECT ? VUE_PROJECT_MAX_MESSAGES : 20;
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(memoryId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(maxMessages)
                .build();

        return switch (codeGenType) {
            case VUE_PROJECT -> {
                List<?> cachedMessages = redisChatMemoryStore.getMessages(memoryId);
                if (cachedMessages == null || cachedMessages.isEmpty()) {
                    int replayed = chatMemoryReplayService.rebuildFromEventLog(
                            memoryId, chatMemory, VUE_PROJECT_REPLAY_MAX_EVENTS);
                    log.info("VUE_PROJECT memory 回放完成，memoryId={}, replayed={}", memoryId, replayed);
                }
                compactMemoryBeforeModelCall(memoryId);
                contextCompactionService.autoCompactIfNeeded(chatMemory, memoryId);
                StreamingChatModel selectedReasoningModel = agent == ChatAgentEnum.DEEPSEEK
                        ? deepSeekModelFactory.createReasoningStreamingChatModel()
                        : reasoningStreamingChatModel;
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(selectedReasoningModel)
                        .chatMemoryProvider(ignoredMemoryId -> chatMemory)
                        .tools(toolManager.getAllTools())
                        .inputGuardrails(new PromptSafetyInputGuardrail())
                        .outputGuardrails(new RetryOutputGuardrail())
                        .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                                toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                        ))
                        .build();
            }
            case HTML, MULTI_FILE -> {
                chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
                ChatModel selectedChatModel = agent == ChatAgentEnum.DEEPSEEK
                        ? deepSeekModelFactory.createChatModel()
                        : chatModel;
                StreamingChatModel selectedStreamingModel = agent == ChatAgentEnum.DEEPSEEK
                        ? deepSeekModelFactory.createChatStreamingModel()
                        : openAiStreamingChatModel;
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(selectedChatModel)
                        .streamingChatModel(selectedStreamingModel)
                        .chatMemory(chatMemory)
                        .build();
            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }

    /**
     * 根据 appId 获取服务（兼容历史逻辑）
     */
    @Deprecated
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        throw new BusinessException(
                ErrorCode.SYSTEM_ERROR,
                "请使用 getAiCodeGeneratorService(appId, codeGenType) 显式传入代码生成类型，避免默认 HTML 回落"
        );
    }

    /**
     * 根据 appId 和代码生成类型获取服务（无状态：每次动态创建）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        return getAiCodeGeneratorService(appId, codeGenType, ChatAgentEnum.DEFAULT);
    }

    public AiCodeGeneratorService getAiCodeGeneratorService(
            long appId, CodeGenTypeEnum codeGenType, ChatAgentEnum agent) {
        return createAiCodeGeneratorService(appId, codeGenType, agent);
    }

    private void compactMemoryBeforeModelCall(String memoryId) {
        if (redisChatMemoryStore instanceof CompactingChatMemoryStore compactingChatMemoryStore) {
            compactingChatMemoryStore.compactAndPersist(memoryId);
        }
    }

}
