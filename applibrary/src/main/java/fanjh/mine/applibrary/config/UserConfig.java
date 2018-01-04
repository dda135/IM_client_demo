package fanjh.mine.applibrary.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import fanjh.mine.applibrary.base.BaseApplication;
import fanjh.mine.applibrary.bean.UserBean;

/**
* @author fanjh
* @date 2017/11/28 17:05
* @description
* @note
**/
public class UserConfig {
    public static final String USER_MESSAGE = "user_message";
    public static final String SEX_MAN = "0";
    public static final String SEX_WOMAN = "1";
    public static final String CACHE_DIRNAME = "user_cache";
    public static final String CACHE_KEY_USER_MESSAGE = "key_user_message";
    public static final String USER_ID = "id";
    public static final String USER_NICKNAME = "nickname";
    public static final String USER_MOBILE = "mobile";
    public static final String USER_PORTRAIT = "portrait";
    public static final String USER_SEX = "sex";
    public static final String USER_BIRTH = "birth";
    public static final String USER_ADDRESS = "address";
    public static final String TOKEN = "token";

    public static String getToken(){
        return getSharedPreferences().getString(TOKEN,null);
    }

    public static int getID(){
        return getSharedPreferences().getInt(USER_ID,0);
    }

    public static String getAddress(){
        return getSharedPreferences().getString(USER_ADDRESS,null);
    }

    public static String getNickname(){
        return getSharedPreferences().getString(USER_NICKNAME,null);
    }

    public static String getMobile(){
        return getSharedPreferences().getString(USER_MOBILE,null);
    }

    public static String getPortrait(){
        return getSharedPreferences().getString(USER_PORTRAIT,null);
    }

    public static String getSex(){
        return getSharedPreferences().getString(USER_SEX,null);
    }

    public static String getSexText(){
        String sex = getSex();
        if(SEX_MAN.equals(sex)){
            return "男";
        }else if(SEX_WOMAN.equals(sex)){
            return "女";
        }
        return "不识别！";
    }

    public static boolean isMan(){
        return SEX_MAN.equals(getSex());
    }

    public static boolean isWoman(){
        return SEX_WOMAN.equals(getSex());
    }

    public static String getBirth(){
        return getSharedPreferences().getString(USER_BIRTH,null);
    }

    private static SharedPreferences getSharedPreferences(){
        return BaseApplication.application.getSharedPreferences(CACHE_DIRNAME,Context.MODE_PRIVATE);
    }

    public static void updateUserMessage(UserBean userBean){
        SharedPreferences sp = getSharedPreferences();
        SharedPreferences.Editor editor = sp.edit();
        if(userBean.id > 0) {
            editor.putInt(USER_ID, userBean.id);
        }
        if(!TextUtils.isEmpty(userBean.nickname)){
            editor.putString(USER_NICKNAME,userBean.nickname);
        }
        if(!TextUtils.isEmpty(userBean.mobile)){
            editor.putString(USER_MOBILE,userBean.mobile);
        }
        if(!TextUtils.isEmpty(userBean.portrait)){
            editor.putString(USER_PORTRAIT,userBean.portrait);
        }
        if(!TextUtils.isEmpty(userBean.address)){
            editor.putString(USER_ADDRESS,userBean.address);
        }
        if(!TextUtils.isEmpty(userBean.sex)){
            editor.putString(USER_SEX,userBean.sex);
        }
        if(!TextUtils.isEmpty(userBean.birth)){
            editor.putString(USER_BIRTH,userBean.birth);
        }
        if(null != userBean.token && !TextUtils.isEmpty(userBean.token.tokenValue)){
            editor.putString(TOKEN,userBean.token.tokenValue);
        }
        editor.apply();
    }

}
