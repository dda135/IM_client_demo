package fanjh.mine.applibrary.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.ParameterizedType;

/**
* @author fanjh
* @date 2017/11/29 10:50
* @description
* @note
**/
public class GsonUtils {

    private static class Holder{
        static Gson IMPL = new GsonBuilder().
                setDateFormat("MMM dd,yyyy KK:mm:ss aa").
                create();
    }

    public static Gson getInstance(){
        return Holder.IMPL;
    }

}
