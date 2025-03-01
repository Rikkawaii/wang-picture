package com.wang.wangpicture.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.wang.wangpicture.exception.BusinessException;
import com.wang.wangpicture.exception.ErrorCode;
import com.wang.wangpicture.exception.ThrowUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
@Component
public class FilePictureUpload extends PictureUploadTemplate {

    @Override
    protected void processPicture(Object inputSource, File file) throws IOException {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }

    @Override
    protected String getOriginalFileName(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected String validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile == null, new BusinessException(ErrorCode.PARAMS_ERROR, "图片不能为空"));
        // 上传文件大小不得超过2M
        final long TWO_M = 2 * 1024 * 1024L;
        ThrowUtils.throwIf(multipartFile.getSize() > TWO_M, new BusinessException(ErrorCode.PARAMS_ERROR, "图片大小不能超过2M"));
        // 上传文件类型必须为图片
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final List<String> ALLOW_TYPES = Arrays.asList("jpg", "jpeg", "png", "webp", "gif", "bmp");
        ThrowUtils.throwIf(!ALLOW_TYPES.contains(suffix), new BusinessException(ErrorCode.PARAMS_ERROR, "图片格式不正确"));
        return null;
    }
}
