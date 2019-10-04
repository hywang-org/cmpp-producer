package iNetty.com.i;

import org.junit.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.JedisPoolConfig;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class operateRedis {
	@Test
	public void createRedisAppData() {
		Map<String, Object> appMap = new HashMap<String, Object>();
		appMap.put("max_connection", "4");
		appMap.put("app_secret", "123456");

		StringRedisTemplate temple = new StringRedisTemplate();
		temple.setConnectionFactory(
				connectionFactory("172.31.6.41", 6395, "UIX*$MD78p", 1000, 16, 0, 3000, false));
		temple.afterPropertiesSet();
//		temple.execute(new SessionCallback<Object>() {
//			@SuppressWarnings({ "rawtypes", "unchecked" })
//			public Object execute(RedisOperations operations) throws DataAccessException {
//				operations.opsForValue().set("test", "1");
//				return null;
//			}
//		});
//		temple.opsForHash().get("appId01", "speed_limit");
		temple.opsForHash().putAll("109010", appMap);
	}

	public RedisConnectionFactory connectionFactory(String hostName, int port, String password, int maxIdle,
	                                                int maxTotal, int index, long maxWaitMillis, boolean testOnBorrow) {
		JedisConnectionFactory jedis = new JedisConnectionFactory();
		jedis.setHostName(hostName);
		jedis.setPort(port);
		if (!StringUtils.isEmpty(password)) {
			jedis.setPassword(password);
		}
		if (index != 0) {
			jedis.setDatabase(index);
		}
		jedis.setPoolConfig(poolCofig(maxIdle, maxTotal, maxWaitMillis, testOnBorrow));
		jedis.afterPropertiesSet();
		// 初始化连接pool
		return jedis;
	}

	public JedisPoolConfig poolCofig(int maxIdle, int maxTotal, long maxWaitMillis, boolean testOnBorrow) {
		JedisPoolConfig poolCofig = new JedisPoolConfig();
		poolCofig.setMaxIdle(maxIdle);
		poolCofig.setMaxTotal(maxTotal);
		poolCofig.setMaxWaitMillis(maxWaitMillis);
		poolCofig.setTestOnBorrow(testOnBorrow);
		return poolCofig;
	}
}
