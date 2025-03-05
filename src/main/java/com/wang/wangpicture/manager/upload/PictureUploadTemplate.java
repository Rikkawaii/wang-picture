package com.wang.wangpicture.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIUploadResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.wang.wangpicture.config.CosClientConfig;
import com.wang.wangpicture.exception.BusinessException;
import com.wang.wangpicture.exception.ErrorCode;
import com.wang.wangpicture.manager.CosManager;
import com.wang.wangpicture.model.dto.picture.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public abstract class PictureUploadTemplate {
    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private CosManager cosManager;

    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1. 校验图片
        String suffix = validPicture(inputSource);
        // 2. 构造图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFileName = getOriginalFileName(inputSource);
        String uploadFileName = String.format("%s_%s.%s", uuid, DateUtil.formatDate(new Date()), suffix != null ? suffix : FileUtil.getSuffix(originalFileName));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);

        File file = null;
        try {
            // 3. 创建本地临时文件
            file = File.createTempFile(uploadPath, null);
            // 4. 将inputSource对应的图片放入临时文件中
            processPicture(inputSource, file);
            // 5. 上传图片到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 6. 获取图片信息
            // 数据万象分析图片信息
            CIUploadResult ciUploadResult = putObjectResult.getCiUploadResult();
            ImageInfo imageInfo = ciUploadResult.getOriginalInfo().getImageInfo();
            // 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int width = imageInfo.getWidth();
            int height = imageInfo.getHeight();
            double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFileName));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(width);
            uploadPictureResult.setPicHeight(height);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            deleteTempFile(file);
        }
    }


    /**
     * 校验图片(如果是url上传方式，则还会返回图片的后缀，如果是文件上传方式，则返回null)
     *
     * @param inputSource
     */
    protected abstract String validPicture(Object inputSource);

    protected abstract void processPicture(Object inputSource, File file) throws IOException;

    /**
     * 获取原始文件名
     *
     * @param inputSource
     * @return
     */

    protected abstract String getOriginalFileName(Object inputSource);

    public void deletePicture(String oldUrl) {
        URL url = null; // 解析 URL
        try {
            url = new URL(oldUrl);
        } catch (MalformedURLException e) {
            log.error("图片删除失败，url解析失败", e);
            throw new RuntimeException(e);
        }
        String key = url.getPath().substring(1); // 去掉前面的 "/"
        cosManager.deletePictureObject(key); // 删除对象
    }


    private void deleteTempFile(File file) {
        if (file != null) {
            boolean del = FileUtil.del(file);
            if (!del) {
                log.error("临时文件%s删除失败", file.getAbsolutePath());
            }
        }
    }
}
