package com.wang.wangpicture.constant;

/**
 * cos提供的缩略图参数
 */
public interface ThumbnailUrlParams {
    String THUMBNAIL_PREFIX = "?imageMogr2";
    // 缩放原图尺寸为256x256
    String THUMBNAIL_ZOOM_256x256 = "/thumbnail/256x256";
    // 缩放原图尺寸为128x128
    String THUMBNAIL_ZOOM_128x128 = "/thumbnail/128x128";
    // 格式变换为webp
    String THUMBNAIL_FORMAT_WEBP = "imageMogr2/format/webp";
}
