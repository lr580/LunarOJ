package com.lunaroj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("system_config")
public class SystemConfigEntity {

    @TableId(value = "config_key", type = IdType.INPUT)
    private String configKey;

    private String configValue;
    private String description;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}


