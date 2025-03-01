//package com.wang.wangpicture.manager;
//
//import cn.hutool.core.date.DateUtil;
//import cn.hutool.core.io.FileUtil;
//import cn.hutool.core.util.NumberUtil;
//import cn.hutool.core.util.RandomUtil;
//import com.qcloud.cos.model.PutObjectResult;
//import com.qcloud.cos.model.ciModel.persistence.CIUploadResult;
//import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
//import com.wang.wangpicture.config.CosClientConfig;
//import com.wang.wangpicture.exception.BusinessException;
//import com.wang.wangpicture.exception.ErrorCode;
//import com.wang.wangpicture.exception.ThrowUtils;
//import com.wang.wangpicture.model.dto.picture.UploadPictureResult;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import org.springframework.web.multipart.MultipartFile;
//
//import javax.annotation.Resource;
//import java.io.File;
//import java.util.Arrays;
//import java.util.Date;
//import java.util.List;
//@Deprecated
//@Slf4j
//@Component
//public class FileManager {
//    @Resource
//    private CosClientConfig cosClientConfig;
//    @Resource
//    private CosManager cosManager;
//
//    /**
//     *
//     * @param multipartFile
//     * @param uploadPathPrefix 如果是新增图片，则为上传路径前缀（/public/loginUserId），如果是更新图片，则为图片的完整路径
//     * @return
//     */
//    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
//        // 校验图片
//        validPicture(multipartFile);
//        // 更新上传方式其一
////        String originalFileName = multipartFile.getOriginalFilename();
////        String uploadPath = null;
////        // 构建图片上传地址
////        if(isAdd) {
////            // 新增图片
////            String uuid = RandomUtil.randomString(16);
////            String uploadFileName = String.format("%s_%s.%s", uuid, DateUtil.formatDate(new Date()), FileUtil.getSuffix(originalFileName));
////            uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
////        }else{
////            // 更新图片
////            int publicIndex = uploadPathPrefix.indexOf("/public");
////            uploadPath = uploadPathPrefix.substring(publicIndex);
////        }
//        // 更新上传方式其二
//        String uuid = RandomUtil.randomString(16);
//        String originalFileName = multipartFile.getOriginalFilename();
//        String uploadFileName = String.format("%s_%s.%s", uuid, DateUtil.formatDate(new Date()), FileUtil.getSuffix(originalFileName));
//        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
//        // 上传图片
//        File file = null;
//        // 创建本地临时文件
//        try {
//            file = File.createTempFile(uploadPath, null);
//            multipartFile.transferTo(file);
//            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
//            // 数据万象分析图片信息
//            CIUploadResult ciUploadResult = putObjectResult.getCiUploadResult();
//            ImageInfo imageInfo = ciUploadResult.getOriginalInfo().getImageInfo();
//            // 封装返回结果
//            UploadPictureResult uploadPictureResult = new UploadPictureResult();
//            int width = imageInfo.getWidth();
//            int height = imageInfo.getHeight();
//            double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
//            uploadPictureResult.setUrl(cosClientConfig.getHost() + uploadPath);
//            uploadPictureResult.setPicName(FileUtil.mainName(originalFileName));
//            uploadPictureResult.setPicSize(FileUtil.size(file));
//            uploadPictureResult.setPicWidth(width);
//            uploadPictureResult.setPicHeight(height);
//            uploadPictureResult.setPicScale(picScale);
//            uploadPictureResult.setPicFormat(imageInfo.getFormat());
//            return uploadPictureResult;
//        } catch (Exception e) {
//            log.error("图片上传对象存储失败", e);
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
//        } finally {
//            deleteTempFile(file);
//        }
//    }
//    public void deletePicture(String oldUrl){
//        int publicIndex = oldUrl.indexOf("/public");
//        String key = oldUrl.substring(publicIndex);
//        cosManager.deletePictureObject(key);
//    }
//
//    private void deleteTempFile(File file) {
//        if (file != null) {
//            boolean del = FileUtil.del(file);
//            if(!del){
//                log.error("临时文件%s删除失败", file.getAbsolutePath());
//            }
//        }
//    }
//
//    /**
//     * 校验图片
//     * @param multipartFile
//     */
//    private void validPicture(MultipartFile multipartFile) {
//        ThrowUtils.throwIf(multipartFile == null, new BusinessException(ErrorCode.PARAMS_ERROR, "图片不能为空"));
//        // 上传文件大小不得超过2M
//        final long TWO_M = 2 * 1024 * 1024L;
//        ThrowUtils.throwIf(multipartFile.getSize() > TWO_M, new BusinessException(ErrorCode.PARAMS_ERROR, "图片大小不能超过2M"));
//        // 上传文件类型必须为图片
//        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
//        List<String> ALLOW_TYPES = Arrays.asList("jpg", "jpeg", "png", "webp", "gif", "bmp");
//        ThrowUtils.throwIf(!ALLOW_TYPES.contains(suffix), new BusinessException(ErrorCode.PARAMS_ERROR, "图片格式不正确"));
//    }
//}