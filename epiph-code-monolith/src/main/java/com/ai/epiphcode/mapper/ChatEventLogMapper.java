package com.ai.epiphcode.mapper;

import com.ai.epiphcode.model.entity.ChatEventLog;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天事件日志 Mapper。
 */
@Mapper
public interface ChatEventLogMapper extends BaseMapper<ChatEventLog> {
}
