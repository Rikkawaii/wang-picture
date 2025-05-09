package com.wang.wangpicture.model.vo;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.wang.wangpicture.model.entity.Picture;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
@Data
public class PictureVO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;

    /**
     * 图片 url
     */
    private String url;
    /**
     * 缩略图 url
     */
    private String thumbnailUrl;
    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON 数组）
     */
    private List<String> tags;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 创建用户 id
     */
    private Long userId;
    /**
     * 空间 id
     */
    private Long spaceId;
    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     *  创建用户信息
     */
    private UserVO user;

    /**
     * 权限列表
     */
    private List<String> permissionList = new ArrayList<>();

    /**
     * 封装类转对象
     */
    public static Picture voToObj(PictureVO pictureVO) {
        if (pictureVO == null) {
            return null;
        }
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureVO, picture);
        // 类型不同，需要转换
//        try {
//            picture.setTags(new ObjectMapper().writeValueAsString(pictureVO.getTags()));
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
        picture.setTags(JSON.toJSONString(pictureVO.getTags()));
        return picture;
    }

    /**
     * 对象转封装类
     */
    public static PictureVO objToVo(Picture picture) {
        if (picture == null) {
            return null;
        }
        PictureVO pictureVO = new PictureVO();
        BeanUtils.copyProperties(picture, pictureVO);
//        ObjectMapper objectMapper = new ObjectMapper();
//        List<String> tags = null;
//        try {
//            tags = objectMapper.readValue(picture.getTags(), new TypeReference<List<String>>() {});
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//        // 类型不同，需要转换
//        pictureVO.setTags(tags);

        pictureVO.setTags(JSONUtil.toList(picture.getTags(), String.class));
        return pictureVO;
    }
}
