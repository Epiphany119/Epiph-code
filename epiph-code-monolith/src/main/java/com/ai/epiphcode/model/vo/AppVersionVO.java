package com.ai.epiphcode.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppVersionVO {
    private String id;
    private String appId;
    private Integer versionNo;
    private String parentVersionId;
    private String turnId;
    private String userPrompt;
    private String agent;
    private String codeGenType;
    private String versionType;
    private Long totalBytes;
    private String createUserId;
    private LocalDateTime createTime;
    private boolean working;
    private boolean deployed;
}
