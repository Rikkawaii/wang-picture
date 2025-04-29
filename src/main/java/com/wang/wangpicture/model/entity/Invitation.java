package com.wang.wangpicture.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 邀请表
 * @TableName invitation
 */
@TableName(value ="invitation")
@Data
public class Invitation implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 被邀请者邮箱
     */
    private String email;

    /**
     * 团队空间Id
     */
    private Long spaceId;

    /**
     * token(todo: 考虑加索引)
     */
    private String token;

    /**
     * 过期时间
     */
    private Date expires_at;

    /**
     * 是否被使用
     */
    private Integer used;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}