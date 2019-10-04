package com.zx.sms.session;

import com.i.server.handler.CMPPMessageReceiveHandlerAsServer;
import com.zx.sms.codec.cmpp.msg.CmppConnectRequestMessage;
import com.zx.sms.common.GlobalConstance;
import com.zx.sms.connect.manager.ClientEndpoint;
import com.zx.sms.connect.manager.EndpointConnector;
import com.zx.sms.connect.manager.EndpointEntity;
import com.zx.sms.connect.manager.EndpointManager;
import com.zx.sms.connect.manager.cmpp.CMPPServerChildEndpointEntity;
import com.zx.sms.connect.manager.cmpp.CMPPServerEndpointEntity;
import com.zx.sms.handler.api.BusinessHandlerInterface;
import com.zx.sms.session.cmpp.SessionState;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 处理客户端或者服务端登陆，密码校验。协议协商 建立连接前，不会启动消息重试和消息可靠性保证
 */
public abstract class AbstractSessionLoginManager extends ChannelDuplexHandler {
	private static final Logger logger = LoggerFactory.getLogger(AbstractSessionLoginManager.class);

	protected EndpointEntity entity;
	private final EndpointManager manager = EndpointManager.INS;

	/**
	 * 连接状态
	 **/
	protected SessionState state = SessionState.DisConnect;

	protected String appId = null;

	protected String channelId = null;

	public AbstractSessionLoginManager(EndpointEntity entity) {
		this.entity = entity;
	}

	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (state == SessionState.DisConnect) {
			logger.error("login error entity : " + entity.toString(), cause);
			ctx.close();
		} else {
			ctx.fireExceptionCaught(cause);
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		System.out.println("cmppConnectRequestMsg channelRead = " + msg);

		// 如果是服务端，收到的第一个消息必须是Connect消息
		if (state == SessionState.DisConnect) {
			if (entity instanceof ClientEndpoint) {
				// 客户端收到的第一个消息应该是ConnectResp消息
				receiveConnectResponseMessage(ctx, msg);
			} else {
				receiveConnectMessage(ctx, msg);
			}
			CmppConnectRequestMessage cmppConnectRequestMsg = (CmppConnectRequestMessage) msg;

			// if (cmppConnectRequestMsg != null) {
			// appId = cmppConnectRequestMsg.getSourceAddr();
			// System.out.println("appId = " + appId);
			// }
			//
			// channelId = ctx.channel().id().asLongText();

			System.out.println("AbstractSessionLoginManager channel id = " + channelId);
			manager.addChannelByAppIdChannelId(appId, ctx.channel().id().asLongText(), ctx.channel());
			// 123
		}

		ctx.fireChannelRead(msg);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("123！！！");
		Channel ch = ctx.channel();

		if (state == SessionState.Connect) {
			final EndpointConnector conn = EndpointManager.INS.getEndpointConnector(entity);
			if (conn != null)
				conn.removeChannel(ch);
			logger.warn("Connection closed . {} , connect count : {}", entity,
					conn == null ? 0 : conn.getConnectionNum());
		} else {
			logger.warn("session is not created. the entity is {}.channel remote is {}", entity,
					ctx.channel().remoteAddress());
		}
		if(appId != null) {
			manager.removeChannelByAppIdChannelId(appId, ch.id().asLongText());
		} else {
			logger.info("channelInactive but appId is null");
		}
		ctx.fireChannelInactive();
	}

	protected abstract void doLogin(Channel ch);

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("AbstractSessionLoginManager channelActive state = " + state);
		if (state == SessionState.DisConnect) {
			// 客户端必须先发起Connect消息
			// ly 本服务属于服务端，不是与运营商建连的客户端，所以注释下面代码
			// if (entity instanceof ClientEndpoint) {
			// doLogin(ctx.channel());
			// manager.addChannelByAppId(appId, ctx.channel());
			// }

		}
		ctx.fireChannelActive();
	}

	protected abstract EndpointEntity queryEndpointEntityByMsg(Object msg);

	protected abstract boolean validAddressHost(EndpointEntity childentity, Channel channel);

	protected abstract int validClientMsg(EndpointEntity entity, Object message);

	protected abstract int validClientMsg2(Object message);

	protected abstract int validServermsg(Object message);

	protected abstract void changeProtoVersion(ChannelHandlerContext ctx, EndpointEntity entity, Object message)
			throws Exception;

	protected abstract void doLoginSuccess(ChannelHandlerContext ctx, EndpointEntity entity, Object message);

	protected abstract void failedLogin(ChannelHandlerContext ctx, Object message, long status);

	// ly modify
	protected void receiveConnectMessage(ChannelHandlerContext ctx, Object message) throws Exception {

		// 服务端收到Request，校验用户名密码成功
		int status = validClientMsg2(message);
		String channelId = ctx.channel().id().asLongText();
		System.out.println("status = " + status + ", ctx channel id = " + channelId);

		// 通过用户名获取端口信息

		// 认证成功
		if (status == 0) {
			EndpointEntity childentity = queryEndpointEntityByMsg(message);
			if (childentity == null) {
				childentity = createChild((CmppConnectRequestMessage) message, channelId);
				if (childentity == null){
					failedLogin(ctx, message, 3);
				return;
				}
			}

			// 修改协议版本，使用客户端对应协议的协议解析器
			changeProtoVersion(ctx, childentity, message);

			if (!validAddressHost(childentity, ctx.channel())) {
				failedLogin(ctx, message, 2);
				return;
			}

			// 绑定端口为对应账号的端口
			entity = childentity;

			// 打开连接，并把连接加入管理 器
			EndpointManager.INS.openEndpoint(childentity);
			// 端口已打开，获取连接器
			EndpointConnector conn = EndpointManager.INS.getEndpointConnector(childentity);

			if (conn == null) {
				logger.warn("entity may closed. {}", childentity);
				failedLogin(ctx, message, 5);
				return;
			}

			// 检查是否超过最大连接数
			if (conn.addChannel(ctx.channel())) {
				IdleStateHandler idlehandler = (IdleStateHandler) ctx.pipeline()
						.get(GlobalConstance.IdleCheckerHandlerName);
				ctx.pipeline().replace(idlehandler, GlobalConstance.IdleCheckerHandlerName,
						new IdleStateHandler(0, 0, childentity.getIdleTimeSec(), TimeUnit.SECONDS));
				state = SessionState.Connect;

				// channelHandler已绑定完成，给客户端发resp.
				doLoginSuccess(ctx, childentity, message);

				// 通知业务handler连接已建立完成
				notifyChannelConnected(ctx);
				logger.info("{} login success on channel {}", childentity.getId(), ctx.channel());
			} else {
				// 超过最大连接数了
				failedLogin(ctx, message, 5);
			}
		} else {
			failedLogin(ctx, message, status);
		}
	}

	private EndpointEntity createChild(CmppConnectRequestMessage message, String channelId) {
		appId = message.getSourceAddr();
		this.channelId = channelId;
		CMPPServerChildEndpointEntity child = new CMPPServerChildEndpointEntity();
		child.setId(appId);
		child.setChartset(Charset.forName("utf-8"));
		child.setGroupName(appId);
		// child.setUserName("901783");
		// child.setPassword("ICP001");
		child.setUserName(appId);
		child.setPassword("123456");

		child.setValid(true);
		child.setVersion((short) 0x20);

		child.setMaxChannels((short) 2);
		child.setRetryWaitTimeSec((short) 30);
		child.setMaxRetryCnt((short) 3);
		child.setReSendFailMsg(true);
		// child.setWriteLimit(200);
		// child.setReadLimit(200);
		List<BusinessHandlerInterface> serverhandlers = new ArrayList<BusinessHandlerInterface>();
		serverhandlers.add(new CMPPMessageReceiveHandlerAsServer(manager.getRabbitmqService(),manager.getSmsDao(), appId, channelId));
		child.setBusinessHandlerSet(serverhandlers);

		// child.setRedisOperationSets(r1);
		CMPPServerEndpointEntity server = (CMPPServerEndpointEntity) manager.getEndpointEntity("server");
		server.addchild(child);

		return child;
	}

	// protected void receiveConnectMessage(ChannelHandlerContext ctx, Object
	// message) throws Exception {
	//
	// // 通过用户名获取端口信息
	// EndpointEntity childentity = queryEndpointEntityByMsg(message);
	// if (childentity == null) {
	// failedLogin(ctx, message, 3);
	// return;
	// }
	//
	// // 修改协议版本，使用客户端对应协议的协议解析器
	// changeProtoVersion(ctx, childentity, message);
	//
	// if (!validAddressHost(childentity, ctx.channel())) {
	// failedLogin(ctx, message, 2);
	// return;
	// }
	//
	// // 服务端收到Request，校验用户名密码成功
	// int status = validClientMsg(childentity, message);
	// // 认证成功
	// if (status == 0) {
	// // 绑定端口为对应账号的端口
	// entity = childentity;
	//
	// // 打开连接，并把连接加入管理 器
	// EndpointManager.INS.openEndpoint(childentity);
	// // 端口已打开，获取连接器
	// EndpointConnector conn =
	// EndpointManager.INS.getEndpointConnector(childentity);
	//
	// if (conn == null) {
	// logger.warn("entity may closed. {}", childentity);
	// failedLogin(ctx, message, 5);
	// return;
	// }
	//
	// // 检查是否超过最大连接数
	// if (conn.addChannel(ctx.channel())) {
	// IdleStateHandler idlehandler = (IdleStateHandler) ctx.pipeline()
	// .get(GlobalConstance.IdleCheckerHandlerName);
	// ctx.pipeline().replace(idlehandler,
	// GlobalConstance.IdleCheckerHandlerName,
	// new IdleStateHandler(0, 0, childentity.getIdleTimeSec(),
	// TimeUnit.SECONDS));
	// state = SessionState.Connect;
	//
	// // channelHandler已绑定完成，给客户端发resp.
	// doLoginSuccess(ctx, childentity, message);
	//
	// // 通知业务handler连接已建立完成
	// notifyChannelConnected(ctx);
	// logger.info("{} login success on channel {}", childentity.getId(),
	// ctx.channel());
	// } else {
	// // 超过最大连接数了
	// failedLogin(ctx, message, 5);
	// }
	// } else {
	// failedLogin(ctx, message, status);
	// }
	// }

	/**
	 * 状态 0：正确 1：消息结构错 2：非法源地址 3：认证错 4：版本太高 5~ ：其他错误
	 */

	private void receiveConnectResponseMessage(ChannelHandlerContext ctx, Object message) throws Exception {
		int status = validServermsg(message);
		if (status == 0) {

			EndpointConnector conn = EndpointManager.INS.getEndpointConnector(entity);
			if (conn == null) {
				logger.warn("entity may closed. {}", entity);
				ctx.close();
				return;
			}

			if (conn.addChannel(ctx.channel())) {
				state = SessionState.Connect;
				// 如果没有超过最大连接数配置，建立连接
				notifyChannelConnected(ctx);
				logger.info("{} login success on channel {}", entity.getId(), ctx.channel());
			} else {
				ctx.close();
				return;
			}

		} else {
			logger.info("{} login failed (status = {}) on channel {}", entity.getId(), status, ctx.channel());
			ctx.close();
			return;
		}
	}

	private void notifyChannelConnected(ChannelHandlerContext ctx) {
		// 通知业务handler连接已建立完成
		ctx.channel().pipeline().fireUserEventTriggered(SessionState.Connect);
	}

}
