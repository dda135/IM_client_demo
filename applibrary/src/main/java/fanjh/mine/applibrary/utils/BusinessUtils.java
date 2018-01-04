package fanjh.mine.applibrary.utils;

/**
* @author fanjh
* @date 2017/11/29 14:07
* @description
* @note
**/
public class BusinessUtils {

    public static String getClientID(int userID,String token){
        return userID + "_" + token;
    }

}
