package fanjh.mine.applibrary.utils;

import java.text.SimpleDateFormat;

/**
* @author fanjh
* @date 2017/11/29 18:56
* @description
* @note
**/
public class TimeUtils {
    public static String getSimpleTime(long time){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
        return simpleDateFormat.format(time);
    }
}
