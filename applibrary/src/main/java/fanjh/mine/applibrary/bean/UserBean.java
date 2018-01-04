package fanjh.mine.applibrary.bean;

import java.io.Serializable;
import java.util.Date;

/**
* @author fanjh
* @date 2017/11/29 10:47
* @description
* @note
**/
public class UserBean implements Serializable{
    private static final long serialVersionUID = -6989895402404783571L;
    public int id;
    public TokenBean token;
    public String nickname;
    public String password;
    public String mobile;
    public long createTime;
    public long lastLoginTime;
    public String portrait;
    public String sex;
    public String birth;
    public String address;

    public boolean isMan(){
        return "0".equals(sex);
    }

}
