package com.i.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.i.server.consts.RedisConsts;
import com.i.server.data.mysql.service.dao.SmsDao;
import com.i.server.data.redis.AppInfoRedis;
import com.i.server.data.redis.ProducerRedis;
import com.i.server.data.redis.RedisOperationSets;
import com.i.server.data.redis.SpeedLimitRedis;
import com.i.server.data.redis.ValidateClientRedis;
import com.i.server.rabbitmq.service.RabbitmqService;
import com.zx.sms.connect.manager.EndpointManager;
import com.zx.sms.connect.manager.cmpp.CMPPServerEndpointEntity;

@Service
public class EchoServer {
	// private final int port;
	private Logger log = LoggerFactory.getLogger(this.getClass());
	private final EndpointManager manager = EndpointManager.INS;

	@Resource
	ValidateClientRedis r1;

	@Resource
	SpeedLimitRedis r2;

	@Resource
	AppInfoRedis r3;

	@Resource
	ProducerRedis r4;

	@Resource
	SmsDao smsDao;

	@Resource
	private RabbitmqService rabbitmqService;

	@Value("${serverId}")
	private String serverId;

	String acquire;

	@PostConstruct
	public void EchoServer() {
		openServer();
		acquire = loadFileContent("lua/acquire.lua");
		System.out.println("serverId = " + serverId);
		boolean result = r1.eval(acquire, Arrays.asList("MjOj2W_cn", "127.0.0.1"), 60000);
	}

	private String loadFileContent(String resourcePath) {
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

	public void openServer() {
		CMPPServerEndpointEntity server = new CMPPServerEndpointEntity();
		server.setId("server");
		server.setHost("127.0.0.1");
		server.setPort(7890);
		server.setValid(true);
		// 使用ssl加密数据流
		server.setUseSSL(false);

		// CMPPServerChildEndpointEntity child = new
		// CMPPServerChildEndpointEntity();
		// child.setId("109002");
		// child.setChartset(Charset.forName("utf-8"));
		// child.setGroupName("test");
		// // child.setUserName("901783");
		// // child.setPassword("ICP001");
		// child.setUserName("109002");
		// child.setPassword("Aa123456");
		//
		// child.setValid(true);
		// child.setVersion((short) 0x20);
		//
		// child.setMaxChannels((short) 2);
		// child.setRetryWaitTimeSec((short) 30);
		// child.setMaxRetryCnt((short) 3);
		// child.setReSendFailMsg(true);
		// // child.setWriteLimit(200);
		// // child.setReadLimit(200);
		// List<BusinessHandlerInterface> serverhandlers = new
		// ArrayList<BusinessHandlerInterface>();
		// serverhandlers.add(new
		// CMPPMessageReceiveHandlerAsServer(rabbitmqService));
		// child.setBusinessHandlerSet(serverhandlers);
		//
		// child.setRedisOperationSets(r1);
		// server.addchild(child);

		Map<String, RedisOperationSets> redisOperationSetsMap = new HashMap<String, RedisOperationSets>();
		redisOperationSetsMap.put(RedisConsts.REDIS_VALIDATE_CLINET, r1);
		redisOperationSetsMap.put(RedisConsts.REDIS_SPEED_LIMIT, r2);
		redisOperationSetsMap.put(RedisConsts.REDIS_APP_INFO, r3);
		redisOperationSetsMap.put(RedisConsts.REDIS_PRODUCER, r4);
		// ly modify
		manager.setSmsDao(smsDao);

		manager.setRabbitmqService(rabbitmqService);

		manager.setRedisOperationSetsMap(redisOperationSetsMap);

		manager.setServerId(serverId);

		manager.addEndpointEntity(server);

		manager.openEndpoint(server);
	}
}