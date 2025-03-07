package com.wang.wangpicture.api.imageoutpainting;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.wang.wangpicture.api.imageoutpainting.model.CreateOutPaintingTaskRequest;
import com.wang.wangpicture.api.imageoutpainting.model.CreateOutPaintingTaskResponse;
import com.wang.wangpicture.api.imageoutpainting.model.GetOutPaintingTaskResponse;
import com.wang.wangpicture.exception.BusinessException;
import com.wang.wangpicture.exception.ErrorCode;
import com.wang.wangpicture.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliYunApi {
    @Value("${aliYun.API-KEY}")
    private String API_KEY;
    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    public CreateOutPaintingTaskResponse createImageOutPaintingTask(CreateOutPaintingTaskRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .header("X-DashScope-Async", "enable")
                .body(JSONUtil.toJsonStr(request));
        // note: 这种写法可以自动关闭response，不需要手动关闭
        try(HttpResponse response = httpRequest.execute()) {
            if (!response.isOk()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "第三方服务创建扩图任务失败1");
            }
            String result = response.body();
            CreateOutPaintingTaskResponse responseObject = JSONUtil.toBean(result, CreateOutPaintingTaskResponse.class);
            String errorCode = responseObject.getCode();
            if (StrUtil.isNotEmpty(errorCode)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "第三方服务创建扩图任务失败2");
            }
            return responseObject;
        }
    }
    public GetOutPaintingTaskResponse getOutPaintingTaskResult(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        HttpRequest httpRequest = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header("Authorization", "Bearer " + API_KEY);
        try(HttpResponse response = httpRequest.execute()){
            if (!response.isOk()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "第三方服务查询扩图任务失败");
            }
            // 因为前端会轮询任务状态，所以这里不需要判断任务是否成功，只需要返回结果即可
            return BeanUtil.toBean(JSONUtil.parseObj(response.body()), GetOutPaintingTaskResponse.class);
        }
    }
}
