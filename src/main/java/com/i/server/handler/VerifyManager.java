package com.i.server.handler;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.i.server.consts.RedisConsts;
import com.i.server.data.redis.RedisOperationSets;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitResponseMessage;
import com.zx.sms.connect.manager.EndpointManager;
import com.zx.sms.session.cmpp.SessionState;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

public class VerifyManager extends ChannelDuplexHandler {
	private static final Logger logger = LoggerFactory.getLogger(VerifyManager.class);

	private final EndpointManager manager = EndpointManager.INS;

	SessionState state = SessionState.DisConnect;

	String channelId = "";

	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (state == SessionState.DisConnect) {
			// logger.error("login error entity : " + entity.toString(), cause);
			ctx.close();
		} else {
			ctx.fireExceptionCaught(cause);
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		boolean isEnd = false;
		logger.info("inside VerifyManager channelRead");
		if (msg instanceof CmppSubmitRequestMessage) {
			logger.info("VerifyManager CmppSubmitRequestMessage msg = " + (CmppSubmitRequestMessage) msg);
		}

		// 如果是服务端，收到的第一个消息必须是Connect消息
		if (state == SessionState.Connect && isEnd) {
			logger.info("result = " + verifySpeedLimit("109002"));
			if (verifySpeedLimit("109002")) {
				isEnd = true;
			}
			if (msg instanceof CmppSubmitRequestMessage) {
				logger.info("CmppSubmitRequestMessage msg = " + (CmppSubmitRequestMessage) msg);
				CmppSubmitResponseMessage resp = new CmppSubmitResponseMessage(
						((CmppSubmitRequestMessage) msg).getHeader().getSequenceId());
				resp.setMsgId(((CmppSubmitRequestMessage) msg).getMsgid());
				resp.setResult(9);
				logger.info("server CmppSubmitResponseMessage = " + resp);
				// isEnd = true;
				ctx.channel().writeAndFlush(resp);
			}
		}
		if (!isEnd) {
			ctx.fireChannelRead(msg);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("VerifyManager channelInactive");
		// Channel ch = ctx.channel();
		//
		// if (state == SessionState.Connect) {
		// final EndpointConnector conn =
		// EndpointManager.INS.getEndpointConnector(entity);
		// if (conn != null)
		// conn.removeChannel(ch);
		// logger.warn("Connection closed . {} , connect count : {}", entity,
		// conn == null ? 0 : conn.getConnectionNum());
		// } else {
		// logger.warn("session is not created. the entity is {}.channel remote
		// is
		// {}", entity,
		// ctx.channel().remoteAddress());
		// }
		ctx.fireChannelInactive();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("VerifyManager channelActive");
		// if (state == SessionState.DisConnect) {
		// // 客户端必须先发起Connect消息
		// if (entity instanceof ClientEndpoint) {
		//
		// doLogin(ctx.channel());
		//
		// }
		// }
		ctx.fireChannelActive();
	}

	private boolean verifySpeedLimit(String appId) {
		RedisOperationSets redisTemplate = manager.getRedisOperationSetsMap().get(RedisConsts.REDIS_SPEED_LIMIT);
		if (Objects.isNull(redisTemplate.get(appId))) {
			RedisOperationSets appInfoRedis = manager.getRedisOperationSetsMap().get(RedisConsts.REDIS_APP_INFO);
			String appInfoStr = appInfoRedis.get(appId);
			Long speedLimit = Long.valueOf(JSON.parseObject(appInfoStr).getString("SpeedLimit"));
			try {
				redisTemplate.setValue(appId, speedLimit + "", 60);
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
			logger.error("need add process mysql logic");
		}

		Long speedLeft = redisTemplate.decrement(appId, 1);
		if (speedLeft < 0) {
			return false;
		} else {
			return true;
		}
	}
}
