package com.wang.wangpicture.api.imageoutpainting.model;

import cn.hutool.core.annotation.Alias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * AI扩图创建任务请求对象
 */
@Data
public class CreateOutPaintingTaskRequest implements Serializable {
    /**
     * curl --location --request POST 'https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting' \
     * --header "Authorization: Bearer $DASHSCOPE_API_KEY" \
     * --header 'X-DashScope-Async: enable' \
     * --header 'Content-Type: application/json' \
     * --data '{
     *     "model": "image-out-painting",
     *     "input": {
     *         "image_url": "http://xxx/image.jpg"
     *     },
     *     "parameters":{
     *         "angle": 45,
     *         "x_scale":1.5,
     *         "y_scale":1.5
     *     }
     * }'
      */
    /**
     * @JosProperty注解的作用是mvc用于接收前端传来的json数据，并自动将json数据映射到java对象中。
     * @Alias注解的作用是使用hutool进行序列化和反序列化时，按照指定的别名进行序列化和反序列化。
     *
     * 简单例子：
     * 前端传来的json数据：
     * {
     *     ”xScale”: 1.5,
     * }
     * java对象中可以通过xScale接收
     *
     * 将实体类通过Hutool序列化为json数据（用于调用第三方请求）:
     * {
     *     "x_scale": 1.5
     * }
     *
     */
    private static final long serialVersionUID = 1L;
    @JsonProperty("model")
    @Alias("model")
    private String model = "image-out-painting";

    @JsonProperty("input")
    @Alias("input")
    private Input input;

    @JsonProperty("parameters")
    @Alias("parameters")
    private Parameters parameters;
    // 这些内部类定义为public static, 是为了能够正确序列化和反序列化
    @Data
    public static class Input {
        @JsonProperty("imageUrl")
        @Alias("image_url")
        private String imageUrl;
    }
    @Data
    public static class Parameters implements Serializable {
        private static final long serialVersionUID = 1L;
        @JsonProperty("angle")
        private Integer angle;

        @JsonProperty("xScale")
        @Alias("x_scale")
        private Float xScale;

        @JsonProperty("yScale")
        @Alias("y_scale")
        private Float yScale;
    }

    // Getters and setters (if needed)
}


