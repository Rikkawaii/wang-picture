package com.wang.wangpicture.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureEditByBatchRequest implements Serializable {
    private final static long serialVersionUID = 1L;
    private String category;
    private String nameRule;
    private List<Long> pictureIdList;
    private Long spaceId;
    private List<String> tags;
}
