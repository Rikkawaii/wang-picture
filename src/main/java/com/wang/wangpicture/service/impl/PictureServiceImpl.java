package com.wang.wangpicture.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wang.wangpicture.exception.BusinessException;
import com.wang.wangpicture.exception.ErrorCode;
import com.wang.wangpicture.exception.ThrowUtils;
import com.wang.wangpicture.manager.upload.FilePictureUpload;
import com.wang.wangpicture.manager.upload.PictureUploadTemplate;
import com.wang.wangpicture.manager.upload.UrlPictureUpload;
import com.wang.wangpicture.model.dto.picture.*;
import com.wang.wangpicture.model.entity.Picture;
import com.wang.wangpicture.model.entity.User;
import com.wang.wangpicture.model.enums.PictureReviewStatusEnum;
import com.wang.wangpicture.model.vo.PictureVO;
import com.wang.wangpicture.model.vo.UserVO;
import com.wang.wangpicture.service.PictureService;
import com.wang.wangpicture.mapper.PictureMapper;
import com.wang.wangpicture.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author xwzy
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-02-25 22:30:20
 */
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {
    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private UserService userService;

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
                ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 用于判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 查询id对应图片是否存在，如果存在且是本人或管理员，则取出旧图片的url，便于覆盖上传
        String oldPicUrl = null;
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 仅本人或管理员可编辑
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            oldPicUrl = oldPicture.getUrl();
        }
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        if (StrUtil.isNotBlank(oldPicUrl)){
            pictureUploadTemplate.deletePicture(oldPicUrl);
        }
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        // （构造图片name时，先判断PictureUploadRequest中是否携带picName，不携带时再根据图片信息解析出名字）
        String picName = pictureUploadRequest.getPicName();
        picture.setName(StrUtil.isBlank(picName) ? uploadPictureResult.getPicName() : picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 填充审核信息
        fillReviewParams(picture, loginUser);
        // 入库
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
        return PictureVO.objToVo(picture);
    }
//    @Override
//    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
//        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
//        // 用于判断是新增还是更新图片
//        Long pictureId = null;
//        if (pictureUploadRequest != null) {
//            pictureId = pictureUploadRequest.getId();
//        }
//        // 查询id对应图片是否存在，如果存在且是本人或管理员，则取出旧图片的url，便于覆盖上传
//        String oldPicUrl = null;
//        if (pictureId != null) {
//            Picture oldPicture = this.getById(pictureId);
//            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
//            // 仅本人或管理员可编辑
//            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//            }
//            oldPicUrl = oldPicture.getUrl();
//        }
////        UploadPictureResult uploadPictureResult = null;
//        // 更新上传方式其一
////        // a.如果是更新图片，则通过oldPicUrl覆盖上传
////        if(oldPicUrl != null){
////            uploadPictureResult = fileManager.uploadPicture(multipartFile, oldPicUrl, false);
////        }else{
////            // b.如果是新增图片，则正常构建url上传
////            // 按照用户 id 划分目录 todo: 考虑根据不同场景设置不同上传路径
////            String uploadPathPrefix = String.format("public/%s", loginUser.getId());
////            uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix, true);
////        }
//        // 更新上传方式其二
//        if(oldPicUrl != null){
//            fileManager.deletePicture(oldPicUrl);
//        }
//        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
//        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
//        // 构造要入库的图片信息
//        Picture picture = new Picture();
//        picture.setUrl(uploadPictureResult.getUrl());
//        picture.setName(uploadPictureResult.getPicName());
//        picture.setPicSize(uploadPictureResult.getPicSize());
//        picture.setPicWidth(uploadPictureResult.getPicWidth());
//        picture.setPicHeight(uploadPictureResult.getPicHeight());
//        picture.setPicScale(uploadPictureResult.getPicScale());
//        picture.setPicFormat(uploadPictureResult.getPicFormat());
//        picture.setUserId(loginUser.getId());
//        // 如果 pictureId 不为空，表示更新，否则是新增
//        if (pictureId != null) {
//            // 如果是更新，需要补充 id 和编辑时间
//            picture.setId(pictureId);
//            picture.setEditTime(new Date());
//        }
//        // 填充审核信息
//        fillReviewParams(picture, loginUser);
//        // 入库
//        boolean result = this.saveOrUpdate(picture);
//        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
//        return PictureVO.objToVo(picture);
//    }

    /**
     * 构造查询条件
     * @param pictureQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null){
            return queryWrapper;
        }
        // 获取pictureQueryRequest中的所有属性
        Long id = pictureQueryRequest.getId();
        String url = pictureQueryRequest.getUrl();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Date createTime = pictureQueryRequest.getCreateTime();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        // 构造查询条件
        // 1.根据搜索词searchText进行模糊查询
        if (searchText != null && !searchText.trim().equals("")) {
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or().like("introduction", searchText));
        }
        // 2.根据其他条件进行精确查询
        // isNotEmpty() 方法通常用于判断一个对象或字符串是否非空。
        // 对于字符串来说，它主要检查字符串是否为 null 或者长度是否为 0
        // isNotBlank() 方法通常用于判断一个字符串是否非空白。它不仅会检查字符串是否为 null 或空字符串，
        // 还会检查字符串是否只包含空白字符（如空格、制表符等）
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(StrUtil.isNotEmpty(url), "url", url);
        queryWrapper.eq(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.eq(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "pic_size", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "pic_width", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "pic_height", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "pic_scale", picScale);
        queryWrapper.eq(StrUtil.isNotBlank(picFormat), "pic_format", picFormat);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "user_id", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(createTime), "create_time", createTime);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 3. 根据排序字段和排序方式进行排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 编写图片数据校验方法，用于更新和修改图片时进行判断
     * @param picture
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 已是该状态
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        // 更新审核状态
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，创建或编辑都要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * 批量抓取图片
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        // 格式化数量
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
        // 要抓取的地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("a.iusc");
        int uploadCount = 0;
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        for (Element imgElement : imgElementList) {
            String dataM = imgElement.attr("m");
            String fileUrl;
            // 解析JSON字符串
            JSONObject jsonObject = JSONUtil.parseObj(dataM);
            // 获取murl字段（原始图片URL）
            fileUrl = jsonObject.getStr("murl");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过: {}", fileUrl);
                continue;
            }
            // 处理图片上传地址，防止出现转义问题(改成获取murl一般不会出现下面这种情况)
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            if (StrUtil.isNotBlank(namePrefix)) {
                // 设置图片名称，序号连续递增
                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            }
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功, id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }
}




