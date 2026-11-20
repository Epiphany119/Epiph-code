package com.ai.epiphcode.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserResetPasswordRequest implements Serializable {

    /**
     * 用户 id
     */
    private Long id;

    /**
     * 新密码（可选，为空时重置为默认密码 12345678）
     */
    private String newPassword;

    private static final long serialVersionUID = 1L;
}
