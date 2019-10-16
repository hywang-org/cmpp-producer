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
		appMap.put("max_connection", "40");
		appMap.put("now_connection", "4");
		appMap.put("total_num", "1000");
		appMap.put("used_num", "0");
		appMap.put("app_secret", "123456");
		appMap.put("speed_limit", "0");

		StringRedisTemplate temple = new StringRedisTemplate();
		temple.setConnectionFactory(
				connectionFactory("172.31.6.41", 6395, "UIX*$MD78p", 1000, 16, 0, 3000, false));
		temple.afterPropertiesSet();

		temple.opsForHash().putAll("test1", appMap);
	}

	@Test
	public void createRedisChannelData() {
		Map<String, Object> appMap = new HashMap<String, Object>();
		appMap.put("sp_name", "106909009002");
		appMap.put("sp_type", "0");
		appMap.put("sp_status", "0");
		appMap.put("sp_connect_status", "0");
		appMap.put("sp_ip", "121.41.46.165");
		appMap.put("sp_port", "7890");
		appMap.put("sp_login_name", "HF9002");
		appMap.put("sp_login_pwd", "Aa123456");

		StringRedisTemplate temple = new StringRedisTemplate();
		temple.setConnectionFactory(
				connectionFactory("172.31.6.41", 6395, "UIX*$MD78p", 1000, 16, 1, 3000, false));
		temple.afterPropertiesSet();

		temple.opsForHash().putAll("109002", appMap);
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
