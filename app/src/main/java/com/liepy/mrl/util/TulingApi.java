package com.liepy.mrl.util;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.liepy.mrl.util.Util.print;

public class TulingApi {
    // ================== 图灵机器人参数设置 ==========================
    public static final String urlString = "http://openapi.tuling123.com/openapi/api/v2";
    private static final String apiKey = "6837fbca9f0c494aa0745714db9a80a2";
    private static final String TAG = TulingApi.class.getSimpleName();

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
        String text_ans = getTextFromResponse(response,0);

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

        Log.d(TAG,text);
        return text;
    }

    public static String getTextFromResponse(String response, int code){
        JsonParser parser = new JsonParser();
        JsonElement je = parser.parse(response);
        JsonObject jobj = je.getAsJsonObject();//从json元素转变成json对象
        String text = ((JsonArray)(jobj.get("results"))).get(0).getAsJsonObject().
                get("values").getAsJsonObject().get("text").getAsString();//从json对象获取指定属性的值
//        System.out.println(text);
//        int age = jobj.get("age").getAsInt();
        return text;
    }
}
