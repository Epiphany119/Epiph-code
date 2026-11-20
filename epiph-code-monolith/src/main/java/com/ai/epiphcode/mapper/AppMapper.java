package com.ai.epiphcode.mapper;

import com.ai.epiphcode.model.entity.App;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 应用 映射层。
 *
 * @author Luo QinFeng
 */
public interface AppMapper extends BaseMapper<App> {

    /**
     * 查询已逻辑删除且超过保留期限的应用（绕过逻辑删除拦截）
     */
    @Select("SELECT * FROM app WHERE isDelete = 1 AND updateTime <= #{cutoffTime}")
    List<App> selectExpiredDeletedApps(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Delete("DELETE FROM chat_history WHERE appId = #{appId}")
    int physicalDeleteChatHistoryByAppId(@Param("appId") Long appId);

    @Delete("DELETE FROM chat_event_log WHERE appId = #{appId}")
    int physicalDeleteChatEventLogByAppId(@Param("appId") Long appId);

    @Delete("DELETE FROM app_version WHERE appId = #{appId}")
    int physicalDeleteAppVersionsByAppId(@Param("appId") Long appId);

    @Delete("DELETE FROM app_deployment_history WHERE appId = #{appId}")
    int physicalDeleteDeploymentHistoryByAppId(@Param("appId") Long appId);

    @Delete("DELETE FROM app WHERE id = #{appId} AND isDelete = 1")
    int physicalDeleteAppById(@Param("appId") Long appId);
}
