package com.wang.wangpicture.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureSearchRequest implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    private static final long serialVersionUID = 1L;
}
