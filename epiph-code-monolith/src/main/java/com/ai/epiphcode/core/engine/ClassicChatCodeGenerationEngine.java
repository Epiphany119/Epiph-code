package com.ai.epiphcode.core.engine;

import com.ai.epiphcode.core.AiCodeGeneratorFacade;
import com.ai.epiphcode.core.builder.VueProjectBuilder;
import com.ai.epiphcode.exception.BusinessException;
import com.ai.epiphcode.exception.ErrorCode;
import com.ai.epiphcode.constant.AppConstant;
import com.ai.epiphcode.model.entity.User;
import com.ai.epiphcode.model.enums.ChatAgentEnum;
import com.ai.epiphcode.model.enums.ChatGenModeEnum;
import com.ai.epiphcode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * 普通模式生成引擎
 */
@Component
public class ClassicChatCodeGenerationEngine implements ChatCodeGenerationEngine {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;
    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Override
    public ChatGenModeEnum mode() {
        return ChatGenModeEnum.CLASSIC;
    }

    @Override
    public Flux<String> generate(Long appId, String message, User loginUser, CodeGenTypeEnum codeGenTypeEnum, ChatAgentEnum agent) {
        Flux<String> generated = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId, agent);
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

