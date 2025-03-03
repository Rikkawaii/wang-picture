package com.wang.wangpicture;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class WangPictureApplicationTests {

    @Test
    void contextLoads() {
    }
    @Test
    void testJson(){
        //测试fastjson和hutool对null的处理
        List<String> tags = null;
        String result1 = JSONUtil.toJsonStr(tags);
        String result2 = JSON.toJSONString(tags);
        if(result1 == null){
            System.out.print("result1" + " is null");
        }else if("null".equals(result1)){
            System.out.print("result1" + " == (str)null");
        }
        if(result2 == null){
            System.out.print("result2" + " is null");
        }else if("null".equals(result2)){
            System.out.print("result2" + " == (str)null");
        }
    }
}
