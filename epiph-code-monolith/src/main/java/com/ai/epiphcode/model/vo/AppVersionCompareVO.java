package com.ai.epiphcode.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class AppVersionCompareVO {
    private String leftVersionId;
    private String rightVersionId;
    private List<AppVersionFileChangeVO> files;
}
