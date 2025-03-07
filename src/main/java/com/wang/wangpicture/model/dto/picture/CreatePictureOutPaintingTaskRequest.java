package com.wang.wangpicture.model.dto.picture;

import com.wang.wangpicture.api.imageoutpainting.model.CreateOutPaintingTaskRequest;
import lombok.Data;

import java.io.Serializable;
@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long pictureId;
    private CreateOutPaintingTaskRequest.Parameters parameters;
}
