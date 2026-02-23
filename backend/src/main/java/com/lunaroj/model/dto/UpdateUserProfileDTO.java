package com.lunaroj.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserProfileDTO {

    @NotNull(message = "个人主页内容不能为空，可传空字符串表示清空")
    @Size(max = 65535, message = "个人主页内容过长")
    private String profile;
}




