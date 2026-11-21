package com.ai.epiphcode.model.dto.app;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppFeaturedRequest implements Serializable {

    /**
     * 应用 id
     */
    private Long id;

    /**
     * 是否设为精选展示
     */
    private Boolean featured;

    private static final long serialVersionUID = 1L;
}
