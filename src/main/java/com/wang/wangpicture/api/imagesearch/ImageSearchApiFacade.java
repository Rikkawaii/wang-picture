package com.wang.wangpicture.api.imagesearch;

import com.wang.wangpicture.api.imagesearch.model.ImageSearchResult;
import com.wang.wangpicture.api.imagesearch.sub.GetImageFirstUrlApi;
import com.wang.wangpicture.api.imagesearch.sub.GetImageListApi;
import com.wang.wangpicture.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 门面模式，对外提供搜索图片的接口（一个接口调用多个子接口）
 */
@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索图片
     *
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        // 1.在浏览器通过百度识图搜索图片，查看控制台返回的json数据得到一个新的url。
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        // 2.访问新的url，得到一个html页面，解析获得一个可以返回相似图片列表的接口url。
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        // 3.访问这个接口，得到一个json数据，解析获得一个图片列表。
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://img.shetu66.com/2023/07/14/1689302077000124.png";
        List<ImageSearchResult> resultList = searchImage(imageUrl);
        System.out.println("结果列表" + resultList);
    }
}
