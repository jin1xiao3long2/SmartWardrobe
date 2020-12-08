package com.example.demo.Controller;

import org.springframework.web.bind.annotation.*;
import net.sf.json.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;

@RestController
public class HttpController {

    //黑名单接口
    @RequestMapping(value = "/black", method = RequestMethod.POST)
    public JSONArray black(@RequestParam("Type") String type,
                      @RequestParam("Number") int Num) {
        myController controller = new myController();
        JSONArray jsonArray = new JSONArray();
        String sql;
        if (type.equals("del")) {
            sql = "DELETE FROM black WHERE Number = " + Num + ";";
            controller.add_delSql(sql);
        } else if (type.equals("add")) {
            sql = "INSERT INTO black VALUES(" + Num + ");";
            controller.add_delSql(sql);
        } else if (type.equals("all")){
            jsonArray = blackall(controller);
        } else
            sql = "";

        return jsonArray;
    }

    //获得温度的接口
    public String weather(String param) {
        String weather = getWeather(param);
        System.out.println(weather);
        JSONObject jsonObject = JSONObject.fromObject(weather);
        JSONObject data = (JSONObject) jsonObject.get("data");
        JSONArray forecast = (JSONArray) data.get("forecast");
        JSONObject todayData = (JSONObject) forecast.get(0);
        String temp = todayData.getString("high");
        return temp;
    }

    private String getWeather(String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        StringBuffer stringBuffer = new StringBuffer();
        String result = "";
        String url = "http://wthrcdn.etouch.cn/weather_mini?city=" + param;
        byte[] byteArray = new byte[0];
        try {

            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("Accept-Charset", "GBK");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 发送POST请求必须设置如下两行
            conn.connect();
            // 获取URLConnection对象对应的输出流
//            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
//            out.print("");
            // flush输出流的缓冲
//            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            GZIPInputStream gZIPInputStream = null;
            String encoding = conn.getContentEncoding();
            if (encoding.equals("gzip")) {
                gZIPInputStream = new GZIPInputStream(conn.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gZIPInputStream));
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    //转化为UTF-8的编码格式
                    line = new String(line.getBytes("UTF-8"));
                    stringBuffer.append(line);
                }
                bufferedReader.close();
            } else {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    //转化为UTF-8的编码格式
                    line = new String(line.getBytes("UTF-8"));
                    stringBuffer.append(line);
                }
                bufferedReader.close();
            }
            //返回打开连接读取的输入流，输入流转化为StringBuffer类型，这一套流程要记住，常用

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuffer.toString();
    }

    public JSONArray blackall(myController controller) {
        String sql = "SELECT Number FROM black";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("编号", "Number");
        ArrayList<String> func = new ArrayList<String>(){
            {
                add("int");
            }
        };
        JSONArray jsonArray = controller.connect_sql(sql,jsonObject,func);
        return jsonArray;
    }
}
