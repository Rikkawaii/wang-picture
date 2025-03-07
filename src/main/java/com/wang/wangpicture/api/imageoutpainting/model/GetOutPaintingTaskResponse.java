package com.wang.wangpicture.api.imageoutpainting.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class GetOutPaintingTaskResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId; // 请求唯一标识
    private Output output; // 任务输出信息
    private Usage usage; // 图像统计信息
    @Data
    // 内部类 Output
    public static class Output implements Serializable {
        private static final long serialVersionUID = 1L;
        private String taskId; // 任务ID
        private String taskStatus; // 任务状态
        private String submitTime; // 任务提交时间
        private String scheduledTime; // 任务调度时间
        private String endTime; // 任务结束时间
        private String outputImageUrl; // 输出图像URL地址
        private String code; // 错误码（失败时返回）
        private String message; // 错误信息（失败时返回）
        private TaskMetrics taskMetrics; // 任务结果统计（仅在任务进行中时有）
    }
    @Data
    // 内部类 TaskMetrics (仅在任务进行中时返回)
    public static class TaskMetrics implements Serializable {
        private static final long serialVersionUID = 1L;
        private int total; // 总的任务数
        private int succeeded; // 任务状态为成功的任务数
        private int failed; // 任务状态为失败的任务数
    }

    // 内部类 Usage (图像统计信息)
    @Data
    public static class Usage implements Serializable {
        private static final long serialVersionUID = 1L;
        private int imageCount; // 模型生成图片的数量

        // Getter 和 Setter 方法
        public int getImageCount() {
            return imageCount;
        }

        public void setImageCount(int imageCount) {
            this.imageCount = imageCount;
        }
    }
}
