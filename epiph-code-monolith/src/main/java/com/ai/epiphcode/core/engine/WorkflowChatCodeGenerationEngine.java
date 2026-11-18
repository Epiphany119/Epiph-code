package com.ai.epiphcode.core.engine;

import com.ai.epiphcode.constant.AppConstant;
import com.ai.epiphcode.core.builder.VueProjectBuilder;
import com.ai.epiphcode.langgraph4j.CodeGenWorkflow;
import com.ai.epiphcode.exception.BusinessException;
import com.ai.epiphcode.exception.ErrorCode;
import com.ai.epiphcode.model.entity.User;
import com.ai.epiphcode.model.enums.ChatAgentEnum;
import com.ai.epiphcode.model.enums.ChatGenModeEnum;
import com.ai.epiphcode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * 工作流模式生成引擎
 */
@Component
public class WorkflowChatCodeGenerationEngine implements ChatCodeGenerationEngine {

    @Resource
    private CodeGenWorkflow codeGenWorkflow;
    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Override
    public ChatGenModeEnum mode() {
        return ChatGenModeEnum.WORKFLOW;
    }

    @Override
    public Flux<String> generate(Long appId, String message, User loginUser, CodeGenTypeEnum codeGenTypeEnum,
                                 ChatAgentEnum agent, String turnId) {
        Flux<String> generated = codeGenWorkflow.executeWorkflowForChat(message, appId, codeGenTypeEnum, agent, turnId);
        if (codeGenTypeEnum != CodeGenTypeEnum.VUE_PROJECT) {
            return generated;
        }
        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "vue_project_" + appId;
        return generated.concatWith(Flux.defer(() -> {
            if (!vueProjectBuilder.buildProject(projectPath)) {
                return Flux.error(new BusinessException(ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败"));
            }
            return Flux.empty();
        }));
    }
}

