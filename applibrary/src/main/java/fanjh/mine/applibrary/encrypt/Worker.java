package fanjh.mine.applibrary.encrypt;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by faker on 2017/11/16.
 */

public class Worker {
    static String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCMIVUfXRO3UbPqeQqrcVOr4ywbbPBzj5LlqOA8\n" +
            "teTikn+eaDJckP3oVChf6zsfzTC471B0Eo93MPX0lVK3CyxXZOpPiWpOZ4kMmgI1N4F+Q9BGVDEc\n" +
            "EhnVp3XhumMt2dnNjt4ec3pTtohKclUpdr2Dzf88hVQA2cQSIi3zf257OwIDAQAB";

    public static String getContent(String desKey,String json){
        String headContent = GetKey.getKey(10);//内容头部的10个字符串
        String tailContent = GetKey.getKey(10);//内容尾部的10个字符串
        String desKeyWithRsa = SecUtil.encrypt(publicKey, desKey);//经过rsa处理的随机数

        try {
            String contentWithDes = DesUtil.encrypt(json, desKey);//经过des处理的内容
            return headContent + contentWithDes + desKeyWithRsa + tailContent;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * 解密服务器端返回来的加密字符串
     *
     * @param string
     * @return
     */
    public static String getDecrypt(String desKey, String string) {
        String data = "";
        if (string.length() > 20) {
            String keyAndContentWithDes = string.substring(10, string.length() - 10);
            String[] keyAndContent = keyAndContentWithDes.split("\\|");
            String keyWithDes = keyAndContent[0];
            try {
                String D = DesUtil.decrypt(keyWithDes, desKey);
                data = DesUtil.decrypt(keyAndContent[1], D);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return data;
    }

    public static String md5(String content) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(content.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("NoSuchAlgorithmException",e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UnsupportedEncodingException", e);
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10){
                hex.append("0");
            }
            hex.append(Integer.toHexString(b & 0xFF));
        }
        return hex.toString();
    }

}
