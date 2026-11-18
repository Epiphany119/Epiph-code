package com.ai.epiphcode.model.vo;

import lombok.Data;

@Data
public class AppVersionFileChangeVO {
    private String path;
    private String status;
    private Long oldBytes;
    private Long newBytes;
    private String unifiedDiff;
}
