package com.zx.sms.connect.manager;

import com.i.server.data.mysql.service.dao.SmsDao;
import com.i.server.data.redis.RedisOperationSets;
import com.i.server.rabbitmq.service.RabbitmqService;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Lihuanghe(18852780@qq.com) 系统连接的统一管理器，负责连接服务端，或者开启监听端口，等客户端连接 。
 */
public enum EndpointManager implements EndpointManagerInterface {
	INS;
	private static final Logger logger = LoggerFactory.getLogger(EndpointManager.class);

	private Set<EndpointEntity> endpoints = Collections.synchronizedSet(new HashSet<EndpointEntity>());

	private ConcurrentHashMap<String, EndpointEntity> idMap = new ConcurrentHashMap<String, EndpointEntity>();

	private ConcurrentHashMap<String, EndpointConnector<?>> map = new ConcurrentHashMap<String, EndpointConnector<?>>();

	private ConcurrentHashMap<String, Map<String, Channel>> appChannelInfomap = new ConcurrentHashMap<String, Map<String, Channel>>();

	private volatile boolean started = false;

	private SmsDao smsDao;

	private RabbitmqService rabbitmqService;

	private String serverId;

	public void setSmsDao(SmsDao smsDao) {
		this.smsDao = smsDao;
	}

	public SmsDao getSmsDao() {
		return smsDao;
	}

	public RabbitmqService getRabbitmqService() {
		return rabbitmqService;
	}

	public void setRabbitmqService(RabbitmqService rabbitmqService) {
		this.rabbitmqService = rabbitmqService;
	}

	public String getServerId() {
		return serverId;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	public synchronized void openEndpoint(EndpointEntity entity) {
		if (!entity.isValid())
			return;

		EndpointEntity old = idMap.get(entity.getId());
		if (old == null) {
			addEndpointEntity(entity);
		}

		EndpointConnector<?> conn = map.get(entity.getId());
		if (conn == null) {
			conn = entity.buildConnector();
			map.put(entity.getId(), conn);
		}

		try {
			conn.open();
		} catch (Exception e) {
			logger.error("Open Endpoint Error. {}", entity, e);
		}
	}

	public synchronized void close(EndpointEntity entity) {
		EndpointConnector<?> conn = map.get(entity.getId());
		if (conn == null)
			return;
		try {
			conn.close();
			// 关闭所有连接，并把Connector删掉
			map.remove(entity.getId());

		} catch (Exception e) {
			logger.error("close Error", e);
		}
	}

	public Channel getChannelByAppId(String appId, String channelId) {
		Channel channel = null;
		Map<String, Channel> channelMap = appChannelInfomap.get(appId);
		if (channelMap != null) {
			channel = channelMap.get(channelId);
		}
		return channel;
	}

	public void addChannelByAppIdChannelId(String appId, String channelId, Channel channel) {
		logger.info("addChannelByAppIdChannelId, appId = {} and channelId = {}", appId, channelId);
		if (appChannelInfomap !=null && appId != null){
			Map<String, Channel> channelMap = appChannelInfomap.get(appId);
			if (channelMap != null) {
				channelMap.put(channelId, channel);
			} else {
				channelMap = new HashMap<String, Channel>();
				channelMap.put(channelId, channel);
			}
		}else {
			logger.info("appId or appChannelInfomap is null, appId = {}",appId);
		}
	}

	public void removeChannelByAppIdChannelId(String appId, String channelId) {
		Map<String, Channel> channelMap = appChannelInfomap.get(appId);
		if (channelMap != null) {
			channelMap.remove(channelId);
		}
	}

	public EndpointConnector<?> getEndpointConnector(EndpointEntity entity) {
		return map.get(entity.getId());
	}

	public EndpointConnector<?> getEndpointConnector(String entityId) {
		return map.get(entityId);
	}

	public EndpointEntity getEndpointEntity(String id) {
		return idMap.get(id);
	}

	public void openAll() throws Exception {
		for (EndpointEntity e : endpoints)
			openEndpoint(e);
	}

	public synchronized void addEndpointEntity(EndpointEntity entity) {
		endpoints.add(entity);
		idMap.put(entity.getId(), entity);
	}

	public void addAllEndpointEntity(List<EndpointEntity> entities) {
		if (entities == null || entities.size() == 0)
			return;
		for (EndpointEntity entity : entities) {
			if (entity.isValid())
				addEndpointEntity(entity);
		}
	}

	public Set<EndpointEntity> allEndPointEntity() {
		return endpoints;
	}

	@Override
	public synchronized void remove(String id) {
		EndpointEntity entity = idMap.remove(id);
		if (entity != null) {
			endpoints.remove(entity);
			close(entity);
		}
	}

	public void close() {
		for (EndpointEntity en : endpoints) {
			close(en);
		}
	}

	public void stopConnectionCheckTask() {
		started = false;
	}

	public void startConnectionCheckTask() {
		if (started)
			return;

		started = true;
		// 每秒检查一次所有连接，不足数目的就新建一个连接
		EventLoopGroupFactory.INS.submitUnlimitCircleTask(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				for (Map.Entry<String, EndpointConnector<?>> entry : map.entrySet()) {
					EndpointConnector conn = entry.getValue();
					EndpointEntity entity = conn.getEndpointEntity();
					int max = entity.getMaxChannels();
					int actual = conn.getConnectionNum();

					// 客户端重连
					if (entity instanceof ClientEndpoint && actual < max) {
						logger.debug("open connection {}", entity);
						conn.open();
					}
				}
				return started;
			}

		}, new ExitUnlimitCirclePolicy<Boolean>() {

			@Override
			public boolean notOver(Future<Boolean> future) {
				return started;
			}

		}, 1000);
	}

	// ly modify
	private Map<String, RedisOperationSets> redisOperationSetsMap;

	public Map<String, RedisOperationSets> getRedisOperationSetsMap() {
		return redisOperationSetsMap;
	}

	public void setRedisOperationSetsMap(Map<String, RedisOperationSets> redisOperationSetsMap) {
		this.redisOperationSetsMap = redisOperationSetsMap;
	}

}
