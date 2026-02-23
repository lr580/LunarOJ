package com.lunaroj.model.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserBasicDTO {

    @Size(max = 64, message = "昵称最长64字符")
    private String nickname;

    @Pattern(
            regexp = "^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "邮箱格式不正确"
    )
    @Size(max = 191, message = "邮箱最长191字符")
    private String email;

    private Boolean defaultCodePublic;
}




