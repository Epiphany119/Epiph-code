package com.ai.epiphcode.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppDeploymentHistoryVO {
    private String id;
    private String versionId;
    private Integer versionNo;
    private String deployKey;
    private String deployedBy;
    private LocalDateTime deployedTime;
}
