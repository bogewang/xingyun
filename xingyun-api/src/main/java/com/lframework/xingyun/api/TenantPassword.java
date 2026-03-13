package com.lframework.xingyun.api;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;

public class TenantPassword {

    public static void main(String[] args) {
        // 租户数据库密码
        String s = "7212413WEnmu";

        // jugg.secret.key
        byte[] key = Base64.decode("O1O8N/gniv4/2sAXPymRcg==");
        AES aes = SecureUtil.aes(key);

        System.out.println(aes.encryptHex(s));

    }
}
