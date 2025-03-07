package com.wang.wangpicture.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
@Data
public class DeleteByBatchRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long spaceId;
    private List<Long> pictureIds;
}
