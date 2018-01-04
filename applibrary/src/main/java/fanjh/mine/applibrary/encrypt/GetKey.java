package fanjh.mine.applibrary.encrypt;

import java.util.UUID;

/**
 * 随机生成字符串
 * Created by wsdevotion on 15/10/26.
 */
public class GetKey {

    public static String getKey(int length) {
        String s = UUID.randomUUID().toString();
        System.out.println(s);
        String[] str = s.split("-");
        s = "";
        for (String st : str) {
            s = s + st;
        }
        System.out.println(s);
        String string = s.substring(0, length);
        return string;
    }
}
