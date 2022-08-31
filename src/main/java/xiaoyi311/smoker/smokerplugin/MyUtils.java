package xiaoyi311.smoker.smokerplugin;

import org.bukkit.Location;
import org.json.simple.JSONObject;

import java.util.ArrayList;

public class MyUtils {

    //JSON数据读取
    public static Object IsJsonObjectRead(JSONObject json, String key){
        Object DataTemp = json.get(key);
        return DataTemp == null ? new JSONObject() : DataTemp;
    }

    //是否为数字
    public static boolean IsInt(String str){
        try{
            Integer.parseInt(str);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    //是否位于两个数字间
    public static boolean IsInInts(int int1, int int2, double target){
        if (int1 > int2){
            return target < int1 && target > int2;
        }else{
            return target > int1 && target < int2;
        }
    }

    //是否位于区域内
    public static boolean IsInRegion(Location pos1, Location pos2, Location playerPos){
        return IsInInts(pos1.getBlockX(), pos2.getBlockX(), playerPos.getBlockX()) && IsInInts(pos1.getBlockY(), pos2.getBlockY(), playerPos.getBlockY()) && IsInInts(pos1.getBlockZ(), pos2.getBlockZ(), playerPos.getBlockZ());
    }

    //JSON列表数据读取
    public static Object IsJsonObjectListRead(JSONObject json, String key) {
        Object DataTemp = json.get(key);
        return DataTemp == null ? new ArrayList<JSONObject>() : DataTemp;
    }
}
