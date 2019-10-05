package com.i.server.data.redis.service;

import com.i.server.consts.RedisConsts;
import com.zx.sms.connect.manager.EndpointManager;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class RedisService {
    private final static EndpointManager manager = EndpointManager.INS;
    static String deduction = loadFileContent("lua/deduction.lua");
    static String connection = loadFileContent("lua/connection.lua");

    public static int getMaxChannelByAppId(String appId) {
        System.out.println("RedisService appId = " + appId);
        int maxNumber = Integer
                .parseInt(String.valueOf(manager.getRedisOperationSetsMap().get(RedisConsts.REDIS_APP_INFO)
                        .getRedisTemplate().opsForHash().get(appId, "max_connection")));
        System.out.println("maxNumber = " + maxNumber);
        return maxNumber;
    }

    public static boolean conn(String appId) {
        boolean result = manager.getRedisOperationSetsMap().get(RedisConsts.REDIS_APP_INFO).eval(connection, Collections.singletonList(appId));
        return result;
    }

    public static boolean deduct(String appId, String number) {
        boolean result = manager.getRedisOperationSetsMap().get(RedisConsts.REDIS_APP_INFO).eval(deduction, Collections.singletonList(appId), number);
        return result;
    }

    private static String loadFileContent(String resourcePath) {
        try {
            File file = new ClassPathResource(resourcePath).getFile();
            Long fileLength = file.length();
            byte[] b = new byte[fileLength.intValue()];
            try (FileInputStream in = new FileInputStream(file)) {
                int length = in.read(b, 0, b.length);
                if (length == b.length || length == -1) { // 已经读满或者已经到了流的结尾
                    return new String(b, StandardCharsets.UTF_8);
                }
                int temp;
                while ((temp = in.read(b, length, b.length - length)) != -1) {
                    length += temp;
                    if (length == b.length) {
                        return new String(b, StandardCharsets.UTF_8);
                    }
                }
                return new String(b, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            return null;
        }
    }
}
