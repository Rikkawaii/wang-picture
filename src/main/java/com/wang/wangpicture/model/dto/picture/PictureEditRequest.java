package com.wang.wangpicture.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
@Data
public class PictureEditRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 图片id
     */
    private Long id;
    /**
     * 图片名称
     */
    private String name;
    /**
     * 图片简介
     */
    private String introduction;
    /**
     * 图片标签
     */
    private List<String> tags;
    /**
     * 图片分类
     */
    private String category;

}
