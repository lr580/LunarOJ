package com.lunaroj.auth.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[A-Za-z0-9._-]{3,64}$", message = "用户名仅支持字母数字和-_.，长度3-64")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在6-64之间")
    private String password;

    @Size(max = 64, message = "昵称最长64字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Size(max = 191, message = "邮箱最长191字符")
    private String email;

    @NotBlank(message = "captchaId 不能为空")
    private String captchaId;

    @NotBlank(message = "验证码不能为空")
    private String captchaCode;
}
