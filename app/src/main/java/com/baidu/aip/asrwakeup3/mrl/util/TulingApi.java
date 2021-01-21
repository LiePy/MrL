package com.baidu.aip.asrwakeup3.mrl.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.baidu.aip.asrwakeup3.mrl.util.util.print;

public class TulingApi {
    //图灵机器人接口
    public static String chat(String text, String apiKey){
        /**
         * @param
         */

        String urlString = "http://openapi.tuling123.com/openapi/api/v2";
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
        String text = "";
        Pattern p = Pattern.compile("[^\\x00-\\xff]+");
        Matcher m = p.matcher(response);

        if(m.find()){
            print(m.group(0));
            text = m.group(0);
        }

        return text;
    }
}
