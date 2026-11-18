package com.ai.epiphcode.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("app_version")
public class AppVersion implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("appId")
    private Long appId;

    @Column("versionNo")
    private Integer versionNo;

    @Column("parentVersionId")
    private Long parentVersionId;

    @Column("turnId")
    private String turnId;

    @Column("userPrompt")
    private String userPrompt;

    private String agent;

    @Column("codeGenType")
    private String codeGenType;

    @Column("versionType")
    private String versionType;

    @Column("fileManifest")
    private String fileManifest;

    @Column("totalBytes")
    private Long totalBytes;

    @Column("createUserId")
    private Long createUserId;

    @Column("createTime")
    private LocalDateTime createTime;
}
