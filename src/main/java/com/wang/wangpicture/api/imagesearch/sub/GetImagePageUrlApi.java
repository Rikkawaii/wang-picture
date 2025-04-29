package com.wang.wangpicture.api.imagesearch.sub;

import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import com.alibaba.fastjson.JSON;
import com.wang.wangpicture.exception.BusinessException;
import com.wang.wangpicture.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GetImagePageUrlApi {
    private static final String AcsToken = "1741235469616_1741247912164_Gpttr+ZYFLBM8Cyg8CHsxS6qtdFQYa2uoU42ctrBUrsogc9TDcbWM8iDAKYu65t0CWa/lRM0ZIugDKk5NWOBIREF1PM9GUqUXuetm38a7oSoYHzZ48kEKefPJ4wnLkKQ4/V+zE30KdYgKoajnv6FPXv3Eil6zw13r4rmzx/r1/Ovo7m9uuEYAfuHv46IGyL8LzLZznPBQ+ZMFSXQyLkfCv5107BqEwuKLjIh3KoG8xMg4KDiFpHeJi7Ftx9iAxWtUA/W3l6T2KyywsLgBex+dd8jAe4R/JeZrXWhkCmUCQMGZt7qifp48wzsSzcYJOorMztgeyt7sm96GtBmWaTvXxPozgNy8Wg6ckvQiIHyhp1H6gjc05/LhPHfVhata7GwpYoi3kb3bf71tVXlj83zboOCQl4CxFU5PGD1E7ttEjfJNsn8/ZgSpq9Qrin5hFAj";
    /**
     * 获取图片页面地址
     *
     * @param imageUrl
     * @return
     */
    public static String getImagePageUrl(String imageUrl) {
        // 1. 准备请求参数
        Map<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");
        // 获取当前时间戳
        long uptime = System.currentTimeMillis();
        // 请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;

        try {
            // 2. 发送 POST 请求到百度接口
            HttpResponse response = HttpRequest.post(url)
                    .form(formData)
                    .header("Acs-Token", AcsToken)
                    .timeout(5000)
                    .execute();
            // 判断响应状态
            if (HttpStatus.HTTP_OK != response.getStatus()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            // 解析响应
            String responseBody = response.body();
            Map<String, Object> result = JSON.parseObject(responseBody, Map.class);
            // 3. 处理响应结果
            if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            String rawUrl = (String) data.get("url");
            // 对 URL 进行解码
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            // 如果 URL 为空
            if (searchResultUrl == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效结果");
            }
            return searchResultUrl;
        } catch (Exception e) {
            log.error("搜索失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://img.shetu66.com/2023/07/14/1689302077000124.png";
        String result = getImagePageUrl(imageUrl);
        System.out.println("搜索成功，结果 URL：" + result);
    }
}
