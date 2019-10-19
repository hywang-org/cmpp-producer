package com.i.server;

import com.i.server.consts.RedisConsts;
import com.i.server.data.mysql.entity.App;
import com.i.server.data.mysql.service.dao.SmsDao;
import com.i.server.data.redis.*;
import com.i.server.rabbitmq.service.RabbitmqService;
import com.zx.sms.connect.manager.EndpointManager;
import com.zx.sms.connect.manager.cmpp.CMPPServerEndpointEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
public class EchoServer {
	// private final int port;
	private Logger log = LoggerFactory.getLogger(this.getClass());
	private final EndpointManager manager = EndpointManager.INS;

	@Resource
	AppInfoRedis r1;

	@Resource
	ChannelInfoRedis r2;

	@Resource
	ProducerRedis r3;

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
//		acquire = loadFileContent("lua/acquire.lua");
//		System.out.println("serverId = " + serverId);
//		boolean result = r1.eval(acquire, Arrays.asList("MjOj2W_cn", "127.0.0.1"), 60000);
	}

//	private String loadFileContent(String resourcePath) {
//		try {
//			File file = new ClassPathResource(resourcePath).getFile();
//			Long fileLength = file.length();
//			byte[] b = new byte[fileLength.intValue()];
//			try (FileInputStream in = new FileInputStream(file)) {
//				int length = in.read(b, 0, b.length);
//				if (length == b.length || length == -1) { // 已经读满或者已经到了流的结尾
//					return new String(b, StandardCharsets.UTF_8);
//				}
//				int temp;
//				while ((temp = in.read(b, length, b.length - length)) != -1) {
//					length += temp;
//					if (length == b.length) {
//						return new String(b, StandardCharsets.UTF_8);
//					}
//				}
//				return new String(b, StandardCharsets.UTF_8);
//			}
//		} catch (IOException e) {
//			return null;
//		}
//	}

	public void openServer() {
		App app = smsDao.findSingle("from App where id = ?", 1l);
		System.out.println("app = "+app.getAppId());

		CMPPServerEndpointEntity server = new CMPPServerEndpointEntity();
		server.setId("server");
		server.setHost("127.0.0.1");
		server.setPort(7890);
		server.setValid(true);
		// 使用ssl加密数据流
		server.setUseSSL(false);
		server.setSmsDao(smsDao);

		Map<String, RedisOperationSets> redisOperationSetsMap = new HashMap<String, RedisOperationSets>();
		redisOperationSetsMap.put(RedisConsts.REDIS_APP_INFO, r1);
		redisOperationSetsMap.put(RedisConsts.REDIS_CHANNEL_INFO, r2);
		redisOperationSetsMap.put(RedisConsts.REDIS_PRODUCER, r3);
		// ly modify
		manager.setSmsDao(smsDao);

		manager.setRabbitmqService(rabbitmqService);

		manager.setRedisOperationSetsMap(redisOperationSetsMap);

		manager.setServerId(serverId);

		manager.addEndpointEntity(server);

		manager.openEndpoint(server);
	}
}