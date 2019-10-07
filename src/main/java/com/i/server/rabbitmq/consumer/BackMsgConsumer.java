
package com.i.server.rabbitmq.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.i.server.consts.Consts;
import com.i.server.rabbitmq.service.MqEntity;
import com.i.server.rabbitmq.service.RabbitmqService;
import com.rabbitmq.client.*;
import com.zx.sms.codec.cmpp.msg.CmppDeliverRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppDeliverRequestSelfDefinedMessage;
import com.zx.sms.codec.cmpp.msg.CmppReportRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestMessage;
import com.zx.sms.codec.cmpp.packet.CmppDeliverRequest;
import com.zx.sms.codec.cmpp.packet.CmppReportRequest;
import com.zx.sms.codec.cmpp.wap.LongMessageFrameHolder;
import com.zx.sms.codec.cmpp20.packet.Cmpp20DeliverRequest;
import com.zx.sms.codec.cmpp20.packet.Cmpp20ReportRequest;
import com.zx.sms.common.GlobalConstance;
import com.zx.sms.common.util.DefaultMsgIdUtil;
import com.zx.sms.common.util.MsgId;
import com.zx.sms.connect.manager.EndpointConnector;
import com.zx.sms.connect.manager.EndpointManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import org.marre.sms.SmsDcs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;

import static com.zx.sms.common.util.NettyByteBufUtil.toArray;

public class BackMsgConsumer extends DefaultConsumer {

	private final EndpointManager manager = EndpointManager.INS;

	public BackMsgConsumer(Channel channel) {
		super(channel);
		// TODO Auto-generated constructor stub
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(BackMsgConsumer.class);

	private RabbitmqService rabbitmqService;

	private String queueName;

	// private RedisService redisService;
	//
	// private APIService apiService;
	//
	// private YPDao ypDao;

	private long waitTime;

	// public AppConvertConsumer(RabbitmqService rabbitmqService, String
	// queueName, Channel channel,
	// RedisService redisService, APIService apiService, YPDao ypDao, long
	// waitTime,
	// DeleteOrderService deleteOrderService) {
	// super(channel);
	// this.rabbitmqService = rabbitmqService;
	// this.queueName = queueName;
	// this.redisService = redisService;
	// this.apiService = apiService;
	// this.ypDao = ypDao;
	// this.waitTime = waitTime;
	// this.deleteOrderService = deleteOrderService;
	// }

	@Override
	public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
			throws IOException {
		// String test = JSONArray.toJSONString(body);
		System.out.println("handleDelivery backmsg ");
		String test = new String(body, "UTF-8");
		System.out.println("test = " + JSON.toJSONString(test));

		MqEntity mqEntity = JSON.parseObject(test, new TypeReference<MqEntity>() {
		});
		String cmppMsgType = mqEntity.getCmppMsgType();
		String cmppVersion = mqEntity.getCmppVersion();
		switch (cmppMsgType) {
		case Consts.CMPP_DELIVER_REQUEST_MESSAGE:
			CmppDeliverRequestSelfDefinedMessage cmppDeliverRequestSelfDefinedMessage = (CmppDeliverRequestSelfDefinedMessage) mqEntity
					.getObj();
			CmppDeliverRequestMessage cmppObj = formCmppMessage(cmppDeliverRequestSelfDefinedMessage, cmppVersion,
					mqEntity.getMsgId());
			String appId = mqEntity.getAppId();
			String channelId = mqEntity.getChannelId();
			io.netty.channel.Channel channel = manager.getChannelByAppId(appId, channelId);
			System.out.println("handleDelivery backmsg = " + mqEntity.getMsgId());
			if (channel != null) {
				channel.writeAndFlush(cmppObj);
			}
		}
		this.getChannel().basicAck(envelope.getDeliveryTag(), false);
	}

	private void sendSms(Object obj) {
		EndpointConnector<?> connector = EndpointManager.INS.getEndpointConnector("109002");
		while (true) {
			LOGGER.info("ready publish from consumer," + obj);
			ChannelFuture write = connector.asynwrite(obj);
			if (write != null) {
				break;
			} else {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		;
	}

	private CmppDeliverRequestMessage formCmppMessage(CmppDeliverRequestSelfDefinedMessage selfDefinedMessage,
			String cmppVersion, MsgId msgId) {
		CmppDeliverRequestMessage cmppDeliverRequestMessage = null;
		switch (cmppVersion) {
		case Consts.TYPE20:
			cmppDeliverRequestMessage = formCmppMessage20(selfDefinedMessage, msgId);
			break;
		default:
			cmppDeliverRequestMessage = formCmppMessage30(selfDefinedMessage, msgId);
			break;
		}
		return cmppDeliverRequestMessage;
	}

	private CmppDeliverRequestMessage formCmppMessage30(CmppDeliverRequestSelfDefinedMessage selfDefinedMessage,
			MsgId msgId) {

		// ly add
		// requestMessage.setMsgid(DefaultMsgIdUtil.bytes2MsgId(toArray(bodyBuffer,
		// CmppSubmitRequest.MSGID.getLength())));
		// DefaultMsgIdUtil.bytes2MsgId(toArray(bodyBuffer,
		// CmppSubmitRequest.MSGID.getLength()));
		// requestMessage.setMsgid(msgId);
		CmppDeliverRequestMessage requestMessage = new CmppDeliverRequestMessage(selfDefinedMessage.getHeader());

		ByteBuf bodyBuffer = Unpooled.wrappedBuffer(selfDefinedMessage.getBodyBuffer());
		requestMessage
				.setMsgId(DefaultMsgIdUtil.bytes2MsgId(toArray(bodyBuffer, CmppDeliverRequest.MSGID.getLength())));
		requestMessage.setDestId(bodyBuffer
				.readCharSequence(CmppDeliverRequest.DESTID.getLength(), GlobalConstance.defaultTransportCharset)
				.toString().trim());
		requestMessage.setServiceid(bodyBuffer
				.readCharSequence(CmppDeliverRequest.SERVICEID.getLength(), GlobalConstance.defaultTransportCharset)
				.toString().trim());

		requestMessage.setTppid(bodyBuffer.readUnsignedByte());
		requestMessage.setTpudhi(bodyBuffer.readUnsignedByte());
		requestMessage.setMsgfmt(new SmsDcs((byte) bodyBuffer.readUnsignedByte()));

		requestMessage.setSrcterminalId(bodyBuffer
				.readCharSequence(CmppDeliverRequest.SRCTERMINALID.getLength(), GlobalConstance.defaultTransportCharset)
				.toString().trim());
		requestMessage.setSrcterminalType(bodyBuffer.readUnsignedByte());

		short registeredDelivery = bodyBuffer.readUnsignedByte();

		int frameLength = LongMessageFrameHolder.getPayloadLength(requestMessage.getMsgfmt().getAlphabet(),
				bodyBuffer.readUnsignedByte());

		if (registeredDelivery == 0) {
			byte[] contentbytes = new byte[frameLength];
			bodyBuffer.readBytes(contentbytes);
			requestMessage.setMsgContentBytes(contentbytes);
			requestMessage.setMsgLength((short) frameLength);
		} else {
			if (frameLength != CmppReportRequest.DESTTERMINALID.getBodyLength()) {
				// logger.warn("CmppDeliverRequestMessage - MsgContent length is
				// {}. should be {}.", frameLength,
				// CmppReportRequest.DESTTERMINALID.getBodyLength());
				System.out.println("CmppDeliverRequestMessage - MsgContent length is " + frameLength + ", should be "
						+ CmppReportRequest.DESTTERMINALID.getBodyLength());
			}
			;
			requestMessage.setReportRequestMessage(new CmppReportRequestMessage());
			// ly modify
			DefaultMsgIdUtil.bytes2MsgId(toArray(bodyBuffer, CmppReportRequest.MSGID.getLength()));
			requestMessage.getReportRequestMessage().setMsgId(msgId);
			requestMessage.getReportRequestMessage().setStat(bodyBuffer
					.readCharSequence(CmppReportRequest.STAT.getLength(), GlobalConstance.defaultTransportCharset)
					.toString().trim());
			requestMessage.getReportRequestMessage().setSubmitTime(bodyBuffer
					.readCharSequence(CmppReportRequest.SUBMITTIME.getLength(), GlobalConstance.defaultTransportCharset)
					.toString().trim());
			requestMessage.getReportRequestMessage().setDoneTime(bodyBuffer
					.readCharSequence(CmppReportRequest.DONETIME.getLength(), GlobalConstance.defaultTransportCharset)
					.toString().trim());
			requestMessage.getReportRequestMessage()
					.setDestterminalId(bodyBuffer.readCharSequence(CmppReportRequest.DESTTERMINALID.getLength(),
							GlobalConstance.defaultTransportCharset).toString().trim());
			requestMessage.getReportRequestMessage().setSmscSequence(bodyBuffer.readUnsignedInt());
		}
		// 卓望发送的状态报告 少了11个字节， 剩下的字节全部读取
		requestMessage.setLinkid(
				bodyBuffer.readCharSequence(bodyBuffer.readableBytes(), GlobalConstance.defaultTransportCharset)
						.toString().trim());
		return requestMessage;
	}

	private CmppDeliverRequestMessage formCmppMessage20(CmppDeliverRequestSelfDefinedMessage selfDefinedMessage,
			MsgId msgId) {

		// ly add
		// requestMessage
		// .setMsgid(DefaultMsgIdUtil.bytes2MsgId(toArray(bodyBuffer,
		// Cmpp20SubmitRequest.MSGID.getLength())));
		CmppDeliverRequestMessage requestMessage = new CmppDeliverRequestMessage(selfDefinedMessage.getHeader());

		ByteBuf bodyBuffer = Unpooled.wrappedBuffer(selfDefinedMessage.getBodyBuffer());

		requestMessage
				.setMsgId(DefaultMsgIdUtil.bytes2MsgId(toArray(bodyBuffer, Cmpp20DeliverRequest.MSGID.getLength())));
		requestMessage.setDestId(bodyBuffer
				.readCharSequence(Cmpp20DeliverRequest.DESTID.getLength(), GlobalConstance.defaultTransportCharset)
				.toString().trim());
		requestMessage.setServiceid(bodyBuffer
				.readCharSequence(Cmpp20DeliverRequest.SERVICEID.getLength(), GlobalConstance.defaultTransportCharset)
				.toString().trim());

		requestMessage.setTppid(bodyBuffer.readUnsignedByte());
		requestMessage.setTpudhi(bodyBuffer.readUnsignedByte());
		requestMessage.setMsgfmt(new SmsDcs((byte) bodyBuffer.readUnsignedByte()));

		requestMessage.setSrcterminalId(bodyBuffer.readCharSequence(Cmpp20DeliverRequest.SRCTERMINALID.getLength(),
				GlobalConstance.defaultTransportCharset).toString().trim());

		// requestMessage.setSrcterminalType(bodyBuffer.readUnsignedByte());//CMPP2.0
		// SrcterminalType不进行编解码
		short registeredDelivery = bodyBuffer.readUnsignedByte();
		short frameLength = (short) (LongMessageFrameHolder.getPayloadLength(requestMessage.getMsgfmt().getAlphabet(),
				bodyBuffer.readUnsignedByte()) & 0xffff);

		if (registeredDelivery == 0) {
			byte[] contentbytes = new byte[frameLength];
			bodyBuffer.readBytes(contentbytes);
			requestMessage.setMsgContentBytes(contentbytes);
			requestMessage.setMsgLength((short) frameLength);
		} else {
			if (frameLength != Cmpp20ReportRequest.DESTTERMINALID.getBodyLength()) {
				// logger.warn("CmppDeliverRequestMessage20 - MsgContent length
				// is {}. should be {}.", frameLength,
				// Cmpp20ReportRequest.DESTTERMINALID.getBodyLength());
				System.out.println("CmppDeliverRequestMessage20 - MsgContent length is " + frameLength + ", should be "
						+ Cmpp20ReportRequest.DESTTERMINALID.getBodyLength());
			}
			;
			requestMessage.setReportRequestMessage(new CmppReportRequestMessage());
			// ly modify
			DefaultMsgIdUtil.bytes2MsgId(toArray(bodyBuffer, Cmpp20ReportRequest.MSGID.getLength()));
			requestMessage.getReportRequestMessage().setMsgId(msgId);
			requestMessage.getReportRequestMessage().setStat(bodyBuffer
					.readCharSequence(Cmpp20ReportRequest.STAT.getLength(), GlobalConstance.defaultTransportCharset)
					.toString().trim());
			requestMessage.getReportRequestMessage()
					.setSubmitTime(bodyBuffer.readCharSequence(Cmpp20ReportRequest.SUBMITTIME.getLength(),
							GlobalConstance.defaultTransportCharset).toString().trim());
			requestMessage.getReportRequestMessage().setDoneTime(bodyBuffer
					.readCharSequence(Cmpp20ReportRequest.DONETIME.getLength(), GlobalConstance.defaultTransportCharset)
					.toString().trim());
			requestMessage.getReportRequestMessage()
					.setDestterminalId(bodyBuffer.readCharSequence(Cmpp20ReportRequest.DESTTERMINALID.getLength(),
							GlobalConstance.defaultTransportCharset).toString().trim());
			requestMessage.getReportRequestMessage().setSmscSequence(bodyBuffer.readUnsignedInt());
		}

		// 剩下的字节全部读取
		requestMessage.setReserved(bodyBuffer
				.readCharSequence(Cmpp20DeliverRequest.RESERVED.getLength(), GlobalConstance.defaultTransportCharset)
				.toString().trim());
		return requestMessage;
	}

	private void backup() throws UnsupportedEncodingException {
		byte[] body = null;
		// String test = JSONArray.toJSONString(body);
		// Object o = ProtoBufUtil.deserializer(body, MqEntity.class);
		// if (o instanceof CmppSubmitRequestMessage) {
		// LOGGER.info("yes!");
		// } else {
		// LOGGER.info("no!");
		// }
		//
		String message = new String(body, "UTF-8");
		// LOGGER.info(queueName + " received: " + envelope.getRoutingKey() + "
		// message: " + message);
		String appId;
		String msgId;
		// Object obj;
		CmppSubmitRequestMessage obj = null;
		// String s = "\"{hello {hey";
		// System.out.println("json to string x, " + JSON.toJSONString(s));
		// System.out.println(
		// "json to string 0, " + JSON.toJSONString(s).replaceAll("\\\\",
		// "123").replaceAll("\"\\{", "{"));
		// System.out.println("json to string 1, " +
		// JSON.toJSONString(message));
		// System.out.println("json to string 2, "
		// + JSON.toJSONString(message).replaceAll("\\\\",
		// "").replaceAll("\"\\{", "{").replaceAll("\\}\"", "}"));
		// MqEntity mqEntity = JSON.parseObject(
		// JSON.toJSONString(message).replaceAll("\\\\", "").replaceAll("\"\\{",
		// "{").replaceAll("\\}\"", "}"),
		// new TypeReference<MqEntity>() {
		// });
		MqEntity mqEntity = JSON.parseObject(message.replaceAll("\\\\", ""), MqEntity.class);
		LOGGER.info("mqEntity.getAppId() = " + mqEntity.getAppId());
		// if (mqEntity.getcObj() instanceof CmppSubmitRequestMessage) {
		// LOGGER.info("yes!");
		// } else {
		// LOGGER.info("no!");
		// }
		// try {
		// JSONObject jsonObject = JSONObject.parseObject(message);
		// appId = jsonObject.getString("appId");
		// msgId = jsonObject.getString("msgId");
		// obj = (CmppSubmitRequestMessage) jsonObject.get("obj");
		// JSON.parseObject(jsonResult, new
		// TypeReference<ResultBean<ModelPOBean>>() {
		// });
		// LOGGER.info("App用户appId={}的consumer接收到的消息体为{}", appId,
		// jsonObject.toJSONString());
		// } catch (Exception e) {
		// LOGGER.error("json格式转换失败，请检查参数......", e);
		// return;
		// }

		// if (obj instanceof CmppSubmitRequestMessage) {
		EndpointConnector<?> connector = EndpointManager.INS.getEndpointConnector("109002");
		while (true) {
			LOGGER.info("ready publish from consumer");
			ChannelFuture write = connector.asynwrite((CmppSubmitRequestMessage) obj);
			if (write != null) {
				break;
			} else {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		;
		// }

		// String sql = "from App where AppId = ?";
		// App app = ypDao.findSingle(sql, appId);
		// if (deleteOrderService.enableCancelOrder(app, language)
		// && deleteOrderService.cancelOrderSuccess(audioId, appId)) {
		// LOGGER.info("audioId: " + audioId + " cancel success, appId: " +
		// appId);
		// this.getChannel().basicAck(envelope.getDeliveryTag(), false);
		// return;
		// }
		// LOGGER.info("APP用户appId={}的音频audioId={}正在被消费", appId, audioId);
		// // 当没有空闲的转写路数时等待
		// while (!redisService.appIdCanConvert(appId, language)) {
		// LOGGER.info("当前APP用户appId={}没有多余的转写路数", appId);
		// try {
		// Thread.sleep(waitTime);
		// } catch (InterruptedException e) {
		// LOGGER.error(e.getMessage());
		// }
		// }
		// this.getChannel().basicAck(envelope.getDeliveryTag(), false);
		// AudioInfo audioInfo = ypDao.findSingle("from AudioInfo where Id = ?",
		// new Object[] {audioId});
		// audioInfo.setTranscriptStatus(AudioConsts.TRANSCRIPT_PROCCESSING);
		// audioInfo.setTransStartTime(new Date());
		// ypDao.save(audioInfo);
		// // 调用转写引擎
		// apiService.callEngineV2(appId, audioInfo, originalFile + ".wav");
		// LOGGER.info("app用户appId={}的音频audioId={}调用引擎转写成功", appId, audioId);
	}

	public static Object toObject(byte[] bytes) {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(bais);
			Object object = ois.readObject();
			return object;
		} catch (IOException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		} finally {
			try {
				ois.close();
			} catch (Exception e) {
			}
		}
	}

	@Override
	public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			LOGGER.error(e1.getMessage(), e1);
		}
		LOGGER.error("v2 ShutdownSignalException by queueName = " + queueName + ", sig: " + sig);
		// Channel channel = null;
		// try {
		// channel = rabbitmqService.getChannel();
		// channel.confirmSelect();
		// channel.basicQos(1);
		// Consumer consumer = new AppConvertConsumer(rabbitmqService,
		// queueName, channel, redisService, apiService,
		// ypDao, waitTime, deleteOrderService);
		// channel.basicConsume(queueName, false, consumer);
		// } catch (Exception e) {
		// LOGGER.info(e.getMessage(), e);
		// }
		LOGGER.info("v2 new channel created by queueName = " + queueName);
	}

	@Override
	public void handleConsumeOk(String consumerTag) {
		LOGGER.info("transcodeConsumer handleConsumeOk: " + consumerTag);
	}

	@Override
	public void handleCancelOk(String consumerTag) {
		LOGGER.info("transcodeConsumer handleCancelOk: " + consumerTag);
	}

	@Override
	public void handleCancel(String consumerTag) throws IOException {
		LOGGER.info("transcodeConsumer handleCancel: " + consumerTag);
	}

	@Override
	public void handleRecoverOk(String consumerTag) {
		LOGGER.info("transcodeConsumer handleRecoverOk: " + consumerTag);
	}

}
