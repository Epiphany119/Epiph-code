package com.ai.epiphcode.model.enums;

/**
 * 用户可选择的主代码生成 Agent。
 */
public enum ChatAgentEnum {

    DEFAULT("default", "默认 Agent"),
    DEEPSEEK("deepseek", "DeepSeek Flash");

    private final String value;
    private final String text;

    ChatAgentEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    public static ChatAgentEnum getByValue(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        for (ChatAgentEnum agent : values()) {
            if (agent.value.equalsIgnoreCase(value)) {
                return agent;
            }
        }
        return null;
    }
}
