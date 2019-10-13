package com.i.server.data.redis.service;

import com.i.server.consts.RedisConsts;
import com.zx.sms.connect.manager.EndpointManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;

public class RedisService {
    private final static EndpointManager manager = EndpointManager.INS;
    static String deduction = loadFileContent("lua/deduction.lua");
    static String connection = loadFileContent("lua/connection.lua");

    public static int getMaxChannelByAppId(String appId) {
        System.out.println("getMaxChannelByAppId RedisService appId = " + appId);
        int maxNumber = Integer
                .parseInt(String.valueOf(manager.getRedisOperationSetsMap().get(RedisConsts.REDIS_APP_INFO)
                        .getRedisTemplate().opsForHash().get(appId, "max_connection")));
        System.out.println("maxNumber = " + maxNumber);
        return maxNumber;
    }

    public static long getSpeedLimitByAppId(String appId) {
        System.out.println("getSpeedLimitByAppId RedisService appId = " + appId);
        long speedLimit = Integer
                .parseInt(String.valueOf(manager.getRedisOperationSetsMap().get(RedisConsts.REDIS_APP_INFO)
                        .getRedisTemplate().opsForHash().get(appId, "speed_limit")));
        System.out.println("speedLimit = " + speedLimit);
        return speedLimit;
    }

    public static boolean addConn(String appId) {
        return manager.getRedisOperationSetsMap().get(RedisConsts.REDIS_APP_INFO).eval(connection, Collections.singletonList(appId), "1");
    }

    public static boolean deductConn(String appId) {
        return manager.getRedisOperationSetsMap().get(RedisConsts.REDIS_APP_INFO).eval(connection, Collections.singletonList(appId), "-1");
    }

    public static boolean deduct(String appId, String number) {
        return manager.getRedisOperationSetsMap().get(RedisConsts.REDIS_APP_INFO).eval(deduction, Collections.singletonList(appId), number);
    }

   /* private static String loadFileContent(String resourcePath) {
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
    }*/

    private static String loadFileContent(String resourcePath) {
        StringBuilder buffer = new StringBuilder();
        try {
            InputStream resourceAsStream = RedisService.class.getClassLoader().getResourceAsStream(resourcePath);
            if (resourceAsStream == null) {
                return null;
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(resourceAsStream));
            String line;
            while ((line = in.readLine()) != null) {
                buffer.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }
}
