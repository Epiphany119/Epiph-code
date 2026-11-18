-- 为已有数据库启用网页版本管理（每个数据库只执行一次）
use epiph_code;

alter table app
    add column workingVersionId bigint null comment '当前工作版本 id' after deployedTime,
    add column deployedVersionId bigint null comment '当前已部署版本 id' after workingVersionId;

create table if not exists app_version
(
    id              bigint       not null primary key comment '版本雪花 id',
    appId           bigint       not null comment '应用 id',
    versionNo       int          not null comment '应用内递增版本号',
    parentVersionId bigint       null comment '父版本 id',
    turnId          varchar(64)  null comment '关联对话轮次',
    userPrompt      longtext     null comment '本轮用户提示词或历史基线说明',
    agent           varchar(64)  null comment '生成 Agent',
    codeGenType     varchar(32)  not null comment '代码类型',
    versionType     varchar(32)  not null comment 'GENERATED/LEGACY_BASELINE/LEGACY_DEPLOYED',
    fileManifest    longtext     not null comment '相对路径到 SHA-256 的 JSON 清单',
    totalBytes      bigint       not null default 0 comment '快照文件总大小',
    createUserId    bigint       null comment '创建用户 id',
    createTime      datetime     not null default current_timestamp comment '版本创建时间',
    unique key uk_appVersionNo (appId, versionNo),
    index idx_appVersionTurn (appId, turnId),
    index idx_appVersionParent (parentVersionId)
) comment '网页代码版本' collate = utf8mb4_unicode_ci;

create table if not exists app_deployment_history
(
    id           bigint      not null primary key comment '部署记录雪花 id',
    appId        bigint      not null comment '应用 id',
    versionId    bigint      not null comment '发布的网页版本 id',
    deployKey    varchar(64) not null comment '稳定部署地址标识',
    deployedBy   bigint      not null comment '部署用户 id',
    deployedTime datetime    not null comment '部署时间',
    index idx_appDeploymentTime (appId, deployedTime),
    index idx_appDeploymentVersion (versionId)
) comment '应用部署历史' collate = utf8mb4_unicode_ci;
