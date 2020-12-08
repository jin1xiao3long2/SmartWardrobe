package com.example.demo.Controller;

import org.springframework.web.bind.annotation.*;
import net.sf.json.*;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


@RestController
public class myController {

    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/Clothes?useUnicode=true&characterEncoding=utf-8";
    static final String USER = "root";
    static final String PASS = "root";
    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    //查询
    @RequestMapping(value = "/query", method = RequestMethod.POST)
    public JSONArray Query(@RequestParam("Category") String Category) {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        //创建sql语句
        String sql = "SELECT * FROM clothes WHERE Category like '" + Category + "';";
        jsonObject.put("编号", "Number");
        jsonObject.put("类别", "Category");
        jsonObject.put("属性", "Property");
        jsonObject.put("季节", "Season");
        jsonObject.put("颜色", "Color");
        jsonObject.put("运动", "Sport");
        jsonObject.put("休闲", "Casual");
        jsonObject.put("轻熟", "Formal");
        jsonObject.put("艺术", "Art");
        jsonObject.put("嘻哈", "HIPHOP");
        jsonObject.put("图片路径", "PICTURE");

        ArrayList<String> func = new ArrayList<String>() {
            {
                add("int");
                add("string");
                add("string");
                add("int");
                add("string");
                add("int");
                add("int");
                add("int");
                add("int");
                add("int");
                add("string");
            }
        };
        //执行sql语句,获得JSON数据包
        jsonArray = connect_sql(sql, jsonObject, func);
        //返回数据包
        return jsonArray;
    }

    //搭配算法
    @RequestMapping(value = "/match", method = RequestMethod.POST)
    public JSONArray match(@RequestParam("Style") String param) {
        //获取当天温度状况
        HttpController controller = new HttpController();
        String data =   controller.weather("成都市");
        data = data.trim();
        String str = "";
        if(data != null && !"".equals(data)){
            for(int i = 0; i < data.length(); i++){
                if(data.charAt(i) >= 48 && data.charAt(i) <= 57){
                    str += data.charAt(i);
                }
            }
        }
        Integer temp = Integer.parseInt(str);

        //将温度进行分类以完成温度适应的算法
        char weather_season = '\0';
        if(temp < 10)
            weather_season = 'l';
        else if (temp < 25)
            weather_season = 'm';
        else
            weather_season = 'h';


        JSONArray blackArrays = controller.blackall(this);
        ArrayList<Integer> blackList = new ArrayList<Integer>();
        for(Iterator<JSONObject> iter = blackArrays.iterator(); iter.hasNext();){
            JSONObject jsonObject = iter.next();
            blackList.add(jsonObject.getInt("编号"));
        }

        JSONObject jsonObject = new JSONObject();
        JSONArray upArray = new JSONArray();
        JSONArray downArray = new JSONArray();
        JSONArray jsonArray = new JSONArray();
        jsonObject.put("编号", "Number");
        jsonObject.put("类别", "Category");
        jsonObject.put("属性", "Property");
        jsonObject.put("颜色", "Color");
        jsonObject.put("季节", "Season");
        jsonObject.put("图片路径", "PICTURE");
        jsonObject.put("得分", param);

        int[] need = new int[]{0, 0};
        int[] score = new int[]{10, 7, 3};
        ArrayList<String> func = new ArrayList<String>() {
            {
                add("int");
                add("string");
                add("string");
                add("string");
                add("int");
                add("string");
                add("int");
            }
        };

        int i = 0;
        boolean upNeed = true;
        boolean downNeed = true;
        do {
            //循环执行sql语句,获得需要的数据
            String sql = "SELECT Number, Category, Property, Color, Season, PICTURE, " + param + " FROM clothes WHERE " + param + " = " + score[i] + ";";
            jsonArray = connect_sql(sql, jsonObject, func);
            JSONObject tempjson = new JSONObject();

            //获取上装和下装的数据
            for (Iterator<JSONObject> iter = jsonArray.iterator(); iter.hasNext(); ) {
                tempjson = iter.next();
                String Category = tempjson.get("类别").toString();
                Integer Number = tempjson.getInt("编号");
                int Season = tempjson.getInt("季节");
                if (Category.equals("上衣") || Category.equals("外套")) {
                    if (!upNeed)
                        continue;
                    if (blackArrays.indexOf(Number) != -1)
                        continue;
                    if(!suit_season(Season,weather_season))
                        continue;
                    need[0]++;
                    upArray.add(tempjson);
                } else {
                    if (!downNeed)
                        continue;
                    if (blackArrays.indexOf(Number) != -1)
                        continue;
                    if(!suit_season(Season,weather_season))
                        continue;
                    need[1]++;
                    downArray.add(tempjson);
                }
            }
            i++;
            if (need[0] >= 5)
                upNeed = false;
            if (need[1] >= 4)
                downNeed = false;
        } while ((need[0] < 5 || need[1] < 4) && i < 3);

        //执行算法,返回结果
        return algo(upArray, downArray);
    }


    //日记查询
    @RequestMapping(value = "/diary", method = RequestMethod.POST)
    public JSONArray getParam(@RequestParam("Date") String param,
                              @RequestParam("Number1") Integer num1,
                              @RequestParam("Number2") Integer num2) {

        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        if (num1 == 0 && num2 == 0) {
            //获取编号
            String sql = "SELECT Number_c, Number_t FROM diary WHERE Date like '" + param + "';";
            jsonObject.put("上装", "Number_c");
            jsonObject.put("下装", "Number_t");
            ArrayList<String> func = new ArrayList<String>() {
                {
                    add("int");
                    add("int");
                }
            };

            jsonArray = connect_sql(sql, jsonObject, func);

            //获取服装信息
            JSONObject midObject = jsonArray.optJSONObject(0);

            sql = "SELECT Number, Category, Property, Color, PICTURE FROM clothes WHERE Number in ("
                    + midObject.get("上装") + " , " + midObject.get("下装") + ");";

            jsonObject.clear();
            jsonObject.put("编号", "Number");
            jsonObject.put("类别", "Category");
            jsonObject.put("属性", "Property");
            jsonObject.put("颜色", "Color");
            jsonObject.put("图片路径", "PICTURE");

            ArrayList<String> midfunc = new ArrayList<String>() {
                {
                    add("int");
                    add("string");
                    add("string");
                    add("string");
                    add("string");
                }
            };

            JSONArray resArray = connect_sql(sql, jsonObject, midfunc);

            return resArray;
        }
        else {
            String sql = "INSERT INTO diary VALUES ('" + param + "', " + num1 + " , " + num2 + ");";
            add_delSql(sql);
            return jsonArray;
        }
    }


    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public Integer add(@RequestParam("Category") String Category, @RequestParam("Property") String Property,
                       @RequestParam("Season") Integer Season, @RequestParam("Color") String Color,
                       @RequestParam("Sport") Integer Sport, @RequestParam("Casual") Integer Casual,
                       @RequestParam("Formal") Integer Formal, @RequestParam("Art") Integer Art,
                       @RequestParam("Hiphop") Integer Hiphop) {

        String sql = "SELECT Number FROM clothes ORDER BY Number DESC LIMIT 1";
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        jsonObject.put("编号", "Number");
        ArrayList<String> func = new ArrayList<String>() {
            {
                add("int");
            }
        };

        jsonArray = connect_sql(sql, jsonObject, func);

        int num = jsonArray.getJSONObject(0).getInt("编号") + 1;
        sql = "INSERT INTO clothes VALUES(" + num + ",'" + Category + "','" + Property
                + "'," + Season + ",'" + Color + "'," + Sport + "," + Casual + "," + Formal
                + "," + Art + "," + Hiphop + "," + "'/res/drawable/img" + num + ".png'" + ");";

        add_delSql(sql);

        return num;
    }

    @RequestMapping(value = "/del", method = RequestMethod.POST)
    public boolean del(@RequestParam("Number") Integer param) {

        String sql = "DELETE FROM clothes WHERE Number = " + param + ";";
        add_delSql(sql);
        return true;
    }

    //搭配算法封装
    private JSONArray algo(JSONArray upArray, JSONArray downArray) {

        JSONArray jsonAlgo = new JSONArray();
        int num = 10;
        JSONArray resArray = new JSONArray();

        int n = 0;
        int w = 0;
        boolean next = false;
        for (Iterator<JSONObject> Iter_up = upArray.iterator(); Iter_up.hasNext(); ) {
            JSONObject tempObject = Iter_up.next();
            for (Iterator<JSONObject> Iter_down = downArray.iterator(); Iter_down.hasNext(); ) {
                if(!next)
                {
                    n++;
                    if(n >= 3){
                        break;
                    }
                }else{
                    n++;
                    if(n < 3){
                        Iter_down.next();
                        continue;
                    }
                }
                JSONArray tempArray = new JSONArray();
                tempArray.add(tempObject);
                tempArray.add(Iter_down.next());
                jsonAlgo.add(tempArray);
            }
            n = 0;
            w++;
            if(w >= 3)
                next = true;
        }
        jsonAlgo.sort(Comparator.comparing(obj -> {
            JSONArray jsonArray = ((JSONArray) obj);
            Integer value = 0;
            for (Iterator<JSONObject> Iterator = jsonArray.iterator(); Iterator.hasNext(); ) {
                JSONObject jsonobj = Iterator.next();
                value += jsonobj.getInt("得分");
            }
            Random random = new Random();
            return (value + random.nextInt(15));
        }).reversed());

        Iterator<JSONArray> iter = jsonAlgo.iterator();
        for (int i = 0; i < num; i++) {
            if (iter.hasNext())
                resArray.add(iter.next());
        }


        return resArray;
    }

    //数据库操作封装
    public void add_delSql(String sql) {
        Connection conn = null;
        Statement stmt = null;
        try {
            // 注册 JDBC 驱动
            Class.forName(JDBC_DRIVER);
            // 打开链接

            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            // 执行查询

            stmt = conn.createStatement();
            stmt.execute(sql);

            // 完成后关闭

            stmt.close();
            conn.close();
        } catch (SQLException se) {
            // 处理 JDBC 错误
            se.printStackTrace();
        } catch (Exception e) {
            // 处理 Class.forName 错误
            e.printStackTrace();
        } finally {
            // 关闭资源
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException se2) {
            }// 什么都不做
            try {
                if (conn != null) conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }

    }

    public JSONArray connect_sql(String sql, JSONObject jsonObject, ArrayList<String> func) {
        JSONArray jsonArray = new JSONArray();
        ArrayList<String> keys = new ArrayList<String>();
        ArrayList<String> values = new ArrayList<String>();
        Set<String> KeySet = jsonObject.keySet();
        Iterator<String> it = KeySet.iterator();
        while (it.hasNext()) {
            String key = it.next();
            keys.add(key);
            values.add(jsonObject.get(key).toString());
        }
        //System.out.println(keys);
        //System.out.println(values);
        //System.out.println(func);
        Connection conn = null;
        Statement stmt = null;
        try {
            // 注册 JDBC 驱动
            Class.forName(JDBC_DRIVER);
            // 打开链接

            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            // 执行查询

            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            //getArrayList
            JSONObject temp = new JSONObject();
            while (rs.next()) {
                // 通过字段检索
                for (int i = 0; i < jsonObject.size(); i++) {
                    if (func.get(i) == "int") {
                        //System.out.println(keys.get(i) + values.get(i) + func.get(i));
                        temp.put(keys.get(i), rs.getInt(values.get(i)));
                        //System.out.println(temp);
                    } else if (func.get(i) == "string") {
                        //System.out.println(keys.get(i) + values.get(i) + func.get(i));
                        temp.put(keys.get(i), rs.getString(values.get(i)));
                        //System.out.println(temp);
                    } else if (func.get(i) == "date") {
                        //System.out.println(keys.get(i) + values.get(i) + func.get(i));
                        temp.put(keys.get(i), rs.getDate(values.get(i)));
                        //System.out.println(temp);
                    }
                }
                //System.out.println(temp);
                // 输出数据
                jsonArray.add(temp);
            }

            // 完成后关闭
            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException se) {
            // 处理 JDBC 错误
            se.printStackTrace();
        } catch (Exception e) {
            // 处理 Class.forName 错误
            e.printStackTrace();
        } finally {
            // 关闭资源
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException se2) {
            }// 什么都不做
            try {
                if (conn != null) conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
        return jsonArray;
    }

    //季节适应算法封装
    private boolean suit_season(int clothes_season, char weather_season){
        if(weather_season == 'l'){
            if(clothes_season == 4 || clothes_season == 5)
                return true;
            return false;
        }else if(weather_season == 'm'){
            if(clothes_season == 2 || clothes_season == 3 || clothes_season == 4)
                return true;
            return false;
        }else{
            if(clothes_season == 1 || clothes_season == 2)
                return true;
            return false;
        }
    }
}
