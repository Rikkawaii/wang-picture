package com.wang.wangpicture.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.EncryptedPutObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.wang.wangpicture.config.CosClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
@Component
public class CosManager {
    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private COSClient cosClient;

    /**
     * 上传文件到cos
     * @param key 唯一键，可以理解为文件路径
     * @param file 文件
     * @return
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 对图片进行处理（获取基本信息也被视作为一种处理）
        // 注意这里的putObjectRequest需要带上picOperations参数，这样上传图片后的返回结果中会包含图片的基本信息（数据万象提供）
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息（是否返回原图信息，0不返回原图信息，1返回原图信息，默认为0）
        picOperations.setIsPicInfo(1);
        // 构造处理参数
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    public void deletePictureObject(String key) {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }
}
