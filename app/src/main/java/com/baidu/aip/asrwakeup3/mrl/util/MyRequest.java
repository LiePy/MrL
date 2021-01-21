package com.baidu.aip.asrwakeup3.mrl.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class MyRequest {
    public static String post(String url, Map<String, Object> param, Map<String, String> requestProperty, String json) {
        StringBuilder builder=new StringBuilder();
        try {
            StringBuilder params=new StringBuilder();
            for(Map.Entry<String, Object> entry:param.entrySet()){
                params.append(entry.getKey());
                params.append("=");
                params.append(entry.getValue().toString());
                params.append("&");
            }
            if(params.length()>0){
                params.deleteCharAt(params.lastIndexOf("&"));
            }
            URL restServiceURL = new URL(url+(params.length()>0 ? "?"+params.toString() : ""));
            HttpURLConnection httpConnection = (HttpURLConnection) restServiceURL.openConnection();
            httpConnection.setRequestMethod("POST");
            for(Map.Entry<String, String> entry:requestProperty.entrySet()){
                httpConnection.setRequestProperty(entry.getKey(), entry.getValue());
            }
//            httpConnection.setRequestProperty("Authorization", "Basic enloaDpwdWJpbmZvOTE4");
            // 设置是否从httpUrlConnection读入，默认情况下是true;
            httpConnection.setDoInput(true);
            // Post 请求不能使用缓存
            httpConnection.setUseCaches(false);

            //新增
//            String json = "";
            httpConnection.connect();
            OutputStreamWriter out = new OutputStreamWriter(httpConnection.getOutputStream(),
                    "utf-8");//解决传参时中文乱码
            out.write(json);
            out.flush();


            if (httpConnection.getResponseCode() != 200) {
                throw new RuntimeException("HTTP POST Request Failed with Error code : "
                        + httpConnection.getResponseCode());
            }
            InputStream inStrm = httpConnection.getInputStream();
            byte []b=new byte[1024];
            int length=-1;
            while((length=inStrm.read(b))!=-1){
                builder.append(new String(b,0,length));
//                Log.d("line", b.toString());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }
}
