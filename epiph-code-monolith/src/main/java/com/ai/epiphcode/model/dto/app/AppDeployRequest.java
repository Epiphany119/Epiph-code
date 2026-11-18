package com.ai.epiphcode.model.dto.app;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppDeployRequest implements Serializable {

    /**
     * 应用 id
     */
    private Long appId;

    /** 目标代码版本；为空时使用当前工作版本。 */
    private Long versionId;

    private static final long serialVersionUID = 1L;
}
