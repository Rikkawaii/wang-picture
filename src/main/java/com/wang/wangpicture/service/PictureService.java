package com.wang.wangpicture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wang.wangpicture.model.dto.picture.PictureQueryRequest;
import com.wang.wangpicture.model.dto.picture.PictureReviewRequest;
import com.wang.wangpicture.model.dto.picture.PictureUploadByBatchRequest;
import com.wang.wangpicture.model.dto.picture.PictureUploadRequest;
import com.wang.wangpicture.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wang.wangpicture.model.entity.User;
import com.wang.wangpicture.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author xwzy
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-02-25 22:30:20
*/
public interface PictureService extends IService<Picture> {

//    PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser);

    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    void validPicture(Picture picture);

    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    void fillReviewParams(Picture picture, User loginUser);
    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );

}
