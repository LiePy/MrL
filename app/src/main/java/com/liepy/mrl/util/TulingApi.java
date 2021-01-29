package com.liepy.mrl.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.liepy.mrl.util.Util.print;

public class TulingApi {
    // ================== 图灵机器人参数设置 ==========================
    public static final String urlString = "http://openapi.tuling123.com/openapi/api/v2";
    private static final String apiKey = "6837fbca9f0c494aa0745714db9a80a2";

    //图灵机器人接口
    public static String chat(String text){
        /**
         * @param text 聊天的输入文本
         * @return 聊天的输出文本
         */

        Map<String, Object> param = new HashMap<>();
        Map<String, String> requestProperty = new HashMap<>();
        requestProperty.put("Content-Type","application/json");
        String json = "{\n" +
                "    \"reqType\": 0,\n" +
                "    \"perception\": {\n" +
                "        \"inputText\": {\n" +
                "            \"text\": \"" + text + "\"\n" +
                "        },\n" +
//                "        \"selfInfo\": {\n" +
//                "            \"location\": {\n" +
//                "                \"city\": \"北京\",\n" +
//                "                \"province\": \"北京\"\n" +
//                "            }\n" +
//                "        }\n" +
                "    },\n" +
                "    \"userInfo\": {\n" +
                "        \"apiKey\": \"" + apiKey + "\",\n" +
                "        \"userId\": \"123456\"\n" +
                "    }\n" +
                "}";
        String response = MyRequest.post(urlString, param, requestProperty, json);
        print(response);
        String text_ans = getTextFromResponse(response);

        return text_ans;
    }

    public static String getTextFromResponse(String response){
        /**
         * @param response 图灵机器人借口的返回参数
         * @return 提取中文内容
         */
        String text = "";
        Pattern p = Pattern.compile("[^\\x00-\\xff]+.*[^\\x00-\\xff]+");
        Matcher m = p.matcher(response);

        if(m.find()){
            print(m.group(0));
            text = m.group(0);
        }

        return text;
    }
}
