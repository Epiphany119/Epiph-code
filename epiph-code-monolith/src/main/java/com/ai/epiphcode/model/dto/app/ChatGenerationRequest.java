package com.ai.epiphcode.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 新版前端 POST /app/chat、/app/chat/workflow 的对话生成请求体。
 * 与旧版 GET /app/chat/gen/code 的查询参数保持一一对应。
 */
@Data
public class ChatGenerationRequest implements Serializable {

    /** 应用 id */
    private Long appId;

    /** 用户对话内容 */
    private String message;

    /** 生成模式：classic / workflow */
    private String mode;

    /** 模型标识：default / deepseek */
    private String agent;

    /** 幂等键，防止 SSE 重连重复生成 */
    private String clientRequestId;

    /** 指定修改基线版本 */
    private Long baseVersionId;

    private static final long serialVersionUID = 1L;
}
