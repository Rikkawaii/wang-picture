package com.wang.wangpicture.manager.auth.model;

import lombok.Data;

/**
 * 接收请求参数的上下文类
 */
@Data
public class SpaceUserAuthContext {
    // 临时参数，用于接收前端请求类如PictureAddRequest或SpaceAddRequest中的id参数
    // 后续会将其转成pictureId或spaceId
    private Long id;
    private Long pictureId;
    private Long spaceId;
    private Long spaceUserId;
}
