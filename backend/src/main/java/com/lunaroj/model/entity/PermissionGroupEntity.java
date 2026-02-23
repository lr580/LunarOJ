package com.lunaroj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("permission_group")
public class PermissionGroupEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;
    private String permissions;
    private String description;
    private Boolean isBuiltIn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


