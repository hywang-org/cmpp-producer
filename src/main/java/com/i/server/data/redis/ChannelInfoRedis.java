package com.i.server.data.redis;

import com.i.server.data.redis.RedisOperationSets;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Repository
public class ChannelInfoRedis extends RedisOperationSets {
	@Resource(name = "redisChannelInfo")
	private RedisTemplate<String, Object> redisTemplate;

	@PostConstruct
	public void Redis1() {
		super.setRedisTemplate(redisTemplate);
	}
}
