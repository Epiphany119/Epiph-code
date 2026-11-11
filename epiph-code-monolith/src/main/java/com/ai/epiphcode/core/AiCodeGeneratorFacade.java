package com.ai.epiphcode.core;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ai.epiphcode.ai.AiCodeGeneratorService;
import com.ai.epiphcode.ai.model.HtmlCodeResult;
import com.ai.epiphcode.ai.model.MultiFileCodeResult;
import com.ai.epiphcode.ai.model.message.AiResponseMessage;
import com.ai.epiphcode.ai.model.message.ThinkingMessage;
import com.ai.epiphcode.ai.model.message.ToolExecutedMessage;
import com.ai.epiphcode.ai.model.message.ToolRequestMessage;
import com.ai.epiphcode.config.AiCodeGeneratorServiceFactory;
import com.ai.epiphcode.core.parser.CodeParser;
import com.ai.epiphcode.core.parser.CodeParserExecutor;
import com.ai.epiphcode.core.saver.CodeFileSaverExecutor;
import com.ai.epiphcode.core.handler.TurnAccumulator;
import com.ai.epiphcode.core.handler.TurnAccumulatorManager;
import com.ai.epiphcode.exception.BusinessException;
import com.ai.epiphcode.exception.ErrorCode;
import com.ai.epiphcode.model.enums.ChatAgentEnum;
import com.ai.epiphcode.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;


import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

/*    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;*/

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private TurnAccumulatorManager turnAccumulatorManager;



    /**
     * 统一入口：根据类型生成并保存代码（使用 appId）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {

        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        // 根据 appId 获取对应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService =
                aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式，使用 appId）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 ID
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        return generateAndSaveCodeStream(userMessage, codeGenTypeEnum, appId, ChatAgentEnum.DEFAULT);
    }

    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum,
                                                  Long appId, ChatAgentEnum agent) {
        return generateAndSaveCodeStream(userMessage, codeGenTypeEnum, appId, agent, null);
    }

    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum,
                                                  Long appId, ChatAgentEnum agent, String turnId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        // 根据 appId 获取对应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum, agent);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                TokenStream codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeTokenStream(codeStream, CodeGenTypeEnum.HTML, appId, turnId);
            }
            case MULTI_FILE -> {
                TokenStream codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeTokenStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId, turnId);
            }
            case VUE_PROJECT -> {
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(tokenStream, appId, turnId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };

    }

    /**
     * 通用流式代码处理方法（使用 appId）
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @param appId       应用 ID
     * @return 流式响应
     */
    private Flux<String> processCodeTokenStream(TokenStream tokenStream, CodeGenTypeEnum codeGenType,
                                                Long appId, String turnId) {
        return Flux.create(sink -> {
            StringBuilder codeBuilder = new StringBuilder();
            TokenStream configuredStream = tokenStream
                    .onPartialResponse((String partialResponse) -> {
                        codeBuilder.append(partialResponse);
                        sink.next(partialResponse);
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        recordTokenUsage(response, turnId);
                        try {
                            if (codeBuilder.length() == 0
                                    && response != null
                                    && response.aiMessage() != null
                                    && StrUtil.isNotBlank(response.aiMessage().text())) {
                                String finalText = response.aiMessage().text();
                                codeBuilder.append(finalText);
                                sink.next(finalText);
                            }
                            Object parsedResult = CodeParserExecutor.executeParser(codeBuilder.toString(), codeGenType);
                            File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                            log.info("代码生成保存成功，appId={}, path={}", appId, savedDir.getAbsolutePath());
                            sink.complete();
                        } catch (Exception e) {
                            log.error("生成代码解析或保存失败", e);
                            sink.error(new BusinessException(ErrorCode.SYSTEM_ERROR, "代码解析或保存失败，请查看后端日志"));
                        }
                    })
                    .onError(sink::error);
            configuredStream.start();
        });
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, Long appId, String turnId) {
        return Flux.create(sink -> {
            AtomicInteger emittedChunkCount = new AtomicInteger(0);
            TokenStream configuredStream = tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                        emittedChunkCount.incrementAndGet();
                    })
                    .beforeToolExecution(beforeToolExecution -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(beforeToolExecution);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                        emittedChunkCount.incrementAndGet();
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                        emittedChunkCount.incrementAndGet();
                    })
//                    .onCompleteResponse((ChatResponse response) -> {
//                        sink.complete();
//                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        recordTokenUsage(response, turnId);
                        if (emittedChunkCount.get() == 0
                                && response != null
                                && response.aiMessage() != null
                                && StrUtil.isNotBlank(response.aiMessage().text())) {
                            // 兜底：某些模型实现可能只触发 complete，不触发 partial 回调
                            sink.next(JSONUtil.toJsonStr(new AiResponseMessage(response.aiMessage().text())));
                            emittedChunkCount.incrementAndGet();
                        }
                        log.info("TokenStream 完成，appId={}, emittedChunkCount={}", appId, emittedChunkCount.get());
                        sink.complete();
                    })

                    .onError((Throwable error) -> {
                        sink.error(error);
                    });
            // 仅当底层模型/实现支持时，才会接收到思考分片
            try {
                configuredStream = configuredStream.onPartialThinking((PartialThinking partialThinking) -> {
                    ThinkingMessage thinkingMessage = new ThinkingMessage(partialThinking);
                    sink.next(JSONUtil.toJsonStr(thinkingMessage));
                    emittedChunkCount.incrementAndGet();
                });
            } catch (UnsupportedOperationException e) {
                log.debug("当前 TokenStream 实现不支持 onPartialThinking，已降级为普通流式输出");
            }
            configuredStream.start();
        });
    }

    private void recordTokenUsage(ChatResponse response, String turnId) {
        if (response == null || response.tokenUsage() == null || turnId == null) {
            return;
        }
        TurnAccumulator accumulator = turnAccumulatorManager.get(turnId);
        if (accumulator == null) {
            return;
        }
        var tokenUsage = response.tokenUsage();
        accumulator.recordTokenUsage(
                tokenUsage.inputTokenCount(),
                tokenUsage.outputTokenCount(),
                tokenUsage.totalTokenCount()
        );
    }





}
