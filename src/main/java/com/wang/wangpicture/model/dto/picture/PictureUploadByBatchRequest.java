package com.wang.wangpicture.model.dto.picture;

import lombok.Data;

@Data
public class PictureUploadByBatchRequest {  
  
    /**  
     * 搜索词  
     */  
    private String searchText;
    /**
     * 图片名前缀（如果没有设置，默认同搜索词）
     */
    private String namePrefix;

    /**  
     * 抓取数量  
     */  
    private Integer count = 10;


}
