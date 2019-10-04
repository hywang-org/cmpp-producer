package com.i.server.data.redis.service;

import com.i.server.consts.RedisConsts;
import com.zx.sms.connect.manager.EndpointManager;

public class RedisService {
    private final static EndpointManager manager = EndpointManager.INS;

    public static int getMaxChannelByAppId(String appId) {
        System.out.println("RedisService appId = " + appId);
        int maxNumber = Integer
                .parseInt(String.valueOf(manager.getRedisOperationSetsMap().get(RedisConsts.REDIS_APP_INFO)
                        .getRedisTemplate().opsForHash().get(appId, "max_connection")));
        System.out.println("maxNumber = " + maxNumber);
        return maxNumber;
    }
}
